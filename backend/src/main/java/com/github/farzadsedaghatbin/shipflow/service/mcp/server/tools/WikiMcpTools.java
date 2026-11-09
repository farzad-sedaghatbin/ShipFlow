package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.CreateWikiPageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.CreateWikiSpaceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.GrantPermissionRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.MovePageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.UpdateWikiPageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.UpdateWikiSpaceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiPageDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiSpaceDTO;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.WikiGranteeType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.WikiPermissionLevel;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.WikiService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * MCP tools giving full read and write control of the wiki: spaces, pages (create, edit, move,
 * delete, restore an old revision) and space permissions.
 *
 * <p>Every call runs as the authenticated MCP user through {@link WikiService}, so the wiki's own
 * per-space ACL ({@code WikiPermissionService}) still decides what that user may read or change —
 * the tools add no authority of their own. Deletes are soft deletes, as everywhere else.
 *
 * <p>Page bodies are stored as BlockNote JSON. For convenience a write tool accepts either {@code
 * content} (BlockNote JSON, as returned by {@code get_wiki_page}) or {@code markdown}, a light
 * Markdown subset (headings, bullet/numbered lists, paragraphs) converted to BlockNote blocks.
 */
@Component
@RequiredArgsConstructor
public class WikiMcpTools {

  private final WikiService wikiService;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  // ── Tool names ────────────────────────────────────────────────────────────

  public static final String TOOL_LIST_SPACES = "list_wiki_spaces";
  public static final String TOOL_GET_TREE = "get_wiki_tree";
  public static final String TOOL_GET_PAGE = "get_wiki_page";
  public static final String TOOL_SEARCH = "search_wiki_pages";
  public static final String TOOL_GET_HISTORY = "get_wiki_page_history";
  public static final String TOOL_GET_REVISION = "get_wiki_revision";
  public static final String TOOL_LIST_PERMISSIONS = "list_wiki_permissions";

  public static final String TOOL_CREATE_SPACE = "create_wiki_space";
  public static final String TOOL_UPDATE_SPACE = "update_wiki_space";
  public static final String TOOL_DELETE_SPACE = "delete_wiki_space";
  public static final String TOOL_CREATE_PAGE = "create_wiki_page";
  public static final String TOOL_UPDATE_PAGE = "update_wiki_page";
  public static final String TOOL_MOVE_PAGE = "move_wiki_page";
  public static final String TOOL_DELETE_PAGE = "delete_wiki_page";
  public static final String TOOL_RESTORE_REVISION = "restore_wiki_revision";
  public static final String TOOL_GRANT_PERMISSION = "grant_wiki_permission";
  public static final String TOOL_REVOKE_PERMISSION = "revoke_wiki_permission";

  // ── Definitions ───────────────────────────────────────────────────────────

  private static Map<String, Object> def(
      String name, String description, Map<String, Object> properties, List<String> required) {
    return Map.of(
        "name", name,
        "description", description,
        "inputSchema", Map.of("type", "object", "properties", properties, "required", required));
  }

  private static Map<String, Object> intProp(String description) {
    return Map.of("type", "integer", "description", description);
  }

  private static Map<String, Object> strProp(String description) {
    return Map.of("type", "string", "description", description);
  }

  private static final String BODY_NOTE =
      "Provide the body as `markdown` (headings #, bullets -, numbered 1., paragraphs) or as "
          + "`content` (BlockNote JSON array, the format get_wiki_page returns). `content` wins "
          + "if both are given.";

  public static Map<String, Object> listSpacesDefinition() {
    return def(
        TOOL_LIST_SPACES,
        "List the wiki spaces the authenticated user can read (id, name, spaceKey, description, "
            + "projectId).",
        Map.of(),
        List.of());
  }

  public static Map<String, Object> getTreeDefinition() {
    return def(
        TOOL_GET_TREE,
        "Get the page tree of a wiki space (nested titles, slugs and ids, in display order).",
        Map.of("spaceId", intProp("Wiki space ID")),
        List.of("spaceId"));
  }

  public static Map<String, Object> getPageDefinition() {
    return def(
        TOOL_GET_PAGE,
        "Read a wiki page: title, plain text (contentText), the BlockNote JSON body (content), "
            + "resolved [[page]] links, and `version` — pass it back as expectedVersion on "
            + "update_wiki_page to avoid overwriting someone else's edit.",
        Map.of("pageId", intProp("Wiki page ID")),
        List.of("pageId"));
  }

  public static Map<String, Object> searchDefinition() {
    return def(
        TOOL_SEARCH,
        "Search wiki page titles (case-insensitive substring) across the spaces the user can "
            + "read, optionally within one space.",
        Map.of("query", strProp("Title text to look for"), "spaceId", intProp("Limit to this space")),
        List.of("query"));
  }

