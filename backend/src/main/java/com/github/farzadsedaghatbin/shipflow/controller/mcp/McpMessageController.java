package com.github.farzadsedaghatbin.shipflow.controller.mcp;

import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpToolDispatcher;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
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
 * <p>Only active when {@code mcp.server.enabled=true}.
 */
@RestController
@RequestMapping("/mcp")
@ConditionalOnProperty(name = "mcp.server.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class McpMessageController {

  private final McpToolDispatcher dispatcher;

  @PostMapping("/messages")
  public ResponseEntity<Void> handleMessage(
      @RequestParam String sessionId,
      @RequestBody Map<String, Object> request) {

    log.debug("MCP message received: session={} method={}", sessionId, request.get("method"));

    // Dispatch asynchronously so the HTTP response returns immediately (202 Accepted).
    // The actual result is pushed back via the SSE stream.
    Thread.startVirtualThread(() -> dispatcher.dispatch(sessionId, request));

    return ResponseEntity.accepted().build();
  }
}
