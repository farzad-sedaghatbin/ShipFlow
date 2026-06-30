package com.github.farzadsedaghatbin.shipflow.service;

import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Manages per-user SSE emitters for real-time notification push.
 *
 * <p>Each authenticated user may hold at most one active SSE stream. When a new
 * {@code GET /api/notifications/stream} request arrives for a user who already has an open stream,
 * the old emitter is completed and replaced.
 *
 * <p>Thread-safety is achieved via {@link ConcurrentHashMap}. Emitter lifecycle callbacks
 * (completion, timeout, error) always remove the entry from the map so the memory footprint stays
 * bounded.
 */
@Component
@Slf4j
public class NotificationSseManager {

  /** One active emitter per user (keyed by user ID). */
  private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

  /** SSE emitter timeout — 30 minutes. Clients must reconnect after this. */
  private static final long SSE_TIMEOUT_MS = 30 * 60 * 1_000L;

  /**
   * Subscribe a user to the notification SSE stream.
   *
   * <p>If the user already has an open emitter it is completed first (the browser will reconnect
   * via the standard EventSource reconnect mechanism).
   *
   * @param userId the authenticated user's ID
   * @return a configured {@link SseEmitter} ready to be returned from the controller
   */
  public SseEmitter subscribe(Long userId) {
    // Replace any existing emitter for this user
    SseEmitter existing = emitters.remove(userId);
    if (existing != null) {
      try {
        existing.complete();
      } catch (Exception ignored) {
        // Best-effort close
      }
    }

    SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
    emitters.put(userId, emitter);

    // Wire cleanup callbacks before sending the first event to avoid race conditions.
    // Use compare-and-remove so a stale callback never evicts a freshly registered emitter.
    emitter.onCompletion(() -> {
      emitters.remove(userId, emitter);
      log.debug("Notification SSE stream completed for user {}", userId);
    });
    emitter.onTimeout(() -> {
      emitters.remove(userId, emitter);
      log.debug("Notification SSE stream timed out for user {}", userId);
    });
    emitter.onError(ex -> {
      emitters.remove(userId, emitter);
      log.debug("Notification SSE stream error for user {}: {}", userId, ex.getMessage());
    });

    // Send an initial "connected" heartbeat so the browser knows the stream is live
    try {
      emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"connected\"}"));
      log.debug("Notification SSE stream opened for user {} (active streams: {})", userId,
          emitters.size());
    } catch (Exception e) {
      log.warn("Failed to send SSE connected event for user {}: {}", userId, e.getMessage());
      emitters.remove(userId, emitter);
    }

    return emitter;
  }

  /**
   * Push a notification payload to a specific user's SSE stream.
   *
   * <p>If the user has no active stream (e.g. browser tab closed) this is a silent no-op.
   *
   * @param userId  the target user's ID
   * @param payload the notification DTO to serialise as JSON
   */
  public void sendToUser(Long userId, Object payload) {
    SseEmitter emitter = emitters.get(userId);
    if (emitter == null) {
      return;
    }
    try {
      emitter.send(
          SseEmitter.event().name("notification").data(payload, MediaType.APPLICATION_JSON));
      log.debug("Notification SSE event sent to user {}", userId);
    } catch (Exception e) {
      log.debug("SSE send failed for user {} — removing stale emitter: {}", userId, e.getMessage());
      emitters.remove(userId, emitter);
    }
  }

  /**
   * Broadcast an SSE event to every active subscriber. Used for app-wide events (e.g. Knowledge
   * Center updates) that are not targeted at a single user.
   *
   * <p>If sending to an individual emitter fails, that emitter is removed but the broadcast
   * continues for the remaining subscribers.
   *
   * @param eventName the SSE event name (used by the browser EventSource listener)
   * @param payload   the payload to serialise as JSON
   */
  public void broadcast(String eventName, Object payload) {
    emitters.forEach((userId, emitter) -> {
      try {
        emitter.send(SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
      } catch (Exception e) {
        log.debug("SSE broadcast failed for user {} on event {} — removing stale emitter: {}",
            userId, eventName, e.getMessage());
        emitters.remove(userId, emitter);
      }
    });
  }

  /**
   * Push an arbitrary named SSE event to a specific user's stream without persisting to the
   * notification table. Used for transient real-time signals (e.g. retro board updates).
   *
   * @param userId    the target user's ID
   * @param eventName the SSE event name (e.g. {@code "retro-updated"})
   * @param payload   the payload to serialise as JSON
   */
  public void sendEventToUser(Long userId, String eventName, Object payload) {
    SseEmitter emitter = emitters.get(userId);
    if (emitter == null) {
      return;
    }
    try {
      emitter.send(
          SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
    } catch (Exception e) {
      log.debug("SSE send '{}' failed for user {} — removing stale emitter", eventName, userId);
      emitters.remove(userId, emitter);
    }
  }

  /**
   * Number of currently active SSE streams. Useful for monitoring/metrics.
   *
   * @return count of open emitters
   */
  public int getActiveCount() {
    return emitters.size();
  }
}
