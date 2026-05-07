package com.github.farzadsedaghatbin.shipflow.service.mcp;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for MCP (Model Context Protocol) client operations.
 * Provides a unified interface for interacting with different MCP servers.
 */
public interface McpClientService {

    /**
     * Check if the MCP client is available and properly configured.
     *
     * @return true if the client is ready to use
     */
    boolean isAvailable();

    /**
     * Get the type/name of this MCP provider.
     *
     * @return provider type identifier
     */
    String getProviderType();

    /**
     * List files in a repository or project.
     *
     * @param context provider-specific context (e.g., repo path, project ID)
     * @return list of file paths
     */
    List<String> listFiles(Map<String, String> context);

    /**
     * Read the content of a file.
     *
     * @param context provider-specific context
     * @param filePath path to the file
     * @return file content, or empty if not accessible
     */
    Optional<String> readFile(Map<String, String> context, String filePath);

    /**
     * Search for files matching a pattern.
     *
     * @param context provider-specific context
     * @param pattern search pattern (glob or regex)
     * @return list of matching file paths
     */
    List<String> searchFiles(Map<String, String> context, String pattern);

    /**
     * Get structured context/metadata about a resource.
     *
     * @param context provider-specific context
     * @return structured context information
     */
    Map<String, Object> getResourceContext(Map<String, String> context);
}
