package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.service.NotificationSseManager;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE endpoint for real-time notification streaming.
 *
 * <p>Clients open a long-lived {@code GET /api/notifications/stream} connection. The server pushes
 * {@code notification} events whenever a new notification is created for the authenticated user.
 * The client should use this to trigger a refresh of its notification list rather than parsing the
 * payload directly (so the full list always comes from the regular REST endpoint).
 *
 * <p>Authentication: standard JWT {@code Authorization: Bearer <token>} header, same as all other
 * {@code /api/**} endpoints. CSRF is not needed — this is a GET-only endpoint with no state
 * mutation, and the application already disables CSRF globally for the stateless JWT API.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications SSE", description = "Real-time notification stream via Server-Sent Events")
public class NotificationSseController {

  private final NotificationSseManager sseManager;
  private final UserService userService;

  /**
   * Subscribe to the real-time notification stream.
   *
   * <p>The response is a {@code text/event-stream}. The server immediately sends a {@code connected}
   * event, then pushes {@code notification} events as they arrive. The stream times out after 30
   * minutes; clients should reconnect automatically (native {@code EventSource} does this by
   * default).
   *
   * @return a configured {@link SseEmitter} that the servlet container will hold open
   */
  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize("isAuthenticated()")
  @Operation(
      summary = "Subscribe to notification stream",
      description = "Opens a long-lived SSE stream. Emits a 'connected' event on open, then "
          + "'notification' events whenever a new notification is created for the current user.")
  public SseEmitter stream() {
    Long userId = getCurrentUserId();
    return sseManager.subscribe(userId);
  }

  private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    return userService.findByUsername(username).getId();
  }
}
