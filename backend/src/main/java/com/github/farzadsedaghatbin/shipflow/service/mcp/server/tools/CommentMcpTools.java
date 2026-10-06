package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.comment.CommentDTO;
import com.github.farzadsedaghatbin.shipflow.dto.comment.CreateCommentRequest;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentEntityType;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.BugReportService;
import com.github.farzadsedaghatbin.shipflow.service.CommentService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** MCP tool implementations for comment operations (read: {@code get_comments}; write: {@code add_comment}). */
@Component
@RequiredArgsConstructor
public class CommentMcpTools {

  private final CommentService commentService;
  private final UserRepository userRepository;
  private final BugReportService bugReportService;

  // ── Tool definitions ──────────────────────────────────────────────────────

  public static final String TOOL_ADD_COMMENT = "add_comment";
  public static final String TOOL_GET_COMMENTS = "get_comments";

  public static Map<String, Object> getCommentsDefinition() {
    return Map.of(
        "name",
        TOOL_GET_COMMENTS,
        "description",
            "Read the comment thread on a task or bug report, oldest first. Returns each comment's "
                + "id, content, author (name + username), createdAt/updatedAt and whether it was "
                + "edited. entityType must be TASK or BUG_REPORT. Identify the entity by its numeric "
                + "entityId; for a bug you may pass its bugKey (e.g. \"BUG-125\") instead.",
        "inputSchema",
            Map.of(
                "type",
                "object",
                "properties",
                    Map.of(
                        "entityType",
                        Map.of(
                            "type",
                            "string",
                            "description",
                            "Entity type: TASK or BUG_REPORT",
                            "enum",
                            List.of("TASK", "BUG_REPORT")),
                        "entityId",
                        Map.of("type", "integer", "description", "The numeric task or bug report ID"),
                        "bugKey",
                        Map.of(
                            "type",
                            "string",
                            "description",
                            "BUG_REPORT only: the bug's key (e.g. BUG-125), as an alternative to entityId")),
                "required",
                List.of("entityType")));
  }

  public static Map<String, Object> addCommentDefinition() {
    return Map.of(
        "name",
        TOOL_ADD_COMMENT,
        "description",
            "Add a comment to a task, bug report, or wiki page. "
                + "Requires WRITE API key scope. "
                + "entityType must be TASK, BUG_REPORT, or WIKI_PAGE.",
        "inputSchema",
            Map.of(
                "type",
                "object",
                "properties",
                    Map.of(
                        "entityType",
                        Map.of(
                            "type",
                            "string",
                            "description",
                            "Entity type: TASK, BUG_REPORT, or WIKI_PAGE",
                            "enum",
                            List.of("TASK", "BUG_REPORT", "WIKI_PAGE")),
                        "entityId",
                        Map.of("type", "integer", "description", "The numeric entity ID"),
                        "content",
                        Map.of(
                            "type",
                            "string",
                            "description",
                            "Comment text (supports @mentions)")),
                "required",
                List.of("entityType", "entityId", "content")));
  }

  // ── Implementations ───────────────────────────────────────────────────────

  /**
   * Add a comment to a task or bug report as the authenticated MCP user.
   *
   * <p>The {@code auth} argument is passed explicitly from the dispatcher so that this method does
   * not rely on {@link org.springframework.security.core.context.SecurityContextHolder}, which is
   * unreliable when dispatch runs on an executor thread without security-context propagation.
   */
  public CommentDTO addComment(Map<String, Object> args, Authentication auth) {
    Object entityTypeValue = args.get("entityType");
    if (entityTypeValue == null) {
      throw new IllegalArgumentException("Missing required argument: entityType");
    }
    String entityTypeStr = entityTypeValue.toString().trim();
    if (entityTypeStr.isBlank()) {
      throw new IllegalArgumentException("Missing required argument: entityType");
    }
    CommentEntityType entityType;
    try {
      entityType = CommentEntityType.valueOf(entityTypeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid entityType '" + entityTypeStr + "'. Must be TASK, BUG_REPORT, or WIKI_PAGE");
    }

    long entityId = toLong(args.get("entityId"), "entityId");
    Object contentValue = args.get("content");
    if (contentValue == null) {
      throw new IllegalArgumentException("Missing required argument: content");
    }
    String content = contentValue.toString().trim();
    if (content.isBlank()) {
      throw new IllegalArgumentException("Missing required argument: content");
    }

    User currentUser = resolveUser(auth);
    CreateCommentRequest request = CreateCommentRequest.builder()
        .content(content)
        .entityType(entityType)
        .entityId(entityId)
        .build();
    return commentService.createComment(request, currentUser.getId());
  }

  /**
   * Read the comments on a task or bug report. Wiki pages are deliberately excluded: wiki spaces
   * have their own access rules that the comment layer doesn't enforce.
   */
  public Map<String, Object> getComments(Map<String, Object> args, Authentication auth) {
    Object entityTypeValue = args.get("entityType");
    String entityTypeStr = entityTypeValue == null ? "" : entityTypeValue.toString().trim().toUpperCase();
    if (entityTypeStr.isBlank()) {
      throw new IllegalArgumentException("Missing required argument: entityType");
    }
    CommentEntityType entityType = switch (entityTypeStr) {
      case "TASK" -> CommentEntityType.TASK;
      case "BUG_REPORT", "BUG" -> CommentEntityType.BUG_REPORT;
      default -> throw new IllegalArgumentException(
          "Invalid entityType '" + entityTypeStr + "'. Must be TASK or BUG_REPORT");
    };

    long entityId;
    Object bugKey = args.get("bugKey");
    if (args.get("entityId") == null && entityType == CommentEntityType.BUG_REPORT && bugKey != null
        && !bugKey.toString().isBlank()) {
      entityId = bugReportService.getBugReportByKey(bugKey.toString().trim()).getId();
    } else {
      entityId = toLong(args.get("entityId"), "entityId");
    }

    User currentUser = resolveUser(auth);
    List<Map<String, Object>> comments = commentService
        .getCommentsForExistingEntity(entityType, entityId, currentUser.getId()).stream()
        .map(this::toSummary)
        .toList();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("entityType", entityType.name());
    result.put("entityId", entityId);
    result.put("count", comments.size());
    result.put("comments", comments);
    return result;
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /** Agent-facing shape: drops UI-only fields (canEdit/canDelete, reaction summaries). */
  private Map<String, Object> toSummary(CommentDTO c) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", c.getId());
    m.put("content", c.getContent());
    m.put("authorName", c.getAuthorName());
    m.put("authorUsername", c.getAuthorUsername());
    m.put("createdAt", c.getCreatedAt());
    m.put("updatedAt", c.getUpdatedAt());
    m.put("edited", Boolean.TRUE.equals(c.getIsEdited()));
    return m;
  }

  private User resolveUser(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new SecurityException("No authenticated user in MCP session");
    }
    return userRepository.findByUsername(auth.getName())
        .orElseThrow(
            () -> new SecurityException("MCP user not found: " + auth.getName()));
  }

  private long toLong(Object val, String argName) {
    if (val == null) {
      throw new IllegalArgumentException("Missing required argument: " + argName);
    }
    if (val instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(val.toString());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Argument '" + argName + "' must be a number, got: " + val);
    }
  }
}
