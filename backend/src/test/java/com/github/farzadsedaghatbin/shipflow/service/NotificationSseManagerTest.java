package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Unit tests for {@link NotificationSseManager}.
 *
 * <p>All tests run without a Spring context — the manager has no external dependencies.
 *
 * <p>Note: {@link SseEmitter} completion/timeout callbacks are invoked by the servlet container
 * during live HTTP processing. In plain unit tests there is no HTTP context, so we cannot rely on
 * {@code emitter.complete()} triggering the {@code onCompletion} callback. The lifecycle-callback
 * tests below are instead covered by verifying that a second {@code subscribe()} call for the same
 * user replaces the first emitter, which exercises the same removal path in the production code.
 */
class NotificationSseManagerTest {

  private NotificationSseManager manager;

  @BeforeEach
  void setUp() {
    manager = new NotificationSseManager();
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
  void subscribe_replacesExistingEmitterForSameUser() {
    // Subscribing the same user twice must not grow the emitter map beyond 1
    manager.subscribe(1L);
    manager.subscribe(1L);
    assertThat(manager.getActiveCount()).isEqualTo(1);
  }

  @Test
  void subscribe_differentUsersEachGetOwnEmitter() {
    SseEmitter e1 = manager.subscribe(10L);
    SseEmitter e2 = manager.subscribe(20L);
    assertThat(e1).isNotSameAs(e2);
    assertThat(manager.getActiveCount()).isEqualTo(2);
  }

  @Test
  void subscribe_returnsDistinctEmitterOnResubscribe() {
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
  void getActiveCount_doesNotDoubleCountResubscribedUser() {
    manager.subscribe(1L);
    manager.subscribe(2L);
    manager.subscribe(1L); // resubscribe user 1 — should not add a second entry
    // Users 1 and 2 each have exactly one entry
    assertThat(manager.getActiveCount()).isEqualTo(2);
  }
}
