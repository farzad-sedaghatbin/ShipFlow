package com.github.farzadsedaghatbin.shipflow.service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Manages per-user SSE emitters for real-time notification push.
 *
 * <p>Each authenticated user may hold any number of simultaneous SSE streams — e.g. one opened by
 * the global notification bell ({@code NotificationCenter.tsx}) and another by the Knowledge
 * Center ({@code KnowledgeCenter.tsx}), both independently connecting to {@code GET
 * /api/notifications/stream} for the same user. Every emitter registered for a user receives every
 * event sent to that user; none are evicted or replaced on a new {@code subscribe} call.
 *
 * <p>Delivery is two-tiered: an event is always delivered to every emitter this JVM/pod holds
 * locally, and — when a {@link RedisTemplate} is configured — is also published on {@link
 * #FANOUT_CHANNEL} so every other backend pod's locally-connected emitters receive it too. This
 * makes delivery correct in a multi-replica deployment (the Helm chart defaults to {@code
 * replicaCount: 2}), where a notification created on one pod must still reach a user whose SSE
 * connection landed on a different pod. Without Redis configured (e.g. the {@code test} profile),
 * delivery falls back to local-only, matching the previous single-instance behavior.
 *
 * <p>Thread-safety is achieved via {@link ConcurrentHashMap} of {@link CopyOnWriteArraySet}s.
 * Emitter lifecycle callbacks (completion, timeout, error) always remove the entry from its user's
 * set — and prune the set from the map entirely once empty — so the memory footprint stays
 * bounded.
 */
@Component
@Slf4j
public class NotificationSseManager {

  /**
   * Redis pub/sub channel used to fan out SSE events across backend pods.
   *
   * <p>Public (not merely package-private) because {@code SseRedisConfig} — which wires the Redis
   * {@code MessageListener} — lives in the {@code config} package, not {@code service}, and needs
   * this constant to subscribe to the same channel without duplicating the literal.
   */
  public static final String FANOUT_CHANNEL = "shipflow:sse:notifications";

  /** All active emitters per user (keyed by user ID); a user may hold more than one. */
  private final ConcurrentHashMap<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

  /** SSE emitter timeout — 30 minutes. Clients must reconnect after this. */
  private static final long SSE_TIMEOUT_MS = 30 * 60 * 1_000L;

  @Nullable
  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * Unique per-JVM/pod identifier, used to recognize (and ignore) this instance's own published
   * messages when they echo back through its own Redis subscription.
   */
  final String instanceId = UUID.randomUUID().toString();

  public NotificationSseManager(@Nullable RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * Subscribe a user to the notification SSE stream.
   *
   * <p>The new emitter is added alongside any existing emitters already open for this user —
   * nothing is evicted or replaced. This lets a single user have multiple independent SSE
   * consumers open at once (e.g. the notification bell and the Knowledge Center) without one
   * silently killing the other's stream.
   *
   * @param userId the authenticated user's ID
   * @return a configured {@link SseEmitter} ready to be returned from the controller
   */
  public SseEmitter subscribe(Long userId) {
    SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
    emitters.computeIfAbsent(userId, id -> new CopyOnWriteArraySet<>()).add(emitter);

    // Wire cleanup callbacks before sending the first event to avoid race conditions.
    // removeEmitter only ever removes this exact emitter instance, so a stale/delayed callback
    // can never evict a sibling emitter registered for the same user.
    emitter.onCompletion(() -> {
      removeEmitter(userId, emitter);
      log.debug("Notification SSE stream completed for user {}", userId);
    });
    emitter.onTimeout(() -> {
      removeEmitter(userId, emitter);
      log.debug("Notification SSE stream timed out for user {}", userId);
    });
    emitter.onError(ex -> {
      removeEmitter(userId, emitter);
      log.debug("Notification SSE stream error for user {}: {}", userId, ex.getMessage());
    });

    // Send an initial "connected" heartbeat so the browser knows the stream is live
    try {
      emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"connected\"}"));
      log.debug("Notification SSE stream opened for user {} (active streams: {})", userId,
          getActiveCount());
    } catch (Exception e) {
      log.warn("Failed to send SSE connected event for user {}: {}", userId, e.getMessage());
      removeEmitter(userId, emitter);
    }

    return emitter;
  }

  /**
   * Removes exactly this emitter instance from the user's set, and prunes the user's entry from
   * the map entirely once its set of emitters is empty (no leaked empty-set map entries).
   */
  private void removeEmitter(Long userId, SseEmitter emitter) {
    emitters.computeIfPresent(userId, (id, set) -> {
      set.remove(emitter);
      return set.isEmpty() ? null : set;
    });
  }

  /**
   * Push a notification payload to every SSE stream a specific user has open.
   *
   * <p>If the user has no active stream (e.g. all browser tabs closed) this is a silent no-op.
   * Delivery happens locally first, then — if Redis is configured — is also published so other
   * backend pods deliver it to any emitters they hold locally for the same user.
   *
   * @param userId  the target user's ID
   * @param payload the notification DTO to serialise as JSON
   */
  public void sendToUser(Long userId, Object payload) {
    deliverLocal(userId, "notification", payload);
    publishRemote(userId, "notification", payload);
  }

  /**
   * Broadcast an SSE event to every active subscriber, across every emitter each user holds. Used
   * for app-wide events (e.g. Knowledge Center updates) that are not targeted at a single user.
   *
   * <p>If sending to an individual emitter fails, that emitter is removed but the broadcast
   * continues for the remaining subscribers. Delivery happens locally first, then — if Redis is
   * configured — is also published so other backend pods broadcast it to their own locally-held
   * emitters.
   *
   * @param eventName the SSE event name (used by the browser EventSource listener)
   * @param payload   the payload to serialise as JSON
   */
  public void broadcast(String eventName, Object payload) {
    deliverLocalBroadcast(eventName, payload);
    publishRemote(null, eventName, payload);
  }

  /**
   * Push an arbitrary named SSE event to every SSE stream a specific user has open, without
   * persisting to the notification table. Used for transient real-time signals (e.g. retro board
   * updates).
   *
   * @param userId    the target user's ID
   * @param eventName the SSE event name (e.g. {@code "retro-updated"})
   * @param payload   the payload to serialise as JSON
   */
  public void sendEventToUser(Long userId, String eventName, Object payload) {
    deliverLocal(userId, eventName, payload);
    publishRemote(userId, eventName, payload);
  }

  /**
   * Delivers an event to every emitter this pod holds locally for one user. Each emitter is sent
   * to independently — a failure on one never stops delivery to its siblings, and any emitter
   * that fails to send is removed.
   */
  private void deliverLocal(Long userId, String eventName, Object payload) {
    Set<SseEmitter> userEmitters = emitters.get(userId);
    if (userEmitters == null) {
      return;
    }
    for (SseEmitter emitter : userEmitters) {
      try {
        emitter.send(SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
        log.debug("Notification SSE event '{}' sent to user {}", eventName, userId);
      } catch (Exception e) {
        log.debug("SSE send '{}' failed for user {} — removing stale emitter: {}", eventName,
            userId, e.getMessage());
        removeEmitter(userId, emitter);
      }
    }
  }

  /**
   * Delivers an event to every emitter this pod holds locally, across every user. Each emitter is
   * sent to independently — a failure on one never stops delivery to the rest, and any emitter
   * that fails to send is removed.
   */
  private void deliverLocalBroadcast(String eventName, Object payload) {
    for (var entry : emitters.entrySet()) {
      Long userId = entry.getKey();
      for (SseEmitter emitter : entry.getValue()) {
        try {
          emitter.send(
              SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
          log.debug("SSE broadcast failed for user {} on event {} — removing stale emitter: {}",
              userId, eventName, e.getMessage());
          removeEmitter(userId, emitter);
        }
      }
    }
  }

  /**
   * Publishes an event to {@link #FANOUT_CHANNEL} so every other backend pod's locally-connected
   * emitters receive it too. A no-op when no {@link RedisTemplate} is configured (single-instance
   * / test profile). Never throws — a Redis outage must not break local delivery, which has
   * already happened by the time this is called.
   *
   * @param userId    the target user's ID, or {@code null} to mean "broadcast to everyone"
   * @param eventName the SSE event name
   * @param payload   the payload to serialise
   */
  private void publishRemote(Long userId, String eventName, Object payload) {
    if (redisTemplate == null) {
      return;
    }
    try {
      redisTemplate.convertAndSend(
          FANOUT_CHANNEL, new SseFanOutMessage(instanceId, userId, eventName, payload));
    } catch (Exception e) {
      log.warn("Failed to publish SSE fan-out message for event '{}': {}", eventName,
          e.getMessage());
    }
  }

  /**
   * Entry point for a Redis {@code MessageListener} (wired in {@code SseRedisConfig}) delivering
   * the raw pub/sub message body received on {@link #FANOUT_CHANNEL}. Deserializes it and, if it's
   * a recognized {@link SseFanOutMessage}, hands it to {@link #onRemoteFanOut(SseFanOutMessage)}.
   * Never throws — a malformed or foreign message must not crash the listener thread.
   *
   * @param rawBody the raw message body from the Redis pub/sub channel
   */
  public void onRemoteMessage(byte[] rawBody) {
    if (redisTemplate == null) {
      return;
    }
    try {
      Object deserialized = redisTemplate.getValueSerializer().deserialize(rawBody);
      if (deserialized instanceof SseFanOutMessage msg) {
        onRemoteFanOut(msg);
      }
    } catch (Exception e) {
      log.warn("Failed to process incoming SSE fan-out message: {}", e.getMessage());
    }
  }

  /**
   * Handles a fan-out message received from Redis (originating from this pod or another one).
   *
   * <p>A message that originated from this same pod is ignored — it was already delivered locally
   * at the moment it was first sent, before being published. Otherwise, the message is delivered
   * to this pod's own locally-held emitters only ({@link #deliverLocal}/{@link
   * #deliverLocalBroadcast}) — this method must never call {@link #publishRemote}, or messages
   * would re-broadcast across pods indefinitely.
   *
   * @param msg the fan-out message
   */
  void onRemoteFanOut(SseFanOutMessage msg) {
    if (instanceId.equals(msg.originInstanceId())) {
      return;
    }
    if (msg.userId() != null) {
      deliverLocal(msg.userId(), msg.eventName(), msg.payload());
    } else {
      deliverLocalBroadcast(msg.eventName(), msg.payload());
    }
  }

  /**
   * Number of currently active SSE streams held by this pod. Useful for monitoring/metrics. Note
   * this counts total live connections across all users, not distinct users — one user with two
   * open tabs contributes two.
   *
   * @return count of open emitters, summed across all users
   */
  public int getActiveCount() {
    return emitters.values().stream().mapToInt(Set::size).sum();
  }
}
