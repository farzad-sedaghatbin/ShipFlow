package com.github.farzadsedaghatbin.shipflow.controller.mcp;

import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpServerSettingsService;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpSession;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpSessionManager;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * MCP SSE transport endpoint.
 *
 * <p>MCP clients open a long-lived SSE connection here to establish a session. The server
 * immediately sends an {@code endpoint} event telling the client where to POST its JSON-RPC
 * messages ({@code /mcp/messages?sessionId=<uuid>}).
 *
 * <p>Authentication: {@code Authorization: Bearer <api-key>} header (handled by
 * {@code McpAuthFilter}).
 *
 * <p>The MCP server is enabled/disabled at runtime via {@link McpServerSettingsService} (admin
 * toggle, falling back to the {@code mcp.server.enabled} environment default). When disabled, this
 * endpoint responds {@code 503 Service Unavailable}.
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpSseController {

  private final McpSessionManager sessionManager;
  private final McpServerSettingsService serverSettings;

  @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter connect() {
    if (!serverSettings.isEnabled()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "MCP server is disabled on this ShipFlow instance");
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    McpSession session = sessionManager.create(auth);
    String sessionId = session.getSessionId();
    SseEmitter emitter = session.getEmitter();

    // Wire lifecycle callbacks before sending events (avoid race conditions)
    emitter.onCompletion(() -> {
      log.debug("MCP SSE connection completed for session {}", sessionId);
      sessionManager.remove(sessionId);
    });
    emitter.onTimeout(() -> {
      log.debug("MCP SSE connection timed out for session {}", sessionId);
      sessionManager.remove(sessionId);
    });
    emitter.onError(ex -> {
      log.debug("MCP SSE connection error for session {}: {}", sessionId, ex.getMessage());
      sessionManager.remove(sessionId);
    });

    // Send the endpoint event — tells the client where to send JSON-RPC messages
    try {
      emitter.send(
          SseEmitter.event()
              .name("endpoint")
              .data("/mcp/messages?sessionId=" + sessionId));
      log.info("MCP client connected, session={}", sessionId);
    } catch (IOException e) {
      log.error("Failed to send MCP endpoint event for session {}", sessionId, e);
      emitter.completeWithError(e);
      sessionManager.remove(sessionId);
    }

    return emitter;
  }
}
