package com.github.farzadsedaghatbin.shipflow.controller.mcp;

import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpServerSettingsService;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpSession;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpSessionManager;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpToolDispatcher;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives JSON-RPC 2.0 messages from MCP clients.
 *
 * <p>Per the MCP HTTP+SSE transport spec, clients POST their JSON-RPC requests here and receive
 * {@code 202 Accepted} immediately. The actual JSON-RPC response is sent asynchronously through
 * the client's open SSE stream ({@code /mcp/sse}).
 *
 * <p>Session ownership is enforced: the authenticated principal on this request must match the
 * principal that opened the target session, preventing one API key from injecting tool calls into
 * another user's session.
 *
 * <p>Two auth transports are supported (see {@code McpAuthFilter} for details): the primary {@code
 * Authorization: Bearer <api-key>} header at {@code POST /mcp/messages}, and a secondary URL
 * path-token transport at {@code POST /mcp/<api-key>/messages} for clients that cannot set custom
 * headers — always capped to read-only regardless of the key's real scopes. Both shapes are
 * handled by this same method; authentication already happened in {@code McpAuthFilter} before the
 * request reaches here, so no extra handling is needed for the path variant.
 *
 * <p>The MCP server is enabled/disabled at runtime via {@link McpServerSettingsService} (admin
 * toggle, falling back to the {@code mcp.server.enabled} environment default). When disabled, this
 * endpoint responds {@code 503 Service Unavailable}.
 */
@RestController
@RequestMapping("/mcp")
@Slf4j
public class McpMessageController {

  private final McpToolDispatcher dispatcher;
  private final McpSessionManager sessionManager;
  private final McpServerSettingsService serverSettings;

  /**
   * Bounded virtual-thread executor — limits concurrent dispatches so MCP traffic cannot exhaust
   * server resources. 100 concurrent MCP tool calls is more than enough for any realistic
   * self-hosted deployment.
   */
  private final ExecutorService mcpExecutor =
      Executors.newFixedThreadPool(100, Thread.ofVirtual().factory());

  public McpMessageController(
      McpToolDispatcher dispatcher,
      McpSessionManager sessionManager,
      McpServerSettingsService serverSettings) {
    this.dispatcher = dispatcher;
    this.sessionManager = sessionManager;
    this.serverSettings = serverSettings;
  }

  @PostMapping({"/messages", "/{apiKeyPathToken}/messages"})
  public ResponseEntity<?> handleMessage(
      @RequestParam String sessionId, @RequestBody Map<String, Object> request) {

    if (!serverSettings.isEnabled()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    // Verify that the caller owns the target session (prevents session-hijacking)
    Optional<McpSession> session = sessionManager.get(sessionId);
    if (session.isEmpty()) {
      log.debug("MCP /messages: session '{}' not found — client must reconnect via GET /mcp/sse", sessionId);
      Object requestId = request.get("id");
      Map<String, Object> error =
          Map.of(
              "jsonrpc", "2.0",
              "id", requestId != null ? requestId : "null",
              "error",
                  Map.of(
                      "code", -32001,
                      "message",
                          "Session not found or expired. Reconnect via GET /mcp/sse to establish a new session.",
                      "data", Map.of("reconnectUrl", "/mcp/sse")));
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .contentType(MediaType.APPLICATION_JSON)
          .body(error);
    }

    Authentication requestAuth = SecurityContextHolder.getContext().getAuthentication();
    Authentication sessionAuth = session.get().getAuth();
    if (requestAuth == null
        || sessionAuth == null
        || !requestAuth.getName().equals(sessionAuth.getName())) {
      log.warn(
          "MCP /messages: session ownership mismatch — request={} session={}",
          requestAuth != null ? requestAuth.getName() : "null",
          sessionAuth != null ? sessionAuth.getName() : "null");
      return ResponseEntity.status(403).build();
    }

    log.debug("MCP message received: session={} method={}", sessionId, request.get("method"));

    // Dispatch asynchronously; the actual result is pushed back via the SSE stream.
    mcpExecutor.submit(() -> dispatcher.dispatch(sessionId, request));

    return ResponseEntity.accepted().build();
  }
}