  public static Map<String, Object> getHistoryDefinition() {
    return def(
        TOOL_GET_HISTORY,
        "List a page's revisions (revision number, timestamp, editorId, title), newest first.",
        Map.of("pageId", intProp("Wiki page ID")),
        List.of("pageId"));
  }

  public static Map<String, Object> getRevisionDefinition() {
    return def(
        TOOL_GET_REVISION,
        "Read the full content of one historical revision of a page.",
        Map.of("pageId", intProp("Wiki page ID"), "revision", intProp("Revision number")),
        List.of("pageId", "revision"));
  }

  public static Map<String, Object> listPermissionsDefinition() {
    return def(
        TOOL_LIST_PERMISSIONS,
        "List the access rules (grantee type/ref and level) on a wiki space.",
        Map.of("spaceId", intProp("Wiki space ID")),
        List.of("spaceId"));
  }

  public static Map<String, Object> createSpaceDefinition() {
    return def(
        TOOL_CREATE_SPACE,
        "Create a wiki space. Requires WRITE API key scope. spaceKey defaults to a slug of the "
            + "name; projectId optionally ties the space to a project.",
        Map.of(
            "name", strProp("Space name"),
            "spaceKey", strProp("Short unique key (optional)"),
            "description", strProp("Description (optional)"),
            "projectId", intProp("Project to associate the space with (optional)")),
        List.of("name"));
  }

  public static Map<String, Object> updateSpaceDefinition() {
    return def(
        TOOL_UPDATE_SPACE,
        "Rename or re-describe a wiki space. Only the fields you pass change. Requires WRITE "
            + "API key scope.",
        Map.of(
            "spaceId", intProp("Wiki space ID"),
            "name", strProp("New name"),
            "description", strProp("New description"),
            "projectId", intProp("Project to associate the space with")),
        List.of("spaceId"));
  }

  public static Map<String, Object> deleteSpaceDefinition() {
    return def(
        TOOL_DELETE_SPACE,
        "Soft-delete a wiki space and every page in it. Requires WRITE API key scope.",
        Map.of("spaceId", intProp("Wiki space ID")),
        List.of("spaceId"));
  }

  public static Map<String, Object> createPageDefinition() {
    return def(
        TOOL_CREATE_PAGE,
        "Create a wiki page at the end of a space (or under a parent page). "
            + BODY_NOTE
            + " Requires WRITE API key scope.",
        Map.of(
            "spaceId", intProp("Wiki space ID"),
            "parentId", intProp("Parent page ID (omit for a top-level page)"),
            "title", strProp("Page title"),
            "markdown", strProp("Body as light Markdown"),
            "content", strProp("Body as BlockNote JSON")),
        List.of("spaceId", "title"));
  }

  public static Map<String, Object> updatePageDefinition() {
    return def(
        TOOL_UPDATE_PAGE,
        "Edit a wiki page's title and/or body; omitted fields are unchanged. "
            + BODY_NOTE
            + " Pass expectedVersion (from get_wiki_page) so a concurrent edit is reported as a "
            + "conflict instead of being overwritten. Requires WRITE API key scope.",
        Map.of(
            "pageId", intProp("Wiki page ID"),
            "title", strProp("New title"),
            "markdown", strProp("New body as light Markdown"),
            "content", strProp("New body as BlockNote JSON"),
            "expectedVersion", intProp("The page version you last read")),
        List.of("pageId"));
  }

  public static Map<String, Object> movePageDefinition() {
    return def(
        TOOL_MOVE_PAGE,
        "Move a page under another parent (or to the top level) at a given position. Requires "
            + "WRITE API key scope.",
        Map.of(
            "pageId", intProp("Wiki page ID"),
            "newParentId", intProp("New parent page ID (omit for top level)"),
            "newIndex", intProp("0-based position among the new siblings (default 0)")),
        List.of("pageId"));
  }

  public static Map<String, Object> deletePageDefinition() {
    return def(
        TOOL_DELETE_PAGE,
        "Soft-delete a wiki page. Requires WRITE API key scope.",
        Map.of("pageId", intProp("Wiki page ID")),
        List.of("pageId"));
  }

  public static Map<String, Object> restoreRevisionDefinition() {
    return def(
        TOOL_RESTORE_REVISION,
        "Restore a page's title and body from an earlier revision (recorded as a new edit). "
            + "Requires WRITE API key scope.",
        Map.of("pageId", intProp("Wiki page ID"), "revision", intProp("Revision number")),
        List.of("pageId", "revision"));
  }

  public static Map<String, Object> grantPermissionDefinition() {
    return def(
        TOOL_GRANT_PERMISSION,
        "Grant access on a wiki space. granteeType: USER (granteeRef = username) or ROLE "
            + "(granteeRef = role name). level: READ or WRITE. Requires WRITE API key scope.",
        Map.of(
            "spaceId", intProp("Wiki space ID"),
            "granteeType", strProp("USER or ROLE"),
            "granteeRef", strProp("Username or role name"),
            "level", strProp("Permission level, e.g. READ or WRITE")),
        List.of("spaceId", "granteeType", "granteeRef", "level"));
  }

