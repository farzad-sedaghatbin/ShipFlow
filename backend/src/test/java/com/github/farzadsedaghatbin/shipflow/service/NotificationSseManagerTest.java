package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RecordingEmitterHandler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Unit tests for {@link NotificationSseManager}.
 *
 * <p>Most tests construct the manager with a {@code null} {@link RedisTemplate} — local-only
 * mode, matching either a single-instance deployment or the {@code test} Spring profile (which
 * never activates the real Redis-backed {@code RedisTemplate} bean, since {@code
 * spring.cache.type=simple} there). A dedicated group of tests further down constructs the manager
 * with a mocked {@link RedisTemplate} to cover the cross-pod fan-out path.
 *
 * <p>Lifecycle-callback and per-emitter delivery tests use {@link RecordingEmitterHandler}, a
 * small test-only bridge class living in the {@code
 * org.springframework.web.servlet.mvc.method.annotation} package. Both {@link
 * SseEmitter#initialize} and the {@code Handler} interface it accepts are package-private in
 * Spring — the exact seam Spring's own MVC infrastructure uses to wire a live emitter to a servlet
 * response — so a bridge class in that same package is what lets these tests drive
 * onCompletion/onTimeout/onError and record actual sent payloads without a real HTTP container.
 * Note that {@code ResponseBodyEmitter} buffers anything sent before a handler is attached and
 * replays it on {@code initialize()} — the "connected" heartbeat {@code subscribe()} sends is
 * therefore present in a freshly-attached handler's recorded list, so tests assert their expected
 * payload is present rather than asserting an exact list size.
 */
@ExtendWith(MockitoExtension.class)
class NotificationSseManagerTest {

  @Mock
  private RedisTemplate<String, Object> mockRedisTemplate;

  private NotificationSseManager manager;

  @BeforeEach
  void setUp() {
    manager = new NotificationSseManager(null);
  }

  // ── subscribe ────────────────────────────────────────────────────────────────

  @Test
  void subscribe_returnsNonNullEmitter() {
    SseEmitter emitter = manager.subscribe(1L);
    assertThat(emitter).isNotNull();
  }

  @Test
  void subscribe_incrementsActiveCount() {
    assertThat(manager.getActiveCount()).isZero();
    manager.subscribe(1L);
    assertThat(manager.getActiveCount()).isEqualTo(1);
    manager.subscribe(2L);
    assertThat(manager.getActiveCount()).isEqualTo(2);
  }

  @Test
  void subscribe_sameUserTwice_addsSecondEmitterWithoutEvictingFirst() {
    SseEmitter first = manager.subscribe(1L);
    AtomicBoolean firstCompleted = new AtomicBoolean(false);
    first.onCompletion(() -> firstCompleted.set(true));

    manager.subscribe(1L);

    assertThat(manager.getActiveCount()).isEqualTo(2);
    assertThat(firstCompleted).isFalse();
  }

  @Test
  void subscribe_differentUsersEachGetOwnEmitter() {
    SseEmitter e1 = manager.subscribe(10L);
    SseEmitter e2 = manager.subscribe(20L);
    assertThat(e1).isNotSameAs(e2);
    assertThat(manager.getActiveCount()).isEqualTo(2);
  }

  @Test
  void subscribe_twiceForSameUser_returnsTwoDistinctEmitters() {
    SseEmitter first = manager.subscribe(42L);
    SseEmitter second = manager.subscribe(42L);
    assertThat(first).isNotSameAs(second);
  }

  // ── sendToUser ───────────────────────────────────────────────────────────────

  @Test
  void sendToUser_withNoActiveEmitter_isNoOp() {
    // Unknown user — must be a silent no-op, no exception
    manager.sendToUser(999L, "some-payload");
    assertThat(manager.getActiveCount()).isZero();
  }

  @Test
  void sendToUser_withStaleUser_doesNotThrow() {
    // Subscribe, then call sendToUser for a different unknown user — no exception
    manager.subscribe(7L);
    manager.sendToUser(99L, "payload");
    // original emitter still registered
    assertThat(manager.getActiveCount()).isEqualTo(1);
  }

  @Test
  void sendToUser_deliversToAllEmittersForSameUser() throws IOException {
    SseEmitter e1 = manager.subscribe(1L);
    SseEmitter e2 = manager.subscribe(1L);
    RecordingEmitterHandler h1 = RecordingEmitterHandler.attach(e1);
    RecordingEmitterHandler h2 = RecordingEmitterHandler.attach(e2);

    manager.sendToUser(1L, "payload-1");

    assertThat(h1.getReceived()).contains("payload-1");
    assertThat(h2.getReceived()).contains("payload-1");
  }

  // ── broadcast ────────────────────────────────────────────────────────────────

  @Test
  void broadcast_deliversToEveryUsersEveryEmitter() throws IOException {
    SseEmitter u1e1 = manager.subscribe(1L);
    SseEmitter u1e2 = manager.subscribe(1L);
    SseEmitter u2e1 = manager.subscribe(2L);
    RecordingEmitterHandler h1 = RecordingEmitterHandler.attach(u1e1);
    RecordingEmitterHandler h2 = RecordingEmitterHandler.attach(u1e2);
    RecordingEmitterHandler h3 = RecordingEmitterHandler.attach(u2e1);

    manager.broadcast("announcement", "hello-everyone");

    assertThat(h1.getReceived()).contains("hello-everyone");
    assertThat(h2.getReceived()).contains("hello-everyone");
    assertThat(h3.getReceived()).contains("hello-everyone");
  }

  // ── sendEventToUser ──────────────────────────────────────────────────────────

  @Test
  void sendEventToUser_onlyReachesTargetUser() throws IOException {
    SseEmitter targetEmitter = manager.subscribe(1L);
    SseEmitter otherEmitter = manager.subscribe(2L);
    RecordingEmitterHandler targetHandler = RecordingEmitterHandler.attach(targetEmitter);
    RecordingEmitterHandler otherHandler = RecordingEmitterHandler.attach(otherEmitter);

    manager.sendEventToUser(1L, "retro-updated", "payload");

    assertThat(targetHandler.getReceived()).contains("payload");
    assertThat(otherHandler.getReceived()).doesNotContain("payload");
  }

  // ── failure handling ─────────────────────────────────────────────────────────

  @Test
  void sendFailureOnOneEmitter_removesOnlyThatEmitter() throws IOException {
    SseEmitter goodEmitter = manager.subscribe(1L);
    SseEmitter badEmitter = manager.subscribe(1L);
    RecordingEmitterHandler goodHandler = RecordingEmitterHandler.attach(goodEmitter);
    RecordingEmitterHandler badHandler = RecordingEmitterHandler.attach(badEmitter);
    badHandler.failOnNextSend();

    int before = manager.getActiveCount();
    manager.sendToUser(1L, "payload");

    assertThat(manager.getActiveCount()).isEqualTo(before - 1);
    assertThat(goodHandler.getReceived()).contains("payload");
  }

  @Test
  void emptyEmitterSet_isPrunedFromMap() throws IOException {
    SseEmitter onlyEmitter = manager.subscribe(1L);
    RecordingEmitterHandler handler = RecordingEmitterHandler.attach(onlyEmitter);
    handler.failOnNextSend();

    manager.sendToUser(1L, "payload"); // fails, removes the only emitter for user 1
    assertThat(manager.getActiveCount()).isZero();

    // A fresh subscription afterward must behave normally — no leaked empty-set map entry for
    // user 1 skewing the count.
    manager.subscribe(2L);
    assertThat(manager.getActiveCount()).isEqualTo(1);
  }

  // ── getActiveCount ───────────────────────────────────────────────────────────

  @Test
  void getActiveCount_initiallyZero() {
    assertThat(manager.getActiveCount()).isZero();
  }

  @Test
  void getActiveCount_reflectsMultipleDistinctSubscriptions() {
    manager.subscribe(1L);
    manager.subscribe(2L);
    manager.subscribe(3L);
    assertThat(manager.getActiveCount()).isEqualTo(3);
  }

  @Test
  void getActiveCount_countsEachEmitterAcrossAllUsers() {
    manager.subscribe(1L);
    manager.subscribe(2L);
    manager.subscribe(1L); // second emitter for user 1 — additive, not a replacement
    assertThat(manager.getActiveCount()).isEqualTo(3);
  }

  // ── Redis fan-out ────────────────────────────────────────────────────────────

  @Test
  void sendToUser_publishesToRedisWhenTemplatePresent() {
    NotificationSseManager redisManager = new NotificationSseManager(mockRedisTemplate);

    redisManager.sendToUser(5L, "payload");

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(mockRedisTemplate)
        .convertAndSend(eq(NotificationSseManager.FANOUT_CHANNEL), captor.capture());
    SseFanOutMessage fanOut = (SseFanOutMessage) captor.getValue();
    assertThat(fanOut.userId()).isEqualTo(5L);
    assertThat(fanOut.eventName()).isEqualTo("notification");
    assertThat(fanOut.payload()).isEqualTo("payload");
    assertThat(fanOut.originInstanceId()).isNotBlank();
  }

  @Test
  void broadcast_publishesWithNullUserId() {
    NotificationSseManager redisManager = new NotificationSseManager(mockRedisTemplate);

    redisManager.broadcast("announcement", "payload");

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(mockRedisTemplate)
        .convertAndSend(eq(NotificationSseManager.FANOUT_CHANNEL), captor.capture());
    SseFanOutMessage fanOut = (SseFanOutMessage) captor.getValue();
    assertThat(fanOut.userId()).isNull();
    assertThat(fanOut.eventName()).isEqualTo("announcement");
  }

  @Test
  void onRemoteFanOut_ignoresOwnEcho() throws IOException {
    NotificationSseManager redisManager = new NotificationSseManager(mockRedisTemplate);
    SseEmitter emitter = redisManager.subscribe(1L);
    RecordingEmitterHandler handler = RecordingEmitterHandler.attach(emitter);
    handler.getReceived().clear(); // discard the buffered "connected" heartbeat

    SseFanOutMessage echo =
        new SseFanOutMessage(redisManager.instanceId, 1L, "notification", "echoed-payload");
    redisManager.onRemoteFanOut(echo);

    assertThat(handler.getReceived()).doesNotContain("echoed-payload");
  }

  @Test
  void onRemoteFanOut_deliversMessageFromOtherPod() throws IOException {
    NotificationSseManager redisManager = new NotificationSseManager(mockRedisTemplate);
    SseEmitter emitter = redisManager.subscribe(1L);
    RecordingEmitterHandler handler = RecordingEmitterHandler.attach(emitter);

    SseFanOutMessage fromOtherPod =
        new SseFanOutMessage(UUID.randomUUID().toString(), 1L, "notification", "remote-payload");
    redisManager.onRemoteFanOut(fromOtherPod);

    assertThat(handler.getReceived()).contains("remote-payload");
  }

  @Test
  void onRemoteFanOut_broadcastFromOtherPod_reachesAllLocalEmitters() throws IOException {
    NotificationSseManager redisManager = new NotificationSseManager(mockRedisTemplate);
    SseEmitter e1 = redisManager.subscribe(1L);
    SseEmitter e2 = redisManager.subscribe(2L);
    RecordingEmitterHandler h1 = RecordingEmitterHandler.attach(e1);
    RecordingEmitterHandler h2 = RecordingEmitterHandler.attach(e2);

    SseFanOutMessage broadcastFromOtherPod =
        new SseFanOutMessage(
            UUID.randomUUID().toString(), null, "announcement", "remote-broadcast");
    redisManager.onRemoteFanOut(broadcastFromOtherPod);

    assertThat(h1.getReceived()).contains("remote-broadcast");
    assertThat(h2.getReceived()).contains("remote-broadcast");
  }
}
