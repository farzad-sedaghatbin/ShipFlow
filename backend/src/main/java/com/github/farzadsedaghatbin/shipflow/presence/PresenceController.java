package com.github.farzadsedaghatbin.shipflow.presence;

import com.github.farzadsedaghatbin.shipflow.dto.UserDTO;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Presence heartbeat endpoints for "who's currently viewing this" indicators (v1.13.0 S64) on
 * pitches, retrospectives, and wiki pages. The actual live viewer list is pushed to clients via
 * the existing {@code presence-update} SSE event (see {@link PresenceService}), not returned from
 * these endpoints — both are fire-and-forget from the caller's perspective.
 */
@RestController
@RequestMapping("/api/presence")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Tag(name = "Presence", description = "Live viewer presence for pitches, retrospectives, and wiki pages")
public class PresenceController {

  private final PresenceService presenceService;
  private final UserService userService;

  @PostMapping("/{entityType}/{entityId}/heartbeat")
  @Operation(
      summary = "Record a presence heartbeat",
      description = "Marks the current user as actively viewing the given entity. Clients should "
          + "call this periodically (well under the 45s server-side staleness window) while the "
          + "entity is open.")
  public ResponseEntity<Void> heartbeat(
      @PathVariable PresenceEntityType entityType, @PathVariable Long entityId) {
    UserDTO user = currentUser();
    presenceService.heartbeat(entityType, entityId, user.getId(), displayName(user));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{entityType}/{entityId}")
  @Operation(
      summary = "Leave presence",
      description = "Marks the current user as no longer viewing the given entity (e.g. on "
          + "navigate-away or tab close).")
  public ResponseEntity<Void> leave(
      @PathVariable PresenceEntityType entityType, @PathVariable Long entityId) {
    UserDTO user = currentUser();
    presenceService.leave(entityType, entityId, user.getId());
    return ResponseEntity.noContent().build();
  }

  private UserDTO currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return userService.findByUsername(authentication.getName());
  }

  private String displayName(UserDTO user) {
    return user.getPersonName() != null ? user.getPersonName() : user.getUsername();
  }
}
