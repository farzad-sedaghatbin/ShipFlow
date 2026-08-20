package com.github.farzadsedaghatbin.shipflow.service.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
 * Azure DevOps MCP client implementation. Connects to an Azure DevOps MCP server to read
 * repository files and code from Azure Repos — either Azure DevOps Services
 * ({@code dev.azure.com}) or a self-hosted Azure DevOps Server, since the organization/project/
 * repository routing is passed as tool arguments rather than baked into the server URL.
 *
 * <p>Targets the Azure DevOps REST API 7.1 Git Items / Code Search surface via the underlying MCP
 * server's {@code tools/call}, the same JSON-RPC convention {@link GitHubMcpProvider} and the
 * other providers in this package use. Unlike GitHub's Contents API (one directory level per
 * call), Azure DevOps' Items API supports {@code recursionLevel=Full} to return an entire
 * repository tree in a single call, so {@link #listFiles} does not need
 * {@code GitHubMcpProvider}'s manual recursive directory walk.
 *
 * <p>MCP Server expected tools (design note: these tool names are this provider's own convention,
 * inspired by the Azure DevOps REST API 7.1 operation names — there is no single de-facto
 * standard "Azure DevOps MCP server" this was validated against, unlike the well-established
 * community GitHub MCP server GitHubMcpProvider targets):
 *
 * <ul>
 *   <li>{@code azuredevops_get_items}: List repository items ({@code recursionLevel=Full} for a
 *       full tree), mirroring the Git Items — List REST API
 *   <li>{@code azuredevops_get_item_content}: Read a single file's content by path, mirroring the
 *       Git Items — Get REST API with {@code $format=text}
 *   <li>{@code azuredevops_search_code}: Full-text code search. Azure DevOps' Code Search is a
 *       separate, optional extension (not part of the REST API 7.1 core surface, and not
 *       guaranteed to be installed on a given organization/self-hosted server) — when this tool
 *       errors or returns no matches, {@link #searchFiles} falls back to a client-side filtered
 *       {@link #listFiles} rather than failing outright
 *   <li>{@code azuredevops_get_repository_context}: Non-standard convenience tool for a
 *       repository summary, mirroring {@code GitHubMcpProvider}'s {@code get_repo_context}; not
 *       part of a generic MCP server and may fail — that is expected
 * </ul>
 *
 * <p>Auth: Azure DevOps REST API 7.1 authenticates Personal Access Tokens (PATs) via HTTP Basic
 * auth with an empty username and the PAT as the password (the documented standard for PAT auth
 * against Azure DevOps REST APIs) — unlike GitHub/Notion/Confluence, which use a Bearer token.
 *
 * @see <a href="https://learn.microsoft.com/en-us/rest/api/azure/devops/git/items">Azure DevOps
 *     REST API 7.1 — Git Items</a>
 */
@Service
@Slf4j
public class AzureDevOpsMcpProvider implements McpClientService {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final McpConfig mcpConfig;
  private final RestTemplate mcpRestTemplate;
  private final OrganizationSettingsService settingsService;

  public AzureDevOpsMcpProvider(
      McpConfig mcpConfig,
      @Qualifier("mcpRestTemplate") RestTemplate mcpRestTemplate,
      OrganizationSettingsService settingsService) {
    this.mcpConfig = mcpConfig;
    this.mcpRestTemplate = mcpRestTemplate;
    this.settingsService = settingsService;
  }

  @Override
  public boolean isAvailable() {
    McpConfig.AzureDevOpsMcpConfig config = mcpConfig.getAzureDevOps();
    boolean available =
        config.isEnabled() && config.getServerUrl() != null && !config.getServerUrl().isBlank();
    if (!available) {
      log.debug(
          "Azure DevOps MCP is not available - enabled: {}, serverUrl: {}",
          config.isEnabled(),
          config.getServerUrl());
    }
    return available;
  }

  @Override
  public String getProviderType() {
    return "azure_devops";
  }

  /**
   * List all files in an Azure Repos Git repository.
   *
   * @param context expected keys {@code "organization"}, {@code "project"}, {@code "repository"}
   *     (repository ID or name), and optionally {@code "branch"}
   * @return list of file paths (folders excluded)
   */
  @Override
  public List<String> listFiles(Map<String, String> context) {
    if (!isAvailable()) {
      log.warn("Azure DevOps MCP not available, cannot list files");
      return List.of();
    }

    String organization = context.get("organization");
    String project = context.get("project");
    String repository = context.get("repository");
    String branch = context.get("branch");

    log.info(
        "Listing files for {}/{}/{} via Azure DevOps MCP", organization, project, repository);

    try {
      String url = mcpConfig.getAzureDevOps().getServerUrl() + "/mcp/v1/tools/call";

      Map<String, Object> arguments = new HashMap<>();
      arguments.put("organization", organization);
      arguments.put("project", project);
      arguments.put("repositoryId", repository);
      arguments.put("recursionLevel", "Full");
      if (branch != null && !branch.isBlank()) {
        arguments.put("branch", branch);
      }

      Map<String, Object> params = Map.of("name", "azuredevops_get_items", "arguments", arguments);

      Map<String, Object> request =
          Map.of(
              "jsonrpc", "2.0",
              "method", "tools/call",
              "params", params,
              "id", java.util.UUID.randomUUID().toString());

      HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
      ResponseEntity<Map<String, Object>> response =
          mcpRestTemplate.exchange(
              url,
              HttpMethod.POST,
              entity,
              new ParameterizedTypeReference<Map<String, Object>>() {});

      List<String> files = extractItemPaths(response.getBody());
      log.debug(
          "Retrieved {} files from Azure DevOps MCP for {}/{}/{}",
          files.size(), organization, project, repository);
      return files;

    } catch (RestClientException e) {
      log.error(
          "Failed to list files via Azure DevOps MCP [{}]: {}",
          mcpConfig.getAzureDevOps().getServerUrl(), e.getMessage());
      return List.of();
    }
  }

  /**
   * Read the content of a single file from an Azure Repos Git repository.
   *
   * @param context expected keys {@code "organization"}, {@code "project"}, {@code "repository"},
   *     and optionally {@code "branch"}
   * @param filePath path to the file within the repository
   * @return the file content, or empty if not accessible
   */
  @Override
  public Optional<String> readFile(Map<String, String> context, String filePath) {
    if (!isAvailable()) {
      log.warn("Azure DevOps MCP not available, cannot read file: {}", filePath);
      return Optional.empty();
    }

    String organization = context.get("organization");
    String project = context.get("project");
    String repository = context.get("repository");
    String branch = context.get("branch");

    log.info(
        "Reading file {}/{}/{}:{} via Azure DevOps MCP",
        organization, project, repository, filePath);

    try {
      String url = mcpConfig.getAzureDevOps().getServerUrl() + "/mcp/v1/tools/call";

      Map<String, Object> arguments = new HashMap<>();
      arguments.put("organization", organization);
      arguments.put("project", project);
      arguments.put("repositoryId", repository);
      arguments.put("path", filePath);
      arguments.put("format", "text");
      if (branch != null && !branch.isBlank()) {
        arguments.put("branch", branch);
      }

      Map<String, Object> params =
          Map.of("name", "azuredevops_get_item_content", "arguments", arguments);

      Map<String, Object> request =
          Map.of(
              "jsonrpc", "2.0",
              "method", "tools/call",
              "params", params,
              "id", java.util.UUID.randomUUID().toString());

      HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
      ResponseEntity<Map<String, Object>> response =
          mcpRestTemplate.exchange(
              url,
              HttpMethod.POST,
              entity,
              new ParameterizedTypeReference<Map<String, Object>>() {});

      Optional<String> content = firstContentText(response.getBody());
      if (content.isEmpty()) {
        log.debug("No content returned from Azure DevOps MCP for file: {}", filePath);
      }
      return content;

    } catch (RestClientException e) {
      log.error(
          "Failed to read file {} via Azure DevOps MCP [{}]: {}",
          filePath, mcpConfig.getAzureDevOps().getServerUrl(), e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Search for files matching a text pattern.
   *
   * @param context expected keys {@code "organization"}, {@code "project"}, {@code "repository"}
   * @param pattern the search text
   * @return list of matching file paths. Falls back to a client-side filtered {@link #listFiles}
   *     when the {@code azuredevops_search_code} tool errors or returns no matches (Code Search
   *     is an optional Azure DevOps extension, not guaranteed to be installed).
   */
  @Override
  public List<String> searchFiles(Map<String, String> context, String pattern) {
    if (!isAvailable()) {
      log.warn("Azure DevOps MCP not available, cannot search files with pattern: {}", pattern);
      return List.of();
    }

    String organization = context.get("organization");
    String project = context.get("project");
    String repository = context.get("repository");

    log.info(
        "Searching files in {}/{}/{} with pattern '{}' via Azure DevOps MCP",
        organization, project, repository, pattern);

    try {
      String url = mcpConfig.getAzureDevOps().getServerUrl() + "/mcp/v1/tools/call";

      Map<String, Object> arguments = new HashMap<>();
      arguments.put("organization", organization);
      arguments.put("project", project);
      arguments.put("repositoryId", repository);
      arguments.put("searchText", pattern);

      Map<String, Object> params =
          Map.of("name", "azuredevops_search_code", "arguments", arguments);

      Map<String, Object> request =
          Map.of(
              "jsonrpc", "2.0",
              "method", "tools/call",
              "params", params,
              "id", java.util.UUID.randomUUID().toString());

      HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
      ResponseEntity<Map<String, Object>> response =
          mcpRestTemplate.exchange(
              url,
              HttpMethod.POST,
              entity,
              new ParameterizedTypeReference<Map<String, Object>>() {});

      List<String> matches = extractItemPaths(response.getBody());
      if (!matches.isEmpty()) {
        log.debug(
            "Found {} files matching pattern '{}' via Azure DevOps MCP", matches.size(), pattern);
        return matches;
      }
      log.debug(
          "No matches from azuredevops_search_code for pattern '{}', falling back to filtered"
              + " file listing",
          pattern);
    } catch (RestClientException e) {
      log.warn(
          "azuredevops_search_code unavailable ({}), falling back to filtered file listing",
          e.getMessage());
    }

    return listFiles(context).stream()
        .filter(path -> path.toLowerCase(Locale.ROOT).contains(pattern.toLowerCase(Locale.ROOT)))
        .collect(Collectors.toList());
  }

  /**
   * Get a summary context map for an Azure Repos repository.
   *
   * @param context expected keys {@code "organization"}, {@code "project"}, {@code "repository"}
   * @return structured repository context, or an empty map if not available/accessible
   */
  @Override
  public Map<String, Object> getResourceContext(Map<String, String> context) {
    if (!isAvailable()) {
      log.warn("Azure DevOps MCP not available, cannot get resource context");
      return Map.of();
    }

    String organization = context.get("organization");
    String project = context.get("project");
    String repository = context.get("repository");

    log.debug(
        "Getting repository context for {}/{}/{} via Azure DevOps MCP",
        organization, project, repository);

    try {
      // Note: azuredevops_get_repository_context is not a standard Azure DevOps MCP tool.
      // This may fail if using a generic MCP server - that's expected.
      String url = mcpConfig.getAzureDevOps().getServerUrl() + "/mcp/v1/tools/call";

      Map<String, Object> arguments =
          Map.of(
              "organization", organization,
              "project", project,
              "repositoryId", repository);

      Map<String, Object> params =
          Map.of("name", "azuredevops_get_repository_context", "arguments", arguments);

      Map<String, Object> request =
          Map.of(
              "jsonrpc", "2.0",
              "method", "tools/call",
              "params", params,
              "id", java.util.UUID.randomUUID().toString());

      HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
      ResponseEntity<Map<String, Object>> response =
          mcpRestTemplate.exchange(
              url,
              HttpMethod.POST,
              entity,
              new ParameterizedTypeReference<Map<String, Object>>() {});

      if (response.getBody() != null && response.getBody().containsKey("result")) {
        Object result = response.getBody().get("result");
        if (result instanceof Map) {
          log.debug(
              "Retrieved repository context for {}/{}/{}", organization, project, repository);
          @SuppressWarnings("unchecked")
          Map<String, Object> resultMap = (Map<String, Object>) result;
          return resultMap;
        }
      } else {
        Map<String, Object> body = response.getBody();
        if (body != null && body.containsKey("error")) {
          log.error(
              "MCP returned error for repo context {}/{}/{}: {}",
              organization, project, repository, body.get("error"));
        }
      }

      log.debug(
          "No context returned from Azure DevOps MCP for {}/{}/{}",
          organization, project, repository);
      return Map.of();

    } catch (RestClientException e) {
      log.error(
          "Failed to get repo context via Azure DevOps MCP [{}]: {}",
          mcpConfig.getAzureDevOps().getServerUrl(), e.getMessage());
      return Map.of();
    }
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  /**
   * Create an HTTP entity with JSON content type and Basic-auth Authorization header.
   *
   * <p>Azure DevOps REST API 7.1 authenticates a Personal Access Token via HTTP Basic auth with
   * an empty username and the PAT as the password — different from GitHub/Notion/Confluence's
   * Bearer token.
   */
  private <T> HttpEntity<T> createJsonEntity(T body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Accept", "application/json, text/event-stream");

    String token = settingsService.getAzureDevOpsAccessToken();
    if (token != null && !token.isBlank()) {
      headers.setBasicAuth("", token);
    } else {
      log.warn(
          "Azure DevOps access token (PAT) not configured — MCP requests will be unauthenticated"
              + " and will 401/403 on private repositories");
    }

    return new HttpEntity<>(body, headers);
  }

  /**
   * Parse an {@code azuredevops_get_items} / {@code azuredevops_search_code} tool response into a
   * flat list of file paths. Handles the Azure DevOps Items REST API's {@code {"value": [...]}}
   * envelope shape, a bare JSON array of item objects, and a bare JSON array of path strings;
   * falls back to newline-delimited raw text if the response isn't valid JSON.
   */
  @SuppressWarnings("unchecked")
  private List<String> extractItemPaths(Map<String, Object> body) {
    Optional<String> textOpt = firstContentText(body);
    if (textOpt.isEmpty()) {
      return List.of();
    }
    String text = textOpt.get();

    List<String> files = new ArrayList<>();
    try {
      Object parsed = JSON.readValue(text, Object.class);
      List<Object> items;
      if (parsed instanceof Map<?, ?> parsedMap && parsedMap.get("value") instanceof List<?> valueList) {
        items = (List<Object>) valueList;
      } else if (parsed instanceof List<?> list) {
        items = (List<Object>) list;
      } else {
        items = List.of();
      }

      for (Object item : items) {
        if (item instanceof Map<?, ?> entry) {
          boolean isFolder =
              Boolean.TRUE.equals(entry.get("isFolder")) || "tree".equals(entry.get("gitObjectType"));
          Object path = entry.get("path");
          if (!isFolder && path != null) {
            files.add(path.toString());
          }
        } else if (item instanceof String str && !str.isBlank()) {
          files.add(str);
        }
      }
    } catch (Exception e) {
      log.debug(
          "Azure DevOps MCP response was not the expected JSON item list, falling back to"
              + " line-delimited parsing: {}",
          e.getMessage());
      for (String line : text.split("\\n")) {
        String trimmed = line.trim();
        if (!trimmed.isEmpty()) {
          files.add(trimmed);
        }
      }
    }
    return files;
  }

  /** Extract the first {@code content[0].text} value from an MCP {@code tools/call} response. */
  @SuppressWarnings("unchecked")
  private Optional<String> firstContentText(Map<String, Object> body) {
    if (body == null || !body.containsKey("result")) {
      return Optional.empty();
    }
    Object result = body.get("result");
    if (!(result instanceof Map)) {
      return Optional.empty();
    }
    Map<String, Object> resultMap = (Map<String, Object>) result;
    Object content = resultMap.get("content");
    if (!(content instanceof List)) {
      return Optional.empty();
    }
    List<Map<String, Object>> contentList = (List<Map<String, Object>>) content;
    if (contentList.isEmpty() || contentList.get(0) == null) {
      return Optional.empty();
    }
    Object text = contentList.get(0).get("text");
    return text == null ? Optional.empty() : Optional.of(text.toString());
  }
}
