package com.github.farzadsedaghatbin.shipflow.controller.mcp;

import com.github.farzadsedaghatbin.shipflow.config.mcp.McpServerProperties;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpSessionManager;
import com.github.farzadsedaghatbin.shipflow.service.mcp.server.McpToolDispatcher;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public health endpoint for the MCP server.
 *
 * <p>No authentication required. Always available regardless of {@code mcp.server.enabled}, so
 * operators can check the MCP server status without an API key.
 *
 * <p>Response example when enabled:
 *
 * <pre>{@code
 * {
 *   "status": "UP",
 *   "mcpServer": {
 *     "enabled": true,
 *     "writeEnabled": true,
 *     "activeSessions": 2,
 *     "toolCount": 11
 *   }
 * }
 * }</pre>
 *
 * <p>Response when disabled:
 *
 * <pre>{@code
 * {
 *   "status": "UP",
 *   "mcpServer": {
 *     "enabled": false
 *   }
 * }
 * }</pre>
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpHealthController {

  private final McpServerProperties properties;

  // ObjectProvider allows optional injection — these beans only exist when MCP is enabled
  private final ObjectProvider<McpSessionManager> sessionManagerProvider;
  private final ObjectProvider<McpToolDispatcher> dispatcherProvider;

  @GetMapping("/health")
  public Map<String, Object> health() {
    Map<String, Object> mcpInfo = new LinkedHashMap<>();
    mcpInfo.put("enabled", properties.isEnabled());

    if (properties.isEnabled()) {
      mcpInfo.put("writeEnabled", properties.isWriteEnabled());
      McpSessionManager mgr = sessionManagerProvider.getIfAvailable();
      McpToolDispatcher dispatcher = dispatcherProvider.getIfAvailable();
      mcpInfo.put("activeSessions", mgr != null ? mgr.activeCount() : 0);
      mcpInfo.put("toolCount", dispatcher != null ? dispatcher.toolCount() : 0);
    }

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("status", "UP");
    response.put("mcpServer", mcpInfo);
    return response;
  }
}
