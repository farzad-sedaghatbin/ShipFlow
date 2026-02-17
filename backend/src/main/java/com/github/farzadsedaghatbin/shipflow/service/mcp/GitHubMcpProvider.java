package com.github.farzadsedaghatbin.shipflow.service.mcp;

import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * GitHub MCP client implementation.
 * Connects to a GitHub MCP server to read repository files and code.
 * 
 * MCP Server expected endpoints:
 * - POST /tools/list_files: List files in a repository
 * - POST /tools/read_file: Read a single file's content
 * - POST /tools/search_files: Search for files matching a pattern
 * - POST /tools/get_repo_context: Get repository metadata (languages, tech stack)
 */
@Service
@Slf4j
public class GitHubMcpProvider implements McpClientService {

    private final McpConfig mcpConfig;
    private final RestTemplate mcpRestTemplate;
    private final OrganizationSettingsService settingsService;

    public GitHubMcpProvider(
            McpConfig mcpConfig,
            @Qualifier("mcpRestTemplate") RestTemplate mcpRestTemplate,
            OrganizationSettingsService settingsService) {
        this.mcpConfig = mcpConfig;
        this.mcpRestTemplate = mcpRestTemplate;
        this.settingsService = settingsService;
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
    @SuppressWarnings("unchecked")
    public List<String> listFiles(Map<String, String> context) {
        if (!isAvailable()) {
            log.warn("GitHub MCP not available, cannot list files");
            return List.of();
        }

        String owner = context.get("owner");
        String repo = context.get("repo");
        String branch = context.getOrDefault("branch", "main");

        log.info("Listing files for {}/{} on branch {} via MCP", owner, repo, branch);

        try {
            String url = mcpConfig.getGithub().getServerUrl() + "/mcp/v1/tools/call";
            
            Map<String, Object> arguments = Map.of(
                "owner", owner,
                "repo", repo,
                "branch", branch
            );
            
            Map<String, Object> params = Map.of(
                "name", "list_files",
                "arguments", arguments
            );
            
            Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "method", "tools/call",
                "params", params,
                "id", System.currentTimeMillis()
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
                        List<String> files = new ArrayList<>();
                        
                        for (Map<String, Object> item : contentList) {
                            if (item == null) {
                                continue;
                            }
                            Object textObj = item.get("text");
                            if (textObj != null) {
                                String text = textObj.toString().trim();
                                if (!text.isEmpty()) {
                                    // Parse each line as a file path
                                    String[] lines = text.split("\\n");
                                    for (String line : lines) {
                                        String trimmed = line.trim();
                                        if (!trimmed.isEmpty()) {
                                            files.add(trimmed);
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (!files.isEmpty()) {
                            log.debug("Retrieved {} files from GitHub MCP", files.size());
                            return files;
                        }
                    }
                }
            }
            
            log.debug("No files returned from GitHub MCP for {}/{}", owner, repo);
            return List.of();

        } catch (RestClientException e) {
            log.error("Failed to list files via GitHub MCP: {}", e.getMessage());
            return List.of();
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
            
            Map<String, Object> arguments = Map.of(
                "owner", owner,
                "repo", repo,
                "branch", branch,
                "path", filePath
            );
            
            Map<String, Object> params = Map.of(
                "name", "read_file",
                "arguments", arguments
            );
            
            Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "method", "tools/call",
                "params", params,
                "id", System.currentTimeMillis()
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
                            log.debug("Successfully read file {} ({} chars)", filePath, 
                                text != null ? text.length() : 0);
                            return Optional.ofNullable(text);
                        }
                    }
                }
            }

            log.debug("No content returned from GitHub MCP for file: {}", filePath);
            return Optional.empty();

        } catch (RestClientException e) {
            log.error("Failed to read file {} via GitHub MCP: {}", filePath, e.getMessage());
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
            
            Map<String, Object> arguments = Map.of(
                "owner", owner,
                "repo", repo,
                "pattern", pattern
            );
            
            Map<String, Object> params = Map.of(
                "name", "search_files",
                "arguments", arguments
            );
            
            Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "method", "tools/call",
                "params", params,
                "id", System.currentTimeMillis()
            );

            HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
            ResponseEntity<Map<String, Object>> response = mcpRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("matches")) {
                Object matches = response.getBody().get("matches");
                if (matches instanceof List) {
                    log.debug("Found {} files matching pattern '{}' via MCP", 
                        ((List<?>) matches).size(), pattern);
                    return (List<String>) matches;
                }
            }

            log.debug("No matches returned from GitHub MCP for pattern: {}", pattern);
            return List.of();

        } catch (RestClientException e) {
            log.error("Failed to search files via GitHub MCP: {}", e.getMessage());
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

        log.info("Getting repository context for {}/{} via MCP", owner, repo);

        try {
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
                "id", System.currentTimeMillis()
            );

            HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
            ResponseEntity<Map<String, Object>> response = mcpRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null) {
                log.debug("Retrieved repository context for {}/{}", owner, repo);
                return response.getBody();
            }

            log.debug("No context returned from GitHub MCP for {}/{}", owner, repo);
            return Map.of();

        } catch (RestClientException e) {
            log.error("Failed to get repo context via GitHub MCP: {}", e.getMessage());
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
                "id", System.currentTimeMillis()
            );

            HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
            ResponseEntity<Map<String, Object>> response = mcpRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("files")) {
                Object files = response.getBody().get("files");
                if (files instanceof Map) {
                    return (Map<String, String>) files;
                }
            }
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
        } else {
            log.warn("GitHub access token not configured, MCP requests may fail");
        }
        
        return new HttpEntity<>(body, headers);
    }
}
