package com.github.farzadsedaghatbin.shipflow.service.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Manages active MCP SSE sessions.
 *
 * <p>Each client that opens {@code GET /mcp/sse} gets a unique session ID. The session tracks the
 * SSE emitter and the authenticated user. When the client POSTs to {@code /mcp/messages}, the
 * dispatcher looks up the session to send the JSON-RPC response back through the SSE stream.
 *
 * <p>This bean always exists; whether the MCP server actually responds to requests is decided at
 * runtime by {@link McpServerSettingsService} (env default + admin toggle).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpSessionManager {

  private final ObjectMapper objectMapper;

  private final ConcurrentHashMap<String, McpSession> sessions = new ConcurrentHashMap<>();

  /** SSE emitter timeout — 30 minutes. Clients must reconnect after this. */
  private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1_000L;

  /** Sessions older than this with no activity will be swept by the cleanup task. */
  private static final long SESSION_MAX_IDLE_MS = EMITTER_TIMEOUT_MS + 60_000L;

  /** Create a new session for an incoming SSE connection. */
  public McpSession create(Authentication auth) {
    String sessionId = UUID.randomUUID().toString();
    SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
    McpSession session = new McpSession(sessionId, emitter, auth, Instant.now());
    sessions.put(sessionId, session);
    log.debug("MCP session created: {} (active sessions: {})", sessionId, sessions.size());
    return session;
  }

  /** Look up an existing session by ID. */
  public Optional<McpSession> get(String sessionId) {
    return Optional.ofNullable(sessions.get(sessionId));
  }

  /** Remove a session (called on SSE completion, timeout, or error). */
  public void remove(String sessionId) {
    sessions.remove(sessionId);
    log.debug("MCP session removed: {} (active sessions: {})", sessionId, sessions.size());
  }

  /**
   * Send a JSON-RPC response or notification to the client through its SSE stream.
   *
   * @param sessionId the target session
   * @param payload the JSON-RPC response object (will be serialised to JSON)
   * @throws IOException if the SSE write fails (client disconnected)
   * @throws IllegalArgumentException if session not found
   */
  public void send(String sessionId, Object payload) throws IOException {
    McpSession session = sessions.get(sessionId);
    if (session == null) {
      log.warn("Attempted to send to non-existent MCP session: {}", sessionId);
      throw new IllegalArgumentException("MCP session not found: " + sessionId);
    }
    String json = objectMapper.writeValueAsString(payload);
    session.getEmitter().send(SseEmitter.event().name("message").data(json));
    log.debug("Sent MCP message to session {}: {}...", sessionId, json.substring(0, Math.min(80, json.length())));
  }

  /** Number of currently active MCP sessions. */
  public int activeCount() {
    return sessions.size();
  }

  /**
   * Periodically remove sessions whose SSE emitter timed out but whose {@code onCompletion} /
   * {@code onTimeout} callback was never fired (e.g., proxy dropped the connection silently).
   *
   * <p>Sessions are identified as stale by comparing {@link McpSession#getCreatedAt()} against
   * {@link #SESSION_MAX_IDLE_MS}. This is a conservative safety net — normal cleanup happens via
   * the SSE callbacks registered in {@link
   * com.github.farzadsedaghatbin.shipflow.controller.mcp.McpSseController}.
   */
  @Scheduled(fixedRate = 60_000)
  public void cleanupDeadSessions() {
    Instant cutoff = Instant.now().minusMillis(SESSION_MAX_IDLE_MS);
    int before = sessions.size();
    sessions.entrySet().removeIf(e -> {
      boolean stale = e.getValue().getCreatedAt().isBefore(cutoff);
      if (stale) {
        log.info("MCP session evicted (idle timeout): {}", e.getKey());
        e.getValue().getEmitter().complete();
      }
      return stale;
    });
    int removed = before - sessions.size();
    if (removed > 0) {
      log.info("MCP session cleanup: removed {} stale session(s), {} active", removed, sessions.size());
    }
  }
}
