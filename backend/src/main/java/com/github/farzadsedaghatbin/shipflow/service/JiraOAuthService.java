package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.JiraConnectionStatusDTO;
import com.github.farzadsedaghatbin.shipflow.entity.OrganizationSettings;
import com.github.farzadsedaghatbin.shipflow.repository.OrganizationSettingsRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/**
 * Manages the Jira (Atlassian) OAuth 2.0 (3LO) authorization-code flow. Follows the same pattern
 * as {@link LinearOAuthService}: in-memory CSRF state map with 10-minute TTL.
 *
 * <p>Authorization URL: {@code https://auth.atlassian.com/authorize}
 *
 * <p>Token URL: {@code https://auth.atlassian.com/oauth/token} (JSON body — not form-encoded)
 *
 * <p>Accessible resources: {@code GET https://api.atlassian.com/oauth/token/accessible-resources}
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class JiraOAuthService {

  private static final String JIRA_AUTH_URL = "https://auth.atlassian.com/authorize";
  private static final String JIRA_TOKEN_URL = "https://auth.atlassian.com/oauth/token";
  private static final String JIRA_RESOURCES_URL =
      "https://api.atlassian.com/oauth/token/accessible-resources";

  private final OrganizationSettingsRepository settingsRepository;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.jira.oauth.client-id:}")
  private String clientId;

  @Value("${app.jira.oauth.client-secret:}")
  private String clientSecret;

  /** CSRF state store — same pattern as LinearOAuthService (10-minute TTL). */
  private final Map<String, OAuthState> pendingStates = new ConcurrentHashMap<>();

  private record OAuthState(String redirectUri, LocalDateTime expiresAt) {}

  /** Returns true when both client-id and client-secret are configured in environment. */
  public boolean isConfigured() {
    return clientId != null
        && !clientId.isBlank()
        && clientSecret != null
        && !clientSecret.isBlank();
  }

  /**
   * Build the Atlassian OAuth2 authorization URL and store the CSRF state.
   *
   * @param callbackBaseUrl scheme+host+port of the ShipFlow backend (auto-detected from request)
   * @return fully-formed Atlassian OAuth authorization URL
   */
  public String generateAuthorizationUrl(String callbackBaseUrl) {
    String state = UUID.randomUUID().toString();
    String redirectUri = callbackBaseUrl + "/api/import/jira/callback";
    pendingStates.put(state, new OAuthState(redirectUri, LocalDateTime.now().plusMinutes(10)));

    return JIRA_AUTH_URL
        + "?audience=" + encode("api.atlassian.com")
        + "&client_id=" + encode(clientId)
        + "&scope=" + encode("read:jira-data read:jira-user offline_access")
        + "&redirect_uri=" + encode(redirectUri)
        + "&state=" + state
        + "&response_type=code"
        + "&prompt=consent";
  }

  /**
   * Exchange the Atlassian authorization code for an access token, fetch the cloud id, and
   * persist everything.
   *
   * @return frontend redirect URL with {@code ?jira_connected=true} or {@code ?jira_error=...}
   */
  public String processCallback(String code, String state) {
    OAuthState oauthState = pendingStates.remove(state);
    if (oauthState == null || oauthState.expiresAt().isBefore(LocalDateTime.now())) {
      log.warn("Jira OAuth callback: invalid or expired state={}", state);
      return "/import?jira_error=invalid_state";
    }
    try {
      TokenResponse tokenResponse = exchangeCodeForToken(code, oauthState.redirectUri());
      CloudResource cloud = fetchPrimaryCloudResource(tokenResponse.accessToken());
      saveTokens(tokenResponse, cloud);
      log.info(
          "Jira OAuth: tokens stored successfully cloudId={} cloudName={}",
          cloud.id(),
          cloud.name());
      return "/import?tab=jira&jira_connected=true";
    } catch (Exception e) {
      log.error("Jira OAuth: token exchange failed", e);
      return "/import?tab=jira&jira_error=token_exchange_failed";
    }
  }

  /** Return current connection status for the organization. */
  @Transactional(readOnly = true)
  public JiraConnectionStatusDTO getConnectionStatus() {
    OrganizationSettings settings = getOrCreateSettings();
    boolean connected =
        settings.getJiraAccessToken() != null && !settings.getJiraAccessToken().isBlank();
    return JiraConnectionStatusDTO.builder()
        .configured(isConfigured())
        .connected(connected)
        .cloudId(settings.getJiraCloudId())
        .cloudName(settings.getJiraCloudName())
        .build();
  }

  /** Remove the stored Jira access/refresh tokens and cloud details. */
  public void disconnect() {
    OrganizationSettings settings = getOrCreateSettings();
    settings.setJiraAccessToken(null);
    settings.setJiraRefreshToken(null);
    settings.setJiraCloudId(null);
    settings.setJiraCloudName(null);
    settings.setUpdatedAt(LocalDateTime.now());
    settingsRepository.save(settings);
    log.info("Jira OAuth: disconnected");
  }

  /** Return the stored Jira access token, or null if not connected. */
  @Transactional(readOnly = true)
  public String getStoredAccessToken() {
    return getOrCreateSettings().getJiraAccessToken();
  }

  /** Return the stored Jira cloud id, or null if not connected. */
  @Transactional(readOnly = true)
  public String getStoredCloudId() {
    return getOrCreateSettings().getJiraCloudId();
  }

  // ── private ──────────────────────────────────────────────────────────────

  /**
   * Jira's token endpoint expects a JSON body (not form-encoded). The response contains
   * access_token, refresh_token, and expires_in.
   */
  private TokenResponse exchangeCodeForToken(String code, String redirectUri) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Accept", "application/json");

    Map<String, String> body =
        Map.of(
            "grant_type", "authorization_code",
            "client_id", clientId,
            "client_secret", clientSecret,
            "code", code,
            "redirect_uri", redirectUri);

    ResponseEntity<String> resp =
        restTemplate.postForEntity(JIRA_TOKEN_URL, new HttpEntity<>(body, headers), String.class);

    JsonNode json = objectMapper.readTree(resp.getBody());
    JsonNode tokenNode = json.get("access_token");
    if (tokenNode == null || tokenNode.isNull()) {
      throw new IllegalStateException("No access_token in Jira response: " + resp.getBody());
    }
    String accessToken = tokenNode.asText();
    String refreshToken =
        json.has("refresh_token") && !json.get("refresh_token").isNull()
            ? json.get("refresh_token").asText()
            : null;
    return new TokenResponse(accessToken, refreshToken);
  }

  /**
   * Calls the accessible-resources endpoint with the new token to obtain the primary cloud id and
   * name. Uses the first resource in the list.
   */
  private CloudResource fetchPrimaryCloudResource(String accessToken) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.set("Accept", "application/json");

    ResponseEntity<String> resp =
        restTemplate.exchange(
            JIRA_RESOURCES_URL, HttpMethod.GET, new HttpEntity<>(headers), String.class);

    JsonNode resources = objectMapper.readTree(resp.getBody());
    if (!resources.isArray() || resources.isEmpty()) {
      throw new IllegalStateException(
          "No accessible Atlassian resources returned: " + resp.getBody());
    }
    JsonNode first = resources.get(0);
    return new CloudResource(first.path("id").asText(), first.path("name").asText());
  }

  private void saveTokens(TokenResponse tokens, CloudResource cloud) {
    OrganizationSettings settings = getOrCreateSettings();
    settings.setJiraAccessToken(tokens.accessToken());
    settings.setJiraRefreshToken(tokens.refreshToken());
    settings.setJiraCloudId(cloud.id());
    settings.setJiraCloudName(cloud.name());
    settings.setUpdatedAt(LocalDateTime.now());
    settingsRepository.save(settings);
  }

  private OrganizationSettings getOrCreateSettings() {
    return settingsRepository
        .findFirstByOrderByIdAsc()
        .orElseGet(
            () -> {
              OrganizationSettings s =
                  OrganizationSettings.builder()
                      .organizationName("ShipFlow")
                      .updatedBy("system")
                      .build();
              return settingsRepository.save(s);
            });
  }

  private static String encode(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  private record TokenResponse(String accessToken, String refreshToken) {}

  private record CloudResource(String id, String name) {}
}
