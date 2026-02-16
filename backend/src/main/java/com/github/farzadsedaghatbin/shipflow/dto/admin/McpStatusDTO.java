package com.github.farzadsedaghatbin.shipflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for MCP (Model Context Protocol) server status information.
 * Contains system-wide configuration from environment variables.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McpStatusDTO {

    /**
     * GitHub MCP server status.
     */
    private McpServerStatus github;

    /**
     * Figma MCP server status.
     */
    private McpServerStatus figma;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class McpServerStatus {
        /**
         * Whether the MCP server is enabled (from environment variable).
         */
        private boolean enabled;

        /**
         * Whether a server URL is configured (without exposing the actual URL).
         */
        private boolean configured;

        /**
         * Masked server URL for display.
         */
        private String serverUrlMasked;

        /**
         * Connection timeout in seconds.
         */
        private int timeoutSeconds;
    }

    /**
     * Helper method to mask a server URL for safe display.
     */
    public static String maskServerUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            java.net.URI uri = java.net.URI.create(url);
            String scheme = uri.getScheme();
            String path = uri.getPath();
            String lastSegment = path != null && !path.isEmpty()
                ? path.substring(path.lastIndexOf('/'))
                : "";
            // Handle missing scheme (e.g., "localhost:3100")
            if (scheme == null || scheme.isBlank()) {
                return "****" + lastSegment;
            }
            return scheme + "://****" + lastSegment;
        } catch (Exception e) {
            return "****";
        }
    }
}
