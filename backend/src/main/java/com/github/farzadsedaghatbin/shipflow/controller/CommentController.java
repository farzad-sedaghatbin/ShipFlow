package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.comment.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentEntityType;
import com.github.farzadsedaghatbin.shipflow.service.CommentService;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for comment management. Provides CRUD operations for comments
 * on tasks and bug reports.
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Comment management for tasks and bug reports")
public class CommentController {

  private final CommentService commentService;
  private final UserService userService;

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Create a comment", description = "Creates a new comment on a task or bug report")
  public ResponseEntity<CommentDTO> createComment(@Valid @RequestBody CreateCommentRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = getUserId(userDetails);
    return ResponseEntity.ok(commentService.createComment(request, userId));
  }

  @GetMapping("/{entityType}/{entityId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get comments for entity", description = "Returns all comments for a task or bug report")
  public ResponseEntity<List<CommentDTO>> getComments(@PathVariable String entityType, @PathVariable Long entityId,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = getUserId(userDetails);
    CommentEntityType type = CommentEntityType.valueOf(entityType.toUpperCase());
    return ResponseEntity.ok(commentService.getComments(type, entityId, userId));
  }

  @GetMapping("/{entityType}/{entityId}/paginated")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get comments for entity (paginated)", description = "Returns paginated comments for a task or bug report")
  public ResponseEntity<Page<CommentDTO>> getCommentsPaginated(@PathVariable String entityType,
      @PathVariable Long entityId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = getUserId(userDetails);
    CommentEntityType type = CommentEntityType.valueOf(entityType.toUpperCase());
    return ResponseEntity.ok(commentService.getCommentsPaginated(type, entityId, userId, pageable));
  }

  @GetMapping("/single/{commentId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get single comment", description = "Returns a single comment by ID")
  public ResponseEntity<CommentDTO> getComment(@PathVariable Long commentId,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = getUserId(userDetails);
    return ResponseEntity.ok(commentService.getComment(commentId, userId));
  }

  @PutMapping("/{commentId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Update a comment", description = "Updates an existing comment")
  public ResponseEntity<CommentDTO> updateComment(@PathVariable Long commentId,
      @Valid @RequestBody UpdateCommentRequest request, @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = getUserId(userDetails);
    return ResponseEntity.ok(commentService.updateComment(commentId, request, userId));
  }

  @DeleteMapping("/{commentId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Delete a comment", description = "Soft deletes a comment")
  public ResponseEntity<Map<String, String>> deleteComment(@PathVariable Long commentId,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = getUserId(userDetails);
    commentService.deleteComment(commentId, userId);
    return ResponseEntity.ok(Map.of("message", "Comment deleted successfully"));
  }

  @PostMapping("/{commentId}/reactions")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Toggle reaction", description = "Adds or removes a reaction on a comment (toggle behavior)")
  public ResponseEntity<CommentDTO> toggleReaction(@PathVariable Long commentId,
      @Valid @RequestBody ReactionRequest request, @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = getUserId(userDetails);
    return ResponseEntity.ok(commentService.toggleReaction(commentId, request, userId));
  }

  @GetMapping("/{entityType}/{entityId}/count")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get comment count", description = "Returns the number of comments for an entity")
  public ResponseEntity<Map<String, Long>> getCommentCount(@PathVariable String entityType,
      @PathVariable Long entityId) {
    CommentEntityType type = CommentEntityType.valueOf(entityType.toUpperCase());
    long count = commentService.getCommentCount(type, entityId);
    return ResponseEntity.ok(Map.of("count", count));
  }

  @GetMapping("/reactions/available")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get available reactions", description = "Returns list of available reaction types with emojis")
  public ResponseEntity<List<Map<String, String>>> getAvailableReactions() {
    return ResponseEntity.ok(commentService.getAvailableReactions());
  }

  @GetMapping("/users/search")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Search users for @mention", description = "Returns users matching the search query for @mention autocomplete")
  public ResponseEntity<List<MentionUserDTO>> searchUsersForMention(@RequestParam(defaultValue = "") String query) {
    return ResponseEntity.ok(commentService.searchUsersForMention(query));
  }

  private Long getUserId(UserDetails userDetails) {
    return userService.findByUsername(userDetails.getUsername()).getId();
  }
}