  public static Map<String, Object> revokePermissionDefinition() {
    return def(
        TOOL_REVOKE_PERMISSION,
        "Remove an access rule from a wiki space (ids come from list_wiki_permissions). "
            + "Requires WRITE API key scope.",
        Map.of("permissionId", intProp("Permission rule ID")),
        List.of("permissionId"));
  }

  // ── Read implementations ──────────────────────────────────────────────────

  public List<WikiSpaceDTO> listSpaces(Authentication auth) {
    return wikiService.listSpaces(userId(auth));
  }

  public Object getTree(Map<String, Object> args, Authentication auth) {
    return wikiService.getTree(toLong(args.get("spaceId"), "spaceId"), userId(auth));
  }

  public Map<String, Object> getPage(Map<String, Object> args, Authentication auth) {
    return pageView(wikiService.getPage(toLong(args.get("pageId"), "pageId"), userId(auth)));
  }

  public Object search(Map<String, Object> args, Authentication auth) {
    Object q = args.get("query");
    if (q == null || q.toString().isBlank()) {
      throw new IllegalArgumentException("Missing required argument: query");
    }
    Long spaceId = args.get("spaceId") == null ? null : toLong(args.get("spaceId"), "spaceId");
    return wikiService.searchPages(q.toString(), spaceId, userId(auth));
  }

  public Object getHistory(Map<String, Object> args, Authentication auth) {
    return wikiService.getHistory(toLong(args.get("pageId"), "pageId"), userId(auth));
  }

  public Map<String, Object> getRevision(Map<String, Object> args, Authentication auth) {
    return pageView(
        wikiService.getRevision(
            toLong(args.get("pageId"), "pageId"),
            (int) toLong(args.get("revision"), "revision"),
            userId(auth)));
  }

  public Object listPermissions(Map<String, Object> args, Authentication auth) {
    return wikiService.listPermissions(toLong(args.get("spaceId"), "spaceId"), userId(auth));
  }

  // ── Write implementations ─────────────────────────────────────────────────

  public WikiSpaceDTO createSpace(Map<String, Object> args, Authentication auth) {
    String name = requireText(args, "name");
    return wikiService.createSpace(
        new CreateWikiSpaceRequest(
            name,
            optText(args, "spaceKey"),
            optText(args, "description"),
            optLong(args, "projectId")),
        userId(auth));
  }

  public WikiSpaceDTO updateSpace(Map<String, Object> args, Authentication auth) {
    return wikiService.updateSpace(
        toLong(args.get("spaceId"), "spaceId"),
        new UpdateWikiSpaceRequest(
            optText(args, "name"), optText(args, "description"), optLong(args, "projectId")),
        userId(auth));
  }

  public Map<String, Object> deleteSpace(Map<String, Object> args, Authentication auth) {
    long spaceId = toLong(args.get("spaceId"), "spaceId");
    wikiService.deleteSpace(spaceId, userId(auth));
    return Map.of("deleted", true, "spaceId", spaceId);
  }

  public Map<String, Object> createPage(Map<String, Object> args, Authentication auth) {
    WikiPageDTO page =
        wikiService.createPage(
            new CreateWikiPageRequest(
                toLong(args.get("spaceId"), "spaceId"),
                optLong(args, "parentId"),
                requireText(args, "title"),
                body(args)),
            userId(auth));
    return pageView(page);
  }

  public Map<String, Object> updatePage(Map<String, Object> args, Authentication auth) {
    String title = optText(args, "title");
    String body = body(args);
    if (title == null && body == null) {
      throw new IllegalArgumentException("Nothing to update: pass title, markdown or content");
    }
    return pageView(
        wikiService.updatePage(
            toLong(args.get("pageId"), "pageId"),
            new UpdateWikiPageRequest(title, body, optLong(args, "expectedVersion")),
            userId(auth)));
  }

  public Map<String, Object> movePage(Map<String, Object> args, Authentication auth) {
    long pageId = toLong(args.get("pageId"), "pageId");
    Long newParentId = optLong(args, "newParentId");
    Long index = optLong(args, "newIndex");
    wikiService.movePage(
        pageId, new MovePageRequest(newParentId, index == null ? 0 : index.intValue()), userId(auth));
    return Map.of("moved", true, "pageId", pageId);
  }

  public Map<String, Object> deletePage(Map<String, Object> args, Authentication auth) {
    long pageId = toLong(args.get("pageId"), "pageId");
    wikiService.deletePage(pageId, userId(auth));
    return Map.of("deleted", true, "pageId", pageId);
  }

