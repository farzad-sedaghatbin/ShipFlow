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

    /**
     * Notion MCP server configuration.
     */
    private NotionMcpConfig notion = new NotionMcpConfig();

    /**
     * Confluence MCP server configuration.
     */
    private ConfluenceMcpConfig confluence = new ConfluenceMcpConfig();

    /**
     * GitLab MCP server configuration.
     */
    private GitLabMcpConfig gitlab = new GitLabMcpConfig();

    /**
     * Azure DevOps MCP server configuration.
     */
    private AzureDevOpsMcpConfig azureDevOps = new AzureDevOpsMcpConfig();

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

    @Getter
    @Setter
    public static class NotionMcpConfig {
        /**
         * Whether Notion MCP integration is enabled.
         * Environment variable: MCP_NOTION_ENABLED
         */
        private boolean enabled = false;

        /**
         * Notion MCP server URL.
         * Environment variable: MCP_NOTION_SERVER_URL
         */
        private String serverUrl;

        /**
         * Connection timeout in seconds.
         */
        private int timeoutSeconds = 30;
    }

    @Getter
    @Setter
    public static class ConfluenceMcpConfig {
        /**
         * Whether Confluence MCP integration is enabled.
         * Environment variable: MCP_CONFLUENCE_ENABLED
         */
        private boolean enabled = false;

        /**
         * Confluence MCP server URL.
         * Environment variable: MCP_CONFLUENCE_SERVER_URL
         */
        private String serverUrl;

        /**
         * Connection timeout in seconds.
         */
        private int timeoutSeconds = 30;
    }

    @Getter
    @Setter
    public static class GitLabMcpConfig {
        /**
         * Whether GitLab MCP integration is enabled.
         * Environment variable: MCP_GITLAB_ENABLED
         */
        private boolean enabled = false;

        /**
         * GitLab instance base URL (e.g. https://gitlab.com or a self-hosted
         * instance such as https://gitlab.example.com). {@link GitLabMcpProvider}
         * calls this instance's REST API v4 directly (no separate MCP-server
         * intermediary process, unlike {@link GitHubMcpConfig#getServerUrl()}) —
         * see {@link GitLabMcpProvider}'s class Javadoc for why.
         * Environment variable: MCP_GITLAB_SERVER_URL
         */
        private String serverUrl;

        /**
         * Connection timeout in seconds.
         */
        private int timeoutSeconds = 30;
    }

    @Getter
    @Setter
    public static class AzureDevOpsMcpConfig {
        /**
         * Whether Azure DevOps MCP integration is enabled.
         * Environment variable: MCP_AZURE_DEVOPS_ENABLED
         */
        private boolean enabled = false;

        /**
         * Azure DevOps MCP server URL. This is the MCP server endpoint (not the Azure DevOps
         * REST API itself), matching the {@code /mcp/v1/tools/call} convention used by the
         * other providers in this file. Works against both dev.azure.com (Azure DevOps
         * Services) and a self-hosted Azure DevOps Server base URL, since the actual
         * organization/project/repository routing is passed as tool arguments, not baked into
         * this URL.
         * Environment variable: MCP_AZURE_DEVOPS_SERVER_URL
         */
        private String serverUrl;

        /**
         * Connection timeout in seconds.
         */
        private int timeoutSeconds = 30;
    }
}
