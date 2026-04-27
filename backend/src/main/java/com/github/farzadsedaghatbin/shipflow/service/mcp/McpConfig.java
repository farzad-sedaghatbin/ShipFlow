package com.github.farzadsedaghatbin.shipflow.service.mcp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for MCP (Model Context Protocol) servers.
 * All properties can be configured via environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "app.mcp")
@Getter
@Setter
public class McpConfig {

    /**
     * GitHub MCP server configuration.
     */
    private GitHubMcpConfig github = new GitHubMcpConfig();

    /**
     * Figma MCP server configuration.
     */
    private FigmaMcpConfig figma = new FigmaMcpConfig();

    @Getter
    @Setter
    public static class GitHubMcpConfig {
        /**
         * Whether GitHub MCP integration is enabled.
         * Environment variable: MCP_GITHUB_ENABLED
         */
        private boolean enabled = false;

        /**
         * GitHub MCP server URL.
         * Environment variable: MCP_GITHUB_SERVER_URL
         */
        private String serverUrl;

        /**
         * Connection timeout in seconds.
         */
        private int timeoutSeconds = 30;
    }

    @Getter
    @Setter
    public static class FigmaMcpConfig {
        /**
         * Whether Figma MCP integration is enabled.
         * Environment variable: MCP_FIGMA_ENABLED
         */
        private boolean enabled = false;

        /**
         * Figma MCP server URL.
         * Environment variable: MCP_FIGMA_SERVER_URL
         */
        private String serverUrl;

        /**
         * Connection timeout in seconds.
         */
        private int timeoutSeconds = 30;
    }
}
