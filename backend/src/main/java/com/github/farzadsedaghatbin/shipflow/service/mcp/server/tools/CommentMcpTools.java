package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import com.github.farzadsedaghatbin.shipflow.dto.comment.CommentDTO;
import com.github.farzadsedaghatbin.shipflow.dto.comment.CreateCommentRequest;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentEntityType;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.CommentService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** MCP tool implementations for comment operations (write). */
@Component
@RequiredArgsConstructor
public class CommentMcpTools {

  private final CommentService commentService;
  private final UserRepository userRepository;

  // ── Tool definitions ──────────────────────────────────────────────────────

  public static final String TOOL_ADD_COMMENT = "add_comment";

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

  // ── Helpers ───────────────────────────────────────────────────────────────

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
