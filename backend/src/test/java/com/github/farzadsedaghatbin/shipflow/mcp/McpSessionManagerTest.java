package com.github.farzadsedaghatbin.shipflow.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpSession;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpSessionManager;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

/**
 * Unit tests for {@link McpSessionManager} — verifies session lifecycle (create, get, remove) and
 * concurrent-access safety without starting a Spring context.
 */
@ExtendWith(MockitoExtension.class)
class McpSessionManagerTest {

  @Mock private Authentication auth;

  private McpSessionManager manager;

  @BeforeEach
  void setUp() {
    manager = new McpSessionManager(new ObjectMapper());
  }

  // ── create ─────────────────────────────────────────────────────────────────

  @Test
  void create_returnsSessionWithUniqueId() {
    McpSession s1 = manager.create(auth);
    McpSession s2 = manager.create(auth);

    assertThat(s1.getSessionId()).isNotBlank();
    assertThat(s2.getSessionId()).isNotBlank();
    assertThat(s1.getSessionId()).isNotEqualTo(s2.getSessionId());
  }

  @Test
  void create_incrementsActiveCount() {
    assertThat(manager.activeCount()).isZero();
    manager.create(auth);
    assertThat(manager.activeCount()).isEqualTo(1);
    manager.create(auth);
    assertThat(manager.activeCount()).isEqualTo(2);
  }

  @Test
  void create_sessionHoldsProvidedAuthentication() {
    McpSession session = manager.create(auth);
    assertThat(session.getAuth()).isSameAs(auth);
  }

  @Test
  void create_sessionHasEmitter() {
    McpSession session = manager.create(auth);
    assertThat(session.getEmitter()).isNotNull();
  }

  // ── get ────────────────────────────────────────────────────────────────────

  @Test
  void get_returnsSessionAfterCreate() {
    McpSession session = manager.create(auth);
    Optional<McpSession> found = manager.get(session.getSessionId());

    assertThat(found).isPresent();
    assertThat(found.get().getSessionId()).isEqualTo(session.getSessionId());
  }

  @Test
  void get_returnsEmptyForUnknownId() {
    Optional<McpSession> found = manager.get("non-existent-id");
    assertThat(found).isEmpty();
  }

  // ── remove ─────────────────────────────────────────────────────────────────

  @Test
  void remove_decreasesActiveCount() {
    McpSession session = manager.create(auth);
    assertThat(manager.activeCount()).isEqualTo(1);

    manager.remove(session.getSessionId());
    assertThat(manager.activeCount()).isZero();
  }

  @Test
  void remove_sessionNoLongerRetrievable() {
    McpSession session = manager.create(auth);
    manager.remove(session.getSessionId());

    assertThat(manager.get(session.getSessionId())).isEmpty();
  }

  @Test
  void remove_unknownId_doesNotThrow() {
    // should be safe to call with a non-existent ID
    manager.remove("does-not-exist");
    assertThat(manager.activeCount()).isZero();
  }

  // ── send ───────────────────────────────────────────────────────────────────

  @Test
  void send_unknownSession_throwsIllegalArgument() {
    assertThatThrownBy(() -> manager.send("ghost-session", Map.of("foo", "bar")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ghost-session");
  }

  // ── concurrent access ──────────────────────────────────────────────────────

  @Test
  void concurrentCreate_allSessionsDistinct() throws InterruptedException {
    int threads = 20;
    java.util.Set<String> ids = java.util.concurrent.ConcurrentHashMap.newKeySet();
    java.util.List<Thread> workers = new java.util.ArrayList<>();

    for (int i = 0; i < threads; i++) {
      workers.add(new Thread(() -> ids.add(manager.create(auth).getSessionId())));
    }
    workers.forEach(Thread::start);
    for (Thread t : workers) t.join();

    assertThat(ids).hasSize(threads);
    assertThat(manager.activeCount()).isEqualTo(threads);
  }
}
