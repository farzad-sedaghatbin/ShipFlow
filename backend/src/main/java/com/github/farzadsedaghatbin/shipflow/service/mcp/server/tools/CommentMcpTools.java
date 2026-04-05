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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** MCP tool implementations for comment operations (write). */
@Component
@ConditionalOnProperty(name = "mcp.server.enabled", havingValue = "true")
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
            "Add a comment to a task or bug report. "
                + "Requires WRITE API key scope. "
                + "entityType must be TASK or BUG_REPORT.",
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

  public CommentDTO addComment(Map<String, Object> args) {
    String entityTypeStr = (String) args.get("entityType");
    if (entityTypeStr == null || entityTypeStr.isBlank()) {
      throw new IllegalArgumentException("Missing required argument: entityType");
    }
    CommentEntityType entityType;
    try {
      entityType = CommentEntityType.valueOf(entityTypeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid entityType '" + entityTypeStr + "'. Must be TASK or BUG_REPORT");
    }

    long entityId = toLong(args.get("entityId"));
    String content = (String) args.get("content");
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("Missing required argument: content");
    }

    User currentUser = resolveCurrentUser();
    CreateCommentRequest request = CreateCommentRequest.builder()
        .content(content)
        .entityType(entityType)
        .entityId(entityId)
        .build();
    return commentService.createComment(request, currentUser.getId());
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private User resolveCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) {
      throw new SecurityException("No authenticated user found in MCP context");
    }
    return userRepository.findByUsername(auth.getName())
        .orElseThrow(
            () -> new SecurityException("MCP user not found: " + auth.getName()));
  }

  private long toLong(Object val) {
    if (val == null) {
      throw new IllegalArgumentException("Missing required argument");
    }
    if (val instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(val.toString());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Argument must be a number, got: " + val);
    }
  }
}
