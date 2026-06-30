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
 * Confluence MCP client implementation. Connects to a Confluence MCP server to read wiki pages and
 * meeting notes into AI context.
 *
 * <p>MCP Server expected tools:
 *
 * <ul>
 *   <li>{@code confluence-get-space-pages}: List all pages in a space by space key
 *   <li>{@code confluence-get-page}: Retrieve a page's full content by page ID
 *   <li>{@code confluence-search}: Full-text search using CQL syntax
 * </ul>
 */
@Service
@Slf4j
public class ConfluenceMcpProvider implements McpClientService {

  private static final String DEFAULT_CQL_ALL = "type = \"page\"";

  private final McpConfig mcpConfig;
  private final RestTemplate mcpRestTemplate;
  private final OrganizationSettingsService settingsService;

  public ConfluenceMcpProvider(
      McpConfig mcpConfig,
      @Qualifier("mcpRestTemplate") RestTemplate mcpRestTemplate,
      OrganizationSettingsService settingsService) {
    this.mcpConfig = mcpConfig;
    this.mcpRestTemplate = mcpRestTemplate;
    this.settingsService = settingsService;
  }

  @Override
  public boolean isAvailable() {
    McpConfig.ConfluenceMcpConfig config = mcpConfig.getConfluence();
    boolean available =
        config.isEnabled() && config.getServerUrl() != null && !config.getServerUrl().isBlank();
    if (!available) {
      log.debug(
          "Confluence MCP is not available - enabled: {}, serverUrl: {}",
          config.isEnabled(),
          config.getServerUrl());
    }
    return available;
  }

  @Override
  public String getProviderType() {
    return "confluence";
  }

  /**
   * List pages in a Confluence space.
   *
   * @param context optional key {@code "spaceKey"} to specify the space; falls back to the
   *     organization default space key, then to an all-pages CQL query
   * @return list of page identifiers/titles returned by the MCP server
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<String> listFiles(Map<String, String> context) {
    if (!isAvailable()) {
      log.warn("Confluence MCP not available, cannot list pages");
      return List.of();
    }

    String spaceKey = context.get("spaceKey");
    if (spaceKey == null || spaceKey.isBlank()) {
      spaceKey = settingsService.getConfluenceSpaceKey();
    }

    log.info("Listing Confluence pages via MCP for space: {}", spaceKey);

    try {
      String url = mcpConfig.getConfluence().getServerUrl() + "/mcp/v1/tools/call";

      Map<String, Object> arguments = new HashMap<>();
      String toolName;
      if (spaceKey != null && !spaceKey.isBlank()) {
        arguments.put("spaceKey", spaceKey);
        toolName = "confluence-get-space-pages";
      } else {
        // Fall back to a broad CQL search when no space key is known
        arguments.put("cql", DEFAULT_CQL_ALL);
        arguments.put("limit", 20);
        toolName = "confluence-search";
      }

      Map<String, Object> params = Map.of("name", toolName, "arguments", arguments);

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
      log.error("Failed to list pages via Confluence MCP: {}", e.getMessage());
      return List.of();
    }
  }

  /**
   * Read the content of a Confluence page by its ID.
   *
   * @param context provider-specific context (unused for this call)
   * @param filePath the Confluence page ID
   * @return the page content, or empty if not accessible
   */
  @Override
  @SuppressWarnings("unchecked")
  public Optional<String> readFile(Map<String, String> context, String filePath) {
    if (!isAvailable()) {
      log.warn("Confluence MCP not available, cannot read page: {}", filePath);
      return Optional.empty();
    }

    log.info("Reading Confluence page {} via MCP", filePath);

    try {
      String url = mcpConfig.getConfluence().getServerUrl() + "/mcp/v1/tools/call";

      Map<String, Object> arguments = new HashMap<>();
      arguments.put("pageId", filePath);

      Map<String, Object> params =
          Map.of("name", "confluence-get-page", "arguments", arguments);

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
      log.error("Failed to read page {} via Confluence MCP: {}", filePath, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Search Confluence pages using a CQL text match.
   *
   * @param context provider-specific context
   * @param pattern the text pattern to search for
   * @return list of matching page identifiers/titles
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<String> searchFiles(Map<String, String> context, String pattern) {
    if (!isAvailable()) {
      log.warn("Confluence MCP not available, cannot search with pattern: {}", pattern);
      return List.of();
    }

    log.info("Searching Confluence pages with pattern '{}' via MCP", pattern);

    try {
      String url = mcpConfig.getConfluence().getServerUrl() + "/mcp/v1/tools/call";

      String cql = "text ~ \"" + pattern.replace("\"", "\\\"") + "\"";
      Map<String, Object> arguments = new HashMap<>();
      arguments.put("cql", cql);
      arguments.put("limit", 10);

      Map<String, Object> params =
          Map.of("name", "confluence-search", "arguments", arguments);

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

      return extractTextLines(response.getBody(), "search results");

    } catch (RestClientException e) {
      log.error("Failed to search pages via Confluence MCP: {}", e.getMessage());
      return List.of();
    }
  }

  /**
   * Get a summary context map for the Confluence space.
   *
   * @param context optional {@code "spaceKey"} to scope the listing
   * @return map with keys {@code "provider"} and {@code "pages"}
   */
  @Override
  public Map<String, Object> getResourceContext(Map<String, String> context) {
    if (!isAvailable()) {
      log.warn("Confluence MCP not available, cannot get resource context");
      return Map.of("error", "Confluence MCP not configured");
    }

    List<String> pages = listFiles(context);
    return Map.of("provider", "confluence", "pages", pages);
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  private <T> HttpEntity<T> createJsonEntity(T body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Accept", "application/json, text/event-stream");

    String token = settingsService.getConfluenceAccessToken();
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    } else {
      log.warn("Confluence access token not configured, MCP requests may fail");
    }

    return new HttpEntity<>(body, headers);
  }

  @SuppressWarnings("unchecked")
  private List<String> extractTextLines(Map<String, Object> body, String logLabel) {
    if (body == null || !body.containsKey("result")) {
      log.debug("No result returned from Confluence MCP for {}", logLabel);
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
    log.debug("Retrieved {} {} from Confluence MCP", lines.size(), logLabel);
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
