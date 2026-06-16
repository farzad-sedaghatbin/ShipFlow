package com.github.farzadsedaghatbin.shipflow.service.mcp;

import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import java.util.ArrayList;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * Microsoft SharePoint client using the Microsoft Graph API directly (no MCP proxy needed).
 *
 * <p>Auth: Azure AD client-credentials flow. An access token is fetched once via
 * {@code https://login.microsoftonline.com/{tenantId}/oauth2/v2.0/token} and cached until
 * 60 seconds before expiry.
 *
 * <p>Required org settings: sharepointTenantId, sharepointClientId,
 * sharepointClientSecret, sharepointSiteUrl.
 *
 * <p>The site URL accepts two formats:
 * <ul>
 *   <li>A site ID GUID: {@code "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"}</li>
 *   <li>A hostname:path pair: {@code "contoso.sharepoint.com:/sites/Engineering"}</li>
 * </ul>
 */
@Service
@Slf4j
public class SharePointGraphProvider implements McpClientService {

  private static final String GRAPH_BASE = "https://graph.microsoft.com/v1.0";
  private static final String TOKEN_URL_TEMPLATE =
      "https://login.microsoftonline.com/{tenantId}/oauth2/v2.0/token";
  private static final String SITE_ID_PATTERN =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}.*";

  private final RestTemplate restTemplate;
  private final OrganizationSettingsService settingsService;

  // Simple in-memory token cache
  private volatile String cachedToken;
  private volatile long tokenExpiresAtMs = 0;

  public SharePointGraphProvider(
      @Qualifier("mcpRestTemplate") RestTemplate mcpRestTemplate,
      OrganizationSettingsService settingsService) {
    this.restTemplate = mcpRestTemplate;
    this.settingsService = settingsService;
  }

  @Override
  public boolean isAvailable() {
    boolean available =
        settingsService.getSharepointTenantId() != null
            && settingsService.getSharepointClientId() != null
            && settingsService.getSharepointClientSecret() != null
            && settingsService.getSharepointSiteUrl() != null;
    if (!available) {
      log.debug("SharePoint Graph provider not available — credentials not fully configured");
    }
    return available;
  }

  @Override
  public String getProviderType() {
    return "sharepoint";
  }

  /**
   * List the top-level children of the SharePoint site's default document library.
   *
   * @param context unused
   * @return list of file/folder names
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<String> listFiles(Map<String, String> context) {
    if (!isAvailable()) {
      log.warn("SharePoint Graph provider not available, cannot list files");
      return List.of();
    }
    try {
      String siteId = resolveSiteId();
      String token = getAccessToken();
      String url = GRAPH_BASE + "/sites/" + siteId + "/drive/root/children";
      ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
          url, HttpMethod.GET, jsonEntity(null, token),
          new ParameterizedTypeReference<Map<String, Object>>() {});
      return extractNames(response.getBody());
    } catch (RestClientException e) {
      log.error("SharePoint listFiles failed: {}", e.getMessage());
      return List.of();
    }
  }

  /**
   * Download the content of a SharePoint drive item by its item ID.
   *
   * @param context unused
   * @param filePath the drive item ID (from a previous listFiles / searchFiles call)
   * @return raw file content as a string, or empty if not accessible
   */
  @Override
  public Optional<String> readFile(Map<String, String> context, String filePath) {
    if (!isAvailable()) {
      log.warn("SharePoint Graph provider not available, cannot read item: {}", filePath);
      return Optional.empty();
    }
    try {
      String siteId = resolveSiteId();
      String token = getAccessToken();
      String url = GRAPH_BASE + "/sites/" + siteId + "/drive/items/" + filePath + "/content";
      ResponseEntity<String> response = restTemplate.exchange(
          url, HttpMethod.GET, jsonEntity(null, token), String.class);
      return Optional.ofNullable(response.getBody());
    } catch (RestClientException e) {
      log.error("SharePoint readFile failed for item {}: {}", filePath, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Search for drive items matching a query string.
   *
   * @param context unused
   * @param pattern the search query
   * @return list of matching item names
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<String> searchFiles(Map<String, String> context, String pattern) {
    if (!isAvailable()) {
      log.warn("SharePoint Graph provider not available, cannot search: {}", pattern);
      return List.of();
    }
    try {
      String siteId = resolveSiteId();
      String token = getAccessToken();
      String encodedPattern = UriUtils.encode(pattern, StandardCharsets.UTF_8);
      String url = GRAPH_BASE + "/sites/" + siteId
          + "/drive/root/search(q='" + encodedPattern + "')";
      ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
          url, HttpMethod.GET, jsonEntity(null, token),
          new ParameterizedTypeReference<Map<String, Object>>() {});
      return extractNames(response.getBody());
    } catch (RestClientException e) {
      log.error("SharePoint searchFiles failed for pattern '{}': {}", pattern, e.getMessage());
      return List.of();
    }
  }

  /**
   * Returns a summary context map for this SharePoint site.
   *
   * @param context unused
   * @return map with keys {@code "provider"} and {@code "files"}
   */
  @Override
  public Map<String, Object> getResourceContext(Map<String, String> context) {
    if (!isAvailable()) {
      return Map.of("error", "SharePoint Graph API not configured");
    }
    List<String> files = listFiles(context);
    return Map.of("provider", "sharepoint", "files", files);
  }

  // ──────────────────────────────────────────────
  // OAuth2 client-credentials token (cached)
  // ──────────────────────────────────────────────

  private synchronized String getAccessToken() {
    if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAtMs) {
      return cachedToken;
    }

    String tenantId = settingsService.getSharepointTenantId();
    String clientId = settingsService.getSharepointClientId();
    String clientSecret = settingsService.getSharepointClientSecret();

    String tokenUrl = TOKEN_URL_TEMPLATE.replace("{tenantId}", tenantId);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "client_credentials");
    body.add("client_id", clientId);
    body.add("client_secret", clientSecret);
    body.add("scope", "https://graph.microsoft.com/.default");

    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
        tokenUrl, HttpMethod.POST, new HttpEntity<>(body, headers),
        new ParameterizedTypeReference<Map<String, Object>>() {});

    Map<String, Object> tokenData = response.getBody();
    if (tokenData == null || !tokenData.containsKey("access_token")) {
      throw new IllegalStateException("SharePoint OAuth2 token response missing access_token");
    }

    cachedToken = (String) tokenData.get("access_token");
    int expiresIn = tokenData.containsKey("expires_in")
        ? ((Number) tokenData.get("expires_in")).intValue()
        : 3600;
    tokenExpiresAtMs = System.currentTimeMillis() + (expiresIn - 60) * 1000L;
    log.debug("SharePoint access token refreshed, expires in {}s", expiresIn);
    return cachedToken;
  }

  // ──────────────────────────────────────────────
  // Site ID resolution
  // ──────────────────────────────────────────────

  /**
   * Resolve the site URL from settings into a Graph-compatible site ID.
   * If the value already looks like a GUID (optionally with a suffix), use it directly.
   * Otherwise call {@code GET /sites/{hostname}:/{path}} to resolve.
   */
  @SuppressWarnings("unchecked")
  private String resolveSiteId() {
    String siteUrl = settingsService.getSharepointSiteUrl();
    if (siteUrl.matches(SITE_ID_PATTERN)) {
      return siteUrl;
    }
    // "contoso.sharepoint.com:/sites/Engineering" → Graph resolves to site GUID
    String token = getAccessToken();
    String url = GRAPH_BASE + "/sites/" + siteUrl;
    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
        url, HttpMethod.GET, jsonEntity(null, token),
        new ParameterizedTypeReference<Map<String, Object>>() {});
    Map<String, Object> site = response.getBody();
    if (site == null || !site.containsKey("id")) {
      throw new IllegalStateException("Could not resolve SharePoint site ID from URL: " + siteUrl);
    }
    return (String) site.get("id");
  }

  // ──────────────────────────────────────────────
  // Helpers
  // ──────────────────────────────────────────────

  private <T> HttpEntity<T> jsonEntity(T body, String bearerToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Accept", "application/json");
    headers.setBearerAuth(bearerToken);
    return new HttpEntity<>(body, headers);
  }

  @SuppressWarnings("unchecked")
  private List<String> extractNames(Map<String, Object> body) {
    if (body == null || !body.containsKey("value")) {
      return List.of();
    }
    List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("value");
    List<String> names = new ArrayList<>();
    for (Map<String, Object> item : items) {
      Object name = item.get("name");
      if (name != null && !name.toString().isBlank()) {
        names.add(name.toString());
      }
    }
    return names;
  }
}
