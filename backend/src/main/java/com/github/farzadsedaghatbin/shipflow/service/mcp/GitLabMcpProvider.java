package com.github.farzadsedaghatbin.shipflow.service.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * GitLab MCP client implementation. Reads repository files from GitLab (gitlab.com or a
 * self-hosted instance) to provide code context for AI features such as Wise Architecture.
 *
 * <p><strong>Design note — direct REST API v4, not a JSON-RPC {@code tools/call} MCP server:</strong>
 * {@link GitHubMcpProvider}, {@link FigmaMcpProvider}, {@link NotionMcpProvider}, and {@link
 * ConfluenceMcpProvider} all proxy through a separate MCP-server process, sending a JSON-RPC
 * {@code tools/call} envelope and letting that intermediary translate the call into a vendor API
 * request. GitLab has no similarly ubiquitous, ready-to-run MCP server to target, but it does
 * expose a stable, well-documented REST API v4 directly on the instance itself (self-hosted or
 * gitlab.com) — so this provider calls {@code {serverUrl}/api/v4/...} directly. {@code
 * McpConfig.GitLabMcpConfig#serverUrl} is therefore the GitLab *instance* base URL, not an
 * MCP-server endpoint. Everything else (constructor shape, {@link #isAvailable()} gating,
 * error-handling/logging conventions, the 10-minute file-list cache) mirrors {@link
 * GitHubMcpProvider} as closely as the underlying protocol difference allows.
 *
 * <p>Context map keys used by every method below:
 *
 * <ul>
 *   <li>{@code projectId} (required) — a GitLab project identifier: either the numeric project
 *       ID, or the URL-encodable {@code namespace/project} path (e.g. {@code group/sub/repo}).
 *   <li>{@code ref} (optional) — branch, tag, or commit SHA; defaults to {@code "main"}.
 * </ul>
 *
 * @see <a href="https://docs.gitlab.com/ee/api/rest/">GitLab REST API</a>
 */
@Service
@Slf4j
public class GitLabMcpProvider implements McpClientService {

  private static final String DEFAULT_REF = "main";
  private static final int MAX_FILES = 1000;
  private static final int MAX_PAGES = 20;
  private static final int PER_PAGE = 100;

  private final McpConfig mcpConfig;
  private final RestTemplate mcpRestTemplate;
  private final OrganizationSettingsService settingsService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  // In-memory cache for file lists (TTL: 10 minutes), mirroring GitHubMcpProvider.
  private static final long CACHE_TTL_MS = 10 * 60 * 1000;
  private final ConcurrentHashMap<String, CachedFileList> fileListCache = new ConcurrentHashMap<>();

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

  public GitLabMcpProvider(
      McpConfig mcpConfig,
      @Qualifier("mcpRestTemplate") RestTemplate mcpRestTemplate,
      OrganizationSettingsService settingsService) {
    this.mcpConfig = mcpConfig;
    this.mcpRestTemplate = mcpRestTemplate;
    this.settingsService = settingsService;
  }

  /** Cleanup expired cache entries periodically, mirroring GitHubMcpProvider. */
  @Scheduled(fixedRate = 60000)
  public void cleanupExpiredCache() {
    int initialSize = fileListCache.size();
    fileListCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    int removed = initialSize - fileListCache.size();
    if (removed > 0) {
      log.debug("Cleaned up {} expired GitLab file list cache entries", removed);
    }
  }

  /** Clear the file list cache for a specific project. */
  public void clearFileListCache(String projectId, String ref) {
    fileListCache.remove(buildCacheKey(projectId, ref));
    log.debug("Cleared GitLab file list cache for {} (ref: {})", projectId, ref);
  }

  private String buildCacheKey(String projectId, String ref) {
    return projectId + "@" + (ref != null ? ref : DEFAULT_REF);
  }

  @Override
  public boolean isAvailable() {
    McpConfig.GitLabMcpConfig config = mcpConfig.getGitlab();
    boolean available =
        config.isEnabled() && config.getServerUrl() != null && !config.getServerUrl().isBlank();
    if (!available) {
      log.debug(
          "GitLab MCP is not available - enabled: {}, serverUrl: {}",
          config.isEnabled(),
          config.getServerUrl());
    }
    return available;
  }

  @Override
  public String getProviderType() {
    return "gitlab";
  }

  @Override
  public List<String> listFiles(Map<String, String> context) {
    if (!isAvailable()) {
      log.warn("GitLab MCP not available, cannot list files");
      return List.of();
    }

    String projectId = context.get("projectId");
    if (projectId == null || projectId.isBlank()) {
      log.warn("GitLab MCP listFiles called without a 'projectId' context key");
      return List.of();
    }
    String ref = context.getOrDefault("ref", DEFAULT_REF);

    String cacheKey = buildCacheKey(projectId, ref);
    CachedFileList cached = fileListCache.get(cacheKey);
    if (cached != null && !cached.isExpired()) {
      log.info("Using cached file list for GitLab project {} ({} files)", projectId, cached.files.size());
      return cached.files;
    }

    log.info("Listing files for GitLab project {} on ref {} via REST API v4", projectId, ref);

    List<String> allFiles = new ArrayList<>();
    try {
      int page = 1;
      while (page <= MAX_PAGES && allFiles.size() < MAX_FILES) {
        String url = baseUrl() + "/projects/" + encodeSegment(projectId) + "/repository/tree"
            + "?ref=" + encodeQueryValue(ref)
            + "&recursive=true&per_page=" + PER_PAGE + "&page=" + page;

        ResponseEntity<String> response =
            mcpRestTemplate.exchange(URI.create(url), HttpMethod.GET, createEntity(), String.class);

        JsonNode entries = parseJson(response.getBody());
        if (entries == null || !entries.isArray() || entries.isEmpty()) {
          break;
        }
        for (JsonNode entry : entries) {
          if ("blob".equals(textOrNull(entry, "type"))) {
            String path = textOrNull(entry, "path");
            if (path != null) {
              allFiles.add(path);
            }
          }
        }

        String nextPage = response.getHeaders().getFirst("X-Next-Page");
        if (nextPage == null || nextPage.isBlank()) {
          break;
        }
        page++;
      }

      log.info("Retrieved {} files from GitLab project {}", allFiles.size(), projectId);
      fileListCache.put(cacheKey, new CachedFileList(allFiles));
      return allFiles;

    } catch (HttpClientErrorException e) {
      log.error(
          "Failed to list files via GitLab REST API — HTTP {} from [{}]: {}",
          e.getStatusCode(),
          mcpConfig.getGitlab().getServerUrl(),
          e.getResponseBodyAsString());
      return List.of();
    } catch (RestClientException e) {
      log.error(
          "Failed to list files via GitLab REST API [{}]: {}",
          mcpConfig.getGitlab().getServerUrl(),
          e.getMessage());
      return List.of();
    }
  }

  @Override
  public Optional<String> readFile(Map<String, String> context, String filePath) {
    if (!isAvailable()) {
      log.warn("GitLab MCP not available, cannot read file: {}", filePath);
      return Optional.empty();
    }

    String projectId = context.get("projectId");
    if (projectId == null || projectId.isBlank()) {
      log.warn("GitLab MCP readFile called without a 'projectId' context key");
      return Optional.empty();
    }
    String ref = context.getOrDefault("ref", DEFAULT_REF);

    log.info("Reading file {}:{}/{} via GitLab REST API v4", projectId, ref, filePath);

    try {
      String url = baseUrl() + "/projects/" + encodeSegment(projectId) + "/repository/files/"
          + encodeSegment(filePath) + "/raw?ref=" + encodeQueryValue(ref);

      ResponseEntity<String> response =
          mcpRestTemplate.exchange(URI.create(url), HttpMethod.GET, createEntity(), String.class);

      String body = response.getBody();
      log.debug("Successfully read file {} ({} chars)", filePath, body != null ? body.length() : 0);
      return Optional.ofNullable(body);

    } catch (HttpClientErrorException e) {
      log.error(
          "Failed to read file {} via GitLab REST API — HTTP {} from [{}]: {}",
          filePath,
          e.getStatusCode(),
          mcpConfig.getGitlab().getServerUrl(),
          e.getResponseBodyAsString());
      if (e.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
        log.error("403 on file read — verify the GitLab PAT has 'read_repository' scope and project access");
      }
      return Optional.empty();
    } catch (RestClientException e) {
      log.error(
          "Failed to read file {} via GitLab REST API [{}]: {}",
          filePath,
          mcpConfig.getGitlab().getServerUrl(),
          e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Search for files matching a pattern using GitLab's advanced search API scoped to blobs.
   *
   * @param context must contain {@code projectId}; may contain {@code ref}
   * @param pattern search term(s), passed through to GitLab's {@code search=} query parameter
   * @return list of matching file paths
   */
  @Override
  public List<String> searchFiles(Map<String, String> context, String pattern) {
    if (!isAvailable()) {
      log.warn("GitLab MCP not available, cannot search files with pattern: {}", pattern);
      return List.of();
    }

    String projectId = context.get("projectId");
    if (projectId == null || projectId.isBlank()) {
      log.warn("GitLab MCP searchFiles called without a 'projectId' context key");
      return List.of();
    }
    String ref = context.get("ref");

    log.info("Searching files in GitLab project {} with pattern '{}' via REST API v4", projectId, pattern);

    try {
      StringBuilder url = new StringBuilder(baseUrl())
          .append("/projects/").append(encodeSegment(projectId))
          .append("/search?scope=blobs&search=").append(encodeQueryValue(pattern));
      if (ref != null && !ref.isBlank()) {
        url.append("&ref=").append(encodeQueryValue(ref));
      }

      ResponseEntity<String> response =
          mcpRestTemplate.exchange(URI.create(url.toString()), HttpMethod.GET, createEntity(), String.class);

      JsonNode results = parseJson(response.getBody());
      List<String> matches = new ArrayList<>();
      if (results != null && results.isArray()) {
        for (JsonNode item : results) {
          String path = textOrNull(item, "path");
          if (path != null) {
            matches.add(path);
          }
        }
      }

      log.debug("Found {} files matching pattern '{}' in GitLab project {}", matches.size(), pattern, projectId);
      return matches;

    } catch (HttpClientErrorException e) {
      log.error(
          "Failed to search files via GitLab REST API — HTTP {} from [{}]: {}",
          e.getStatusCode(),
          mcpConfig.getGitlab().getServerUrl(),
          e.getResponseBodyAsString());
      return List.of();
    } catch (RestClientException e) {
      log.error(
          "Failed to search files via GitLab REST API [{}]: {}",
          mcpConfig.getGitlab().getServerUrl(),
          e.getMessage());
      return List.of();
    }
  }

  /**
   * Bundle basic GitLab project metadata (name, description, default branch, visibility, ...) as
   * a resource-context map, fetched from GitLab's project-details endpoint.
   *
   * @param context must contain {@code projectId}
   * @return the project's JSON attributes as a map, or an empty map if unavailable
   */
  @Override
  public Map<String, Object> getResourceContext(Map<String, String> context) {
    if (!isAvailable()) {
      log.warn("GitLab MCP not available, cannot get resource context");
      return Map.of();
    }

    String projectId = context.get("projectId");
    if (projectId == null || projectId.isBlank()) {
      log.warn("GitLab MCP getResourceContext called without a 'projectId' context key");
      return Map.of();
    }

    log.debug("Getting project context for GitLab project {} via REST API v4", projectId);

    try {
      String url = baseUrl() + "/projects/" + encodeSegment(projectId);

      ResponseEntity<String> response =
          mcpRestTemplate.exchange(URI.create(url), HttpMethod.GET, createEntity(), String.class);

      JsonNode project = parseJson(response.getBody());
      if (project == null || !project.isObject()) {
        return Map.of();
      }

      Map<String, Object> resultMap = new LinkedHashMap<>();
      resultMap.put("provider", "gitlab");
      objectMapper.convertValue(project, Map.class).forEach((k, v) -> resultMap.put((String) k, v));
      return resultMap;

    } catch (HttpClientErrorException e) {
      log.error(
          "Failed to get project context via GitLab REST API — HTTP {} from [{}]: {}",
          e.getStatusCode(),
          mcpConfig.getGitlab().getServerUrl(),
          e.getResponseBodyAsString());
      return Map.of();
    } catch (RestClientException e) {
      log.error(
          "Failed to get project context via GitLab REST API [{}]: {}",
          mcpConfig.getGitlab().getServerUrl(),
          e.getMessage());
      return Map.of();
    }
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  private String baseUrl() {
    String serverUrl = mcpConfig.getGitlab().getServerUrl();
    String trimmed = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
    return trimmed + "/api/v4";
  }

  /**
   * URL-encode a path segment (project ID/path or file path) per GitLab's requirement that
   * slashes in a namespaced project path or nested file path be percent-encoded as {@code %2F}.
   * {@link URLEncoder} already encodes '/' for the {@code application/x-www-form-urlencoded}
   * target, but it also encodes spaces as '+' rather than '%20' — fixed up below.
   */
  private String encodeSegment(String raw) {
    return URLEncoder.encode(raw, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /** Encode a query-string value; same fix-up as {@link #encodeSegment(String)}. */
  private String encodeQueryValue(String raw) {
    return URLEncoder.encode(raw, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /**
   * Build the request headers, authenticating with GitLab's own {@code PRIVATE-TOKEN} header
   * convention for a Personal Access Token (rather than {@code Authorization: Bearer}, which
   * GitLab reserves for OAuth2 tokens).
   */
  private HttpEntity<Void> createEntity() {
    HttpHeaders headers = new HttpHeaders();
    String token = settingsService.getGitlabAccessToken();
    if (token != null && !token.isBlank()) {
      headers.set("PRIVATE-TOKEN", token);
      log.debug(
          "GitLab PAT present: ***{} (len={})",
          token.substring(Math.max(0, token.length() - 4)),
          token.length());
    } else {
      log.warn("GitLab access token not configured — requests will be unauthenticated and will 403/404 on private projects");
    }
    return new HttpEntity<>(headers);
  }

  private JsonNode parseJson(String body) {
    if (body == null || body.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readTree(body);
    } catch (Exception e) {
      log.debug("Failed to parse GitLab response as JSON: {}", e.getMessage());
      return null;
    }
  }

  private String textOrNull(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value != null && !value.isNull() ? value.asText() : null;
  }
}
