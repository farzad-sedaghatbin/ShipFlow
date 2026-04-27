package com.github.farzadsedaghatbin.shipflow.config.mcp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the ShipFlow MCP server.
 *
 * <p>The MCP server is opt-in. All self-hosted instances run with {@code mcp.server.enabled=false}
 * by default. Enable it only when you want AI coding assistants (Claude Code, Cursor, etc.) to
 * connect directly to this ShipFlow instance.
 *
 * <p>Environment variable equivalents:
 *
 * <ul>
 *   <li>{@code MCP_SERVER_ENABLED=true} — enable the server
 *   <li>{@code MCP_SERVER_WRITE_ENABLED=true} — allow write tools (create/update)
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.server")
@Getter
@Setter
public class McpServerProperties {

  /** Whether the MCP server is enabled. Default: false. */
  private boolean enabled = false;

  /** Whether write tools (create_task, update_task_status, add_comment…) are available. */
  private boolean writeEnabled = false;

  /** Server name reported in the MCP initialize handshake. */
  private String serverName = "shipflow";
}
