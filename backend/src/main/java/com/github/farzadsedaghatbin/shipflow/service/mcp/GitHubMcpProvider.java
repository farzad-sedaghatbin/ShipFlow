package com.github.farzadsedaghatbin.shipflow.service.mcp;

import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * GitHub MCP client implementation.
 * Connects to a GitHub MCP server to read repository files and code.
 * 
 * Uses standard GitHub MCP tools:
 * - get_file_contents: Read file content or list directory contents
 * - search_code: Search for code matching a pattern
 * 
 * Includes in-memory caching for file lists to avoid repeated MCP calls
 * during the same detection session.
 * 
 * @see <a href="https://github.com/modelcontextprotocol/servers/tree/main/src/github">GitHub MCP Server</a>
 */
@Service
@Slf4j
public class GitHubMcpProvider implements McpClientService {

    private final McpConfig mcpConfig;
    private final RestTemplate mcpRestTemplate;
    private final OrganizationSettingsService settingsService;

    // In-memory cache for file lists (TTL: 10 minutes)
    private static final long CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes
    private final ConcurrentHashMap<String, CachedFileList> fileListCache = new ConcurrentHashMap<>();

    /**
     * Cache entry for file lists with expiration.
     */
    private static class CachedFileList {
        final List<String> files;
        final long expiresAt;

