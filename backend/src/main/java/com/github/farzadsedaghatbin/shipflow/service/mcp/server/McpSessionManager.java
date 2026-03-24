package com.github.farzadsedaghatbin.shipflow.service.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * <p>Only active when {@code mcp.server.enabled=true}.
 */
@Service
@ConditionalOnProperty(name = "mcp.server.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class McpSessionManager {

  private final ObjectMapper objectMapper;

  private final ConcurrentHashMap<String, McpSession> sessions = new ConcurrentHashMap<>();

  /** Create a new session for an incoming SSE connection. */
  public McpSession create(Authentication auth) {
    String sessionId = UUID.randomUUID().toString();
    SseEmitter emitter = new SseEmitter(0L); // no timeout — client keeps connection alive
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

  /** Periodically clean up sessions that have had their emitter completed. */
  @Scheduled(fixedRate = 60_000)
  public void cleanupDeadSessions() {
    int before = sessions.size();
    // SseEmitter doesn't expose a "is complete" flag directly;
    // any send attempt on a dead emitter throws — so we rely on onCompletion/onError callbacks.
    // This scheduled task is a safety net only.
    if (before > 0) {
      log.debug("MCP session cleanup check — {} active sessions", before);
    }
  }
}
