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
import org.springframework.web.bind.annotation.PathVariable;
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
 * <p>Two auth transports are supported (see {@code McpAuthFilter} for details):
 *
 * <ul>
 *   <li>Header (primary): {@code GET /mcp/sse} with {@code Authorization: Bearer <api-key>}.
 *   <li>URL path token (secondary, read-only): {@code GET /mcp/<api-key>/sse} — for clients that
 *       cannot set custom headers. The {@code endpoint} event echoes back the same transport
 *       ({@code /mcp/<api-key>/messages?sessionId=<uuid>}) so the client's subsequent POSTs stay
 *       on the path-token transport.
 * </ul>
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

  @GetMapping(value = {"/sse", "/{apiKeyPathToken}/sse"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter connect(@PathVariable(required = false) String apiKeyPathToken) {
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

    // Send the endpoint event — tells the client where to send JSON-RPC messages, echoing back
    // whichever transport (header vs. path token) the client used to connect. Echoing the path
    // token here is safe: by the time this method runs, McpAuthFilter has already validated it
    // against a real ApiKey hash, so it can't be arbitrary attacker-controlled input.
    String messagesUrl =
        apiKeyPathToken != null
            ? "/mcp/" + apiKeyPathToken + "/messages?sessionId=" + sessionId
            : "/mcp/messages?sessionId=" + sessionId;
    try {
      emitter.send(SseEmitter.event().name("endpoint").data(messagesUrl));
      log.info("MCP client connected, session={}", sessionId);
    } catch (IOException e) {
      log.error("Failed to send MCP endpoint event for session {}", sessionId, e);
      emitter.completeWithError(e);
      sessionManager.remove(sessionId);
    }

    return emitter;
  }
}
