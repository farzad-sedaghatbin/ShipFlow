package com.github.farzadsedaghatbin.shipflow.service.mcp;

import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import java.util.*;
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
 * Notion MCP client implementation. Connects to a Notion MCP server to read pages and
 * extract document context.
 *
 * <p>MCP Server expected tools:
 *
 * <ul>
 *   <li>{@code notion-search}: Search pages by query text
 *   <li>{@code notion-get-page}: Retrieve a page's full content by page ID
 * </ul>
 */
@Service
@Slf4j
public class NotionMcpProvider implements McpClientService {

  private final McpConfig mcpConfig;
  private final RestTemplate mcpRestTemplate;
  private final OrganizationSettingsService settingsService;

  public NotionMcpProvider(
      McpConfig mcpConfig,
      @Qualifier("mcpRestTemplate") RestTemplate mcpRestTemplate,
      OrganizationSettingsService settingsService) {
    this.mcpConfig = mcpConfig;
    this.mcpRestTemplate = mcpRestTemplate;
    this.settingsService = settingsService;
  }

  @Override
  public boolean isAvailable() {
    McpConfig.NotionMcpConfig config = mcpConfig.getNotion();
    boolean available =
        config.isEnabled() && config.getServerUrl() != null && !config.getServerUrl().isBlank();
    if (!available) {
      log.debug(
          "Notion MCP is not available - enabled: {}, serverUrl: {}",
          config.isEnabled(),
          config.getServerUrl());
    }
    return available;
  }

  @Override
  public String getProviderType() {
    return "notion";
  }

  /**
   * List Notion pages matching an optional query.
   *
   * @param context optional key {@code "query"} for filtering; defaults to empty string
   * @return list of strings in format {@code "<pageId>: <title>"}
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<String> listFiles(Map<String, String> context) {
    if (!isAvailable()) {
      log.warn("Notion MCP not available, cannot list pages");
      return List.of();
    }

    String query = context.getOrDefault("query", "");
    log.info("Listing Notion pages via MCP with query: '{}'", query);

    try {
      String url = mcpConfig.getNotion().getServerUrl() + "/mcp/v1/tools/call";

      Map<String, Object> arguments = new HashMap<>();
      arguments.put("query", query);

      Map<String, Object> params = Map.of("name", "notion-search", "arguments", arguments);

      Map<String, Object> request =
          Map.of(
              "jsonrpc", "2.0",
              "method", "tools/call",
              "params", params,
              "id", System.currentTimeMillis());

      HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
      ResponseEntity<Map<String, Object>> response =
          mcpRestTemplate.exchange(
              url,
              HttpMethod.POST,
              entity,
              new ParameterizedTypeReference<Map<String, Object>>() {});

      return extractTextLines(response.getBody(), "pages");

    } catch (RestClientException e) {
      log.error("Failed to list pages via Notion MCP: {}", e.getMessage());
      return List.of();
    }
  }

  /**
   * Read the content of a Notion page by its ID.
   *
   * @param context provider-specific context (unused for this call)
   * @param filePath the Notion page ID
   * @return the page content, or empty if not accessible
   */
  @Override
  @SuppressWarnings("unchecked")
  public Optional<String> readFile(Map<String, String> context, String filePath) {
    if (!isAvailable()) {
      log.warn("Notion MCP not available, cannot read page: {}", filePath);
      return Optional.empty();
    }

    log.info("Reading Notion page {} via MCP", filePath);

    try {
      String url = mcpConfig.getNotion().getServerUrl() + "/mcp/v1/tools/call";

      Map<String, Object> arguments = new HashMap<>();
      arguments.put("pageId", filePath);

      Map<String, Object> params = Map.of("name", "notion-get-page", "arguments", arguments);

      Map<String, Object> request =
          Map.of(
              "jsonrpc", "2.0",
              "method", "tools/call",
              "params", params,
              "id", System.currentTimeMillis());

      HttpEntity<Map<String, Object>> entity = createJsonEntity(request);
      ResponseEntity<Map<String, Object>> response =
          mcpRestTemplate.exchange(
              url,
              HttpMethod.POST,
              entity,
              new ParameterizedTypeReference<Map<String, Object>>() {});

      return extractFirstText(response.getBody());

    } catch (RestClientException e) {
      log.error("Failed to read page {} via Notion MCP: {}", filePath, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Search for Notion pages matching a text pattern.
   *
   * @param context provider-specific context
   * @param pattern the search query
   * @return list of matching page identifiers/titles
   */
  @Override
  public List<String> searchFiles(Map<String, String> context, String pattern) {
    if (!isAvailable()) {
      log.warn("Notion MCP not available, cannot search with pattern: {}", pattern);
      return List.of();
    }

    log.info("Searching Notion pages with pattern '{}' via MCP", pattern);

    Map<String, String> searchContext = new HashMap<>(context);
    searchContext.put("query", pattern);
    return listFiles(searchContext);
  }

  /**
   * Get a summary context map for the Notion workspace.
   *
   * @param context optional {@code "query"} to scope the listing
   * @return map with keys {@code "provider"} and {@code "pages"}
   */
  @Override
  public Map<String, Object> getResourceContext(Map<String, String> context) {
    if (!isAvailable()) {
      log.warn("Notion MCP not available, cannot get resource context");
      return Map.of("error", "Notion MCP not configured");
    }

    List<String> pages = listFiles(context);
    return Map.of("provider", "notion", "pages", pages);
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  private <T> HttpEntity<T> createJsonEntity(T body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Accept", "application/json, text/event-stream");

    String token = settingsService.getNotionAccessToken();
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    } else {
      log.warn("Notion access token not configured, MCP requests may fail");
    }

    return new HttpEntity<>(body, headers);
  }

  @SuppressWarnings("unchecked")
  private List<String> extractTextLines(Map<String, Object> body, String logLabel) {
    if (body == null || !body.containsKey("result")) {
      log.debug("No result returned from Notion MCP for {}", logLabel);
      return List.of();
    }
    Object result = body.get("result");
    if (!(result instanceof Map)) {
      return List.of();
    }
    Map<String, Object> resultMap = (Map<String, Object>) result;
    Object content = resultMap.get("content");
    if (!(content instanceof List)) {
      return List.of();
    }
    List<Map<String, Object>> contentList = (List<Map<String, Object>>) content;
    List<String> lines = new ArrayList<>();
    for (Map<String, Object> item : contentList) {
      if (item == null) {
        continue;
      }
      Object textObj = item.get("text");
      if (textObj != null) {
        String text = textObj.toString().trim();
        if (!text.isEmpty()) {
          lines.add(text);
        }
      }
    }
    log.debug("Retrieved {} {} from Notion MCP", lines.size(), logLabel);
    return lines;
  }

  @SuppressWarnings("unchecked")
  private Optional<String> extractFirstText(Map<String, Object> body) {
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
    if (contentList.isEmpty()) {
      return Optional.empty();
    }
    Map<String, Object> first = contentList.get(0);
    if (first == null || !first.containsKey("text")) {
      return Optional.empty();
    }
    return Optional.of(first.get("text").toString());
  }
}