  public Map<String, Object> restoreRevision(Map<String, Object> args, Authentication auth) {
    return pageView(
        wikiService.restoreRevision(
            toLong(args.get("pageId"), "pageId"),
            (int) toLong(args.get("revision"), "revision"),
            userId(auth)));
  }

  public Object grantPermission(Map<String, Object> args, Authentication auth) {
    return wikiService.grantPermission(
        toLong(args.get("spaceId"), "spaceId"),
        new GrantPermissionRequest(
            enumArg(WikiGranteeType.class, requireText(args, "granteeType"), "granteeType"),
            requireText(args, "granteeRef"),
            enumArg(WikiPermissionLevel.class, requireText(args, "level"), "level")),
        userId(auth));
  }

  public Map<String, Object> revokePermission(Map<String, Object> args, Authentication auth) {
    long permissionId = toLong(args.get("permissionId"), "permissionId");
    wikiService.revokePermission(permissionId, userId(auth));
    return Map.of("revoked", true, "permissionId", permissionId);
  }

  // ── Markdown → BlockNote ──────────────────────────────────────────────────

  private static final Pattern HEADING = Pattern.compile("^(#{1,3})\\s+(.*)$");
  private static final Pattern BULLET = Pattern.compile("^\\s*[-*]\\s+(.*)$");
  private static final Pattern NUMBERED = Pattern.compile("^\\s*\\d+[.)]\\s+(.*)$");

  /** Resolves the body arguments to a BlockNote JSON string, or {@code null} when none given. */
  String body(Map<String, Object> args) {
    String content = optText(args, "content");
    if (content != null) {
      try {
        if (!objectMapper.readTree(content).isArray()) {
          throw new IllegalArgumentException("content must be a BlockNote JSON array");
        }
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException("content is not valid JSON: " + e.getOriginalMessage());
      }
      return content;
    }
    Object markdown = args.get("markdown");
    return markdown == null ? null : markdownToBlocks(markdown.toString());
  }

  String markdownToBlocks(String markdown) {
    ArrayNode blocks = objectMapper.createArrayNode();
    for (String line : markdown.split("\\R")) {
      if (line.isBlank()) {
        continue;
      }
      String type = "paragraph";
      String text = line.strip();
      ObjectNode props = objectMapper.createObjectNode();
      Matcher m;
      if ((m = HEADING.matcher(text)).matches()) {
        type = "heading";
        props.put("level", m.group(1).length());
        text = m.group(2);
      } else if ((m = BULLET.matcher(line)).matches()) {
        type = "bulletListItem";
        text = m.group(1);
      } else if ((m = NUMBERED.matcher(line)).matches()) {
        type = "numberedListItem";
        text = m.group(1);
      }
      ObjectNode block = blocks.addObject();
      block.put("id", UUID.randomUUID().toString());
      block.put("type", type);
      block.set("props", props);
      ObjectNode run = block.putArray("content").addObject();
      run.put("type", "text");
      run.put("text", text);
      run.putObject("styles");
      block.putArray("children");
    }
    return blocks.toString();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private Map<String, Object> pageView(WikiPageDTO p) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", p.id());
    m.put("spaceId", p.spaceId());
    m.put("parentId", p.parentId());
    m.put("title", p.title());
    m.put("slug", p.slug());
    m.put("contentText", p.contentText());
    m.put("content", p.content());
    m.put("pageLinks", p.pageLinks());
    m.put("version", p.version());
    m.put("updatedAt", p.updatedAt());
    return m;
  }

  private Long userId(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new SecurityException("No authenticated user in MCP session");
    }
    User user =
        userRepository
            .findByUsername(auth.getName())
            .orElseThrow(() -> new SecurityException("MCP user not found: " + auth.getName()));
    return user.getId();
  }

  private static String optText(Map<String, Object> args, String key) {
    Object v = args.get(key);
    return v == null || v.toString().isBlank() ? null : v.toString();
  }

  private static String requireText(Map<String, Object> args, String key) {
    String v = optText(args, key);
    if (v == null) {
      throw new IllegalArgumentException("Missing required argument: " + key);
    }
    return v.trim();
  }

  private static Long optLong(Map<String, Object> args, String key) {
    return args.get(key) == null ? null : toLong(args.get(key), key);
  }

  private static <E extends Enum<E>> E enumArg(Class<E> type, String value, String argName) {
    try {
      return Enum.valueOf(type, value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Argument '" + argName + "' must be one of " + List.of(type.getEnumConstants()));
    }
  }

  private static long toLong(Object val, String argName) {
    if (val == null) {
      throw new IllegalArgumentException("Missing required argument: " + argName);
    }
    if (val instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(val.toString().trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Argument '" + argName + "' must be a number, got: " + val);
    }
  }
}