        CachedFileList(List<String> files) {
            this.files = files;
            this.expiresAt = System.currentTimeMillis() + CACHE_TTL_MS;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    public GitHubMcpProvider(
            McpConfig mcpConfig,
            @Qualifier("mcpRestTemplate") RestTemplate mcpRestTemplate,
            OrganizationSettingsService settingsService) {
        this.mcpConfig = mcpConfig;
        this.mcpRestTemplate = mcpRestTemplate;
        this.settingsService = settingsService;
    }

    /**
     * Cleanup expired cache entries periodically.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void cleanupExpiredCache() {
        // Use removeIf for atomic check-and-remove
        int initialSize = fileListCache.size();
        fileListCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removed = initialSize - fileListCache.size();
        
        if (removed > 0) {
            log.debug("Cleaned up {} expired file list cache entries", removed);
        }
    }

    /**
     * Clear the file list cache for a specific repository.
     * Called when force re-detection is requested.
     */
    public void clearFileListCache(String owner, String repo, String branch) {
        String cacheKey = buildCacheKey(owner, repo, branch);
        fileListCache.remove(cacheKey);
        log.debug("Cleared file list cache for {}/{} (branch: {})", owner, repo, branch);
    }

    /**
     * Clear all file list cache entries.
     */
    public void clearAllFileListCache() {
        int size = fileListCache.size();
        fileListCache.clear();
        log.info("Cleared all {} file list cache entries", size);
    }

    private String buildCacheKey(String owner, String repo, String branch) {
        return owner + "/" + repo + "@" + (branch != null ? branch : "main");
    }

    /**
     * Heuristic: a string looks like a file path if it either:
     * - contains a '/' or '.', and does NOT look like a plain-english error message; or
     * - is a single-token filename (no spaces) without obvious sentence-like patterns.
     * Rejects lines like "missing required parameter: query" while accepting "src/main/App.java"
     * and extensionless files like "README" or "LICENSE".
     */
    private boolean looksLikeFilePath(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        boolean hasPathOrDot = trimmed.contains("/") || trimmed.contains(".");

        // Special-case: allow single-token filenames without '/' or '.', like "README" or "LICENSE"
        if (!hasPathOrDot) {
            // Reject if there is any internal whitespace (must be a single token)
            if (trimmed.split("\\s+").length > 1) {
                return false;
            }
            // Only allow common filename characters; reject lines with punctuation typical of sentences
            return trimmed.matches("[A-Za-z0-9_\\-]+");
        }

        // Reject lines with spaces that look like sentences/error messages
        // File paths rarely contain multiple spaces or colons followed by spaces
        if (trimmed.contains(": ") || (trimmed.split("\\s+").length > 4)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isAvailable() {
        McpConfig.GitHubMcpConfig config = mcpConfig.getGithub();
        boolean available = config.isEnabled() && 
            config.getServerUrl() != null && 
            !config.getServerUrl().isBlank();
        
        if (!available) {
            log.debug("GitHub MCP is not available - enabled: {}, serverUrl: {}", 
                config.isEnabled(), config.getServerUrl());
        }
        return available;
    }

    @Override
    public String getProviderType() {
        return "github";
    }

    @Override
    public List<String> listFiles(Map<String, String> context) {
        if (!isAvailable()) {
            log.warn("GitHub MCP not available, cannot list files");
            return List.of();
        }

        String owner = context.get("owner");
        String repo = context.get("repo");
        String branch = context.getOrDefault("branch", "main");

        // Check cache first
        String cacheKey = buildCacheKey(owner, repo, branch);
        CachedFileList cached = fileListCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.info("Using cached file list for {}/{} ({} files)", owner, repo, cached.files.size());
            return cached.files;
        }

        log.info("Listing files for {}/{} on branch {} via MCP", owner, repo, branch);

        try {
            // Use get_file_contents with empty path to get repository root, then recurse
            List<String> allFiles = new ArrayList<>();
            fetchFilesRecursively(owner, repo, branch, "", allFiles, 0);
            
            if (!allFiles.isEmpty()) {
                log.info("Retrieved {} files from GitHub MCP for {}/{}", allFiles.size(), owner, repo);
                log.debug("Sample files: {}", 
                    allFiles.stream().limit(10).collect(java.util.stream.Collectors.joining(", ")));
                
                // Cache the result
                fileListCache.put(cacheKey, new CachedFileList(allFiles));
            } else {
                log.warn("No files retrieved from repository {}/{}", owner, repo);
            }
            
            return allFiles;

        } catch (HttpClientErrorException e) {
            log.error("Failed to list files via GitHub MCP — HTTP {} from [{}]: {}",
                e.getStatusCode(), mcpConfig.getGithub().getServerUrl(), e.getResponseBodyAsString());
            return List.of();
        } catch (RestClientException e) {
            log.error("Failed to list files via GitHub MCP [{}]: {}",
                mcpConfig.getGithub().getServerUrl(), e.getMessage());
            return List.of();
        }
    }

    /**
     * Recursively fetch files from a directory using get_file_contents.
     */
    @SuppressWarnings("unchecked")
    private void fetchFilesRecursively(String owner, String repo, String branch, 
            String path, List<String> files, int depth) {
        // Limit recursion depth to prevent infinite loops and excessive API calls
        if (depth > 5) {
            log.debug("Max recursion depth reached for path: {}", path);
            return;
        }
        
        // Limit total files to prevent memory issues
        if (files.size() > 1000) {
            log.debug("Max file limit reached, stopping recursion");
            return;
        }

        try {
            String url = mcpConfig.getGithub().getServerUrl() + "/mcp/v1/tools/call";
            
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("owner", owner);
            arguments.put("repo", repo);
            arguments.put("path", path.isEmpty() ? "" : path);
            if (branch != null && !branch.isEmpty()) {
                arguments.put("branch", branch);
            }
            
            Map<String, Object> params = Map.of(
                "name", "get_file_contents",
                "arguments", arguments
            );
            
            Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "method", "tools/call",
                "params", params,
                "id", java.util.UUID.randomUUID().toString()
            );

            HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
            ResponseEntity<Map<String, Object>> response = mcpRestTemplate.exchange(
                url, HttpMethod.POST, entity, 
                new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("result")) {
                Object result = response.getBody().get("result");
                if (result instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    Object content = resultMap.get("content");
                    if (content instanceof List) {
                        List<Map<String, Object>> contentList = (List<Map<String, Object>>) content;
                        
                        for (Map<String, Object> item : contentList) {
                            if (item == null) continue;
                            
                            Object textObj = item.get("text");
                            if (textObj != null) {
                                String text = textObj.toString().trim();
                                // Try to parse as JSON (directory listing)
                                try {
                                    Object parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                                        .readValue(text, Object.class);
                                    if (parsed instanceof List) {
                                        // Directory listing - array of file entries
                                        List<Map<String, Object>> entries = (List<Map<String, Object>>) parsed;
                                        for (Map<String, Object> entry : entries) {
                                            String entryPath = (String) entry.get("path");
                                            String type = (String) entry.get("type");
                                            
                                            if (entryPath != null) {
                                                if ("file".equals(type)) {
                                                    files.add(entryPath);
                                                } else if ("dir".equals(type)) {
                                                    // Recurse into subdirectory
                                                    fetchFilesRecursively(owner, repo, branch, 
                                                        entryPath, files, depth + 1);
                                                }
                                            }
                                        }
                                    } else if (parsed instanceof Map) {
                                        // Single file entry
                                        Map<String, Object> entry = (Map<String, Object>) parsed;
                                        String entryPath = (String) entry.get("path");
                                        if (entryPath != null) {
                                            files.add(entryPath);
                                        }
                                    }
                                } catch (Exception e) {
                                    // Not JSON - might be raw file content or line-delimited paths
                                    if (!text.isEmpty()) {
                                        String[] lines = text.split("\\n");
                                        for (String line : lines) {
                                            String trimmed = line.trim();
                                            if (!trimmed.isEmpty() && !trimmed.startsWith("{") 
                                                    && !trimmed.startsWith("[") && looksLikeFilePath(trimmed)) {
                                                files.add(trimmed);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("error")) {
                    Object error = body.get("error");
                    if (depth == 0) {
                        log.error("MCP returned error for {}/{}: {}", owner, repo, error);
                    } else {
                        log.debug("MCP error fetching path '{}': {}", path, error);
                    }
                }
            }
        } catch (HttpClientErrorException e) {
            if (depth == 0) {
                log.error("HTTP {} listing root of {}/{} via MCP [{}]: {}",
                    e.getStatusCode(), owner, repo, mcpConfig.getGithub().getServerUrl(),
                    e.getResponseBodyAsString());
            } else {
                log.debug("HTTP {} fetching path '{}' in {}/{}: {}",
                    e.getStatusCode(), path, owner, repo, e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            if (depth == 0) {
                log.error("Error listing root of {}/{} via MCP: {}", owner, repo, e.getMessage());
            } else {
                log.debug("Error fetching path '{}': {}", path, e.getMessage());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<String> readFile(Map<String, String> context, String filePath) {
        if (!isAvailable()) {
            log.warn("GitHub MCP not available, cannot read file: {}", filePath);
            return Optional.empty();
        }

        String owner = context.get("owner");
        String repo = context.get("repo");
        String branch = context.getOrDefault("branch", "main");

        log.info("Reading file {}/{}/{}:{} via MCP", owner, repo, branch, filePath);

        try {
            String url = mcpConfig.getGithub().getServerUrl() + "/mcp/v1/tools/call";
            
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("owner", owner);
            arguments.put("repo", repo);
            arguments.put("path", filePath);
            if (branch != null && !branch.isEmpty()) {
                arguments.put("branch", branch);
            }
            
            Map<String, Object> params = Map.of(
                "name", "get_file_contents",
                "arguments", arguments
            );
            
            Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "method", "tools/call",
                "params", params,
                "id", java.util.UUID.randomUUID().toString()
            );

            HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
            ResponseEntity<Map<String, Object>> response = mcpRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("result")) {
                Object result = response.getBody().get("result");
                if (result instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    Object content = resultMap.get("content");
                    if (content instanceof List) {
                        List<Map<String, Object>> contentList = (List<Map<String, Object>>) content;
                        if (!contentList.isEmpty() && contentList.get(0) != null && contentList.get(0).containsKey("text")) {
                            String text = (String) contentList.get(0).get("text");
                            log.debug("Successfully read file {} ({} chars)", filePath, 
                                text != null ? text.length() : 0);
                            return Optional.ofNullable(text);
                        }
                    }
                }
            } else {
                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("error")) {
                    log.error("MCP returned error reading file {}: {}", filePath, body.get("error"));
                } else {
                    log.warn("MCP response missing 'result' key for file {}. Response: {}", filePath, body);
                }
            }

            log.debug("No content returned from GitHub MCP for file: {}", filePath);
            return Optional.empty();

        } catch (HttpClientErrorException e) {
            log.error("Failed to read file {} via GitHub MCP — HTTP {} from [{}]: {}",
                filePath, e.getStatusCode(), mcpConfig.getGithub().getServerUrl(),
                e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 403) {
                log.error("403 on file read — verify the GitHub token has 'repo' scope and access to the repository");
            }
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Failed to read file {} via GitHub MCP [{}]: {}",
                filePath, mcpConfig.getGithub().getServerUrl(), e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> searchFiles(Map<String, String> context, String pattern) {
        if (!isAvailable()) {
            log.warn("GitHub MCP not available, cannot search files with pattern: {}", pattern);
            return List.of();
        }

        String owner = context.get("owner");
        String repo = context.get("repo");

        log.info("Searching files in {}/{} with pattern '{}' via MCP", owner, repo, pattern);

        try {
            String url = mcpConfig.getGithub().getServerUrl() + "/mcp/v1/tools/call";
            
            // Use search_code tool with query scoped to the repository
            Map<String, Object> arguments = Map.of(
                "q", pattern + " repo:" + owner + "/" + repo
            );
            
            Map<String, Object> params = Map.of(
                "name", "search_code",
                "arguments", arguments
            );
            
            Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "method", "tools/call",
                "params", params,
                "id", java.util.UUID.randomUUID().toString()
            );

            HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
            ResponseEntity<Map<String, Object>> response = mcpRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("result")) {
                Object result = response.getBody().get("result");
                if (result instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    Object content = resultMap.get("content");
                    if (content instanceof List) {
                        List<Map<String, Object>> contentList = (List<Map<String, Object>>) content;
                        List<String> matches = new ArrayList<>();
                        
                        for (Map<String, Object> item : contentList) {
                            if (item == null) {
                                continue;
                            }
                            Object textObj = item.get("text");
                            if (textObj != null) {
                                String text = textObj.toString().trim();
                                if (!text.isEmpty()) {
                                    // Parse each line as a matching file path
                                    String[] lines = text.split("\\n");
                                    for (String line : lines) {
                                        String trimmed = line.trim();
                                        if (!trimmed.isEmpty()) {
                                            matches.add(trimmed);
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (!matches.isEmpty()) {
                            log.debug("Found {} files matching pattern '{}' via MCP", matches.size(), pattern);
                            return matches;
                        }
                    }
                }
            } else {
                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("error")) {
                    log.error("MCP returned error searching files: {}", body.get("error"));
                } else {
                    log.warn("MCP response missing 'result' key for search pattern '{}'. Response: {}", pattern, body);
                }
            }

            log.debug("No matches returned from GitHub MCP for pattern: {}", pattern);
            return List.of();

        } catch (HttpClientErrorException e) {
            log.error("Failed to search files via GitHub MCP — HTTP {} from [{}]: {}",
                e.getStatusCode(), mcpConfig.getGithub().getServerUrl(), e.getResponseBodyAsString());
            return List.of();
        } catch (RestClientException e) {
            log.error("Failed to search files via GitHub MCP [{}]: {}",
                mcpConfig.getGithub().getServerUrl(), e.getMessage());
            return List.of();
        }
    }

    @Override
    public Map<String, Object> getResourceContext(Map<String, String> context) {
        if (!isAvailable()) {
            log.warn("GitHub MCP not available, cannot get resource context");
            return Map.of();
        }

        String owner = context.get("owner");
        String repo = context.get("repo");

        log.debug("Getting repository context for {}/{} via MCP", owner, repo);

        try {
            // Note: get_repo_context is not a standard GitHub MCP tool
            // This may fail if using a standard MCP server - that's expected
            String url = mcpConfig.getGithub().getServerUrl() + "/mcp/v1/tools/call";
            
            Map<String, Object> arguments = Map.of(
                "owner", owner,
                "repo", repo
            );
            
            Map<String, Object> params = Map.of(
                "name", "get_repo_context",
                "arguments", arguments
            );
            
            Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "method", "tools/call",
                "params", params,
                "id", java.util.UUID.randomUUID().toString()
            );

            HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
            ResponseEntity<Map<String, Object>> response = mcpRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("result")) {
                Object result = response.getBody().get("result");
                if (result instanceof Map) {
                    log.debug("Retrieved repository context for {}/{}", owner, repo);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    return resultMap;
                }
            } else {
                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("error")) {
                    log.error("MCP returned error for repo context {}/{}: {}", owner, repo, body.get("error"));
                } else {
                    log.warn("MCP response missing 'result' key for {}/{}. Response: {}", owner, repo, body);
                }
            }

            log.debug("No context returned from GitHub MCP for {}/{}", owner, repo);
            return Map.of();

        } catch (HttpClientErrorException e) {
            log.error("Failed to get repo context via GitHub MCP — HTTP {} from [{}]: {}",
                e.getStatusCode(), mcpConfig.getGithub().getServerUrl(), e.getResponseBodyAsString());
            return Map.of();
        } catch (RestClientException e) {
            log.error("Failed to get repo context via GitHub MCP [{}]: {}",
                mcpConfig.getGithub().getServerUrl(), e.getMessage());
            return Map.of();
        }
    }

    /**
     * Read multiple files efficiently via batch API.
     * Falls back to individual reads if batch not supported.
     *
     * @param context repository context (owner, repo, branch)
     * @param filePaths list of file paths to read
     * @return map of file path to content
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> readFiles(Map<String, String> context, List<String> filePaths) {
        if (!isAvailable() || filePaths.isEmpty()) {
            return Map.of();
        }

        String owner = context.get("owner");
        String repo = context.get("repo");
        String branch = context.getOrDefault("branch", "main");

        log.info("Reading {} files from {}/{} via MCP", filePaths.size(), owner, repo);

        try {
            // Try batch read first
            String url = mcpConfig.getGithub().getServerUrl() + "/mcp/v1/tools/call";
            
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("owner", owner);
            arguments.put("repo", repo);
            arguments.put("branch", branch);
            arguments.put("paths", filePaths);
            
            Map<String, Object> params = Map.of(
                "name", "read_files",
                "arguments", arguments
            );
            
            Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "method", "tools/call",
                "params", params,
                "id", java.util.UUID.randomUUID().toString()
            );

            HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
            ResponseEntity<Map<String, Object>> response = mcpRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("result")) {
                Object result = response.getBody().get("result");
                if (result instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    Object content = resultMap.get("content");
                    if (content instanceof List) {
                        List<Map<String, Object>> contentList = (List<Map<String, Object>>) content;
                        if (!contentList.isEmpty() && contentList.get(0).containsKey("text")) {
                            String text = (String) contentList.get(0).get("text");
                            // Parse the text as JSON map of file paths to contents
                            try {
                                Map<String, String> filesMap = new com.fasterxml.jackson.databind.ObjectMapper()
                                    .readValue(text, Map.class);
                                return filesMap;
                            } catch (Exception e) {
                                log.debug("Failed to parse batch file response as JSON: {}", e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (HttpClientErrorException e) {
            log.warn("Batch file read returned HTTP {} from [{}] — falling back to individual reads: {}",
                e.getStatusCode(), mcpConfig.getGithub().getServerUrl(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.debug("Batch file read not available, falling back to individual reads: {}",
                e.getMessage());
        }

        // Fall back to individual reads
        Map<String, String> results = new HashMap<>();
        for (String path : filePaths) {
            readFile(context, path).ifPresent(content -> results.put(path, content));
        }
        return results;
    }

    /**
     * Create an HTTP entity with JSON content type and Authorization headers.
     */
    private <T> HttpEntity<T> createJsonEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/event-stream");
        
        // Add GitHub token for authentication
        String githubToken = settingsService.getGithubAccessToken();
        if (githubToken != null && !githubToken.isBlank()) {
            headers.set("Authorization", "Bearer " + githubToken);
            log.debug("GitHub token present: ***{} (len={})",
                githubToken.substring(Math.max(0, githubToken.length() - 4)),
                githubToken.length());
        } else {
            log.warn("GitHub access token not configured — MCP requests will be unauthenticated and will 403 on private repos");
        }
        
        return new HttpEntity<>(body, headers);
    }
}
