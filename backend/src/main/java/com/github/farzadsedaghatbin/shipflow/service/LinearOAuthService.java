package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.LinearConnectionStatusDTO;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Manages the Linear OAuth2 authorization-code flow. Follows the same pattern as
 * GitHubAppOAuthService: in-memory CSRF state map with 10-minute TTL.
 *
 * <p>Authorization URL: {@code https://linear.app/oauth/authorize}
 * <p>Token URL: {@code https://api.linear.app/oauth/token}
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LinearOAuthService {

  private static final String LINEAR_AUTH_URL = "https://linear.app/oauth/authorize";
  private static final String LINEAR_TOKEN_URL = "https://api.linear.app/oauth/token";

  private final OrganizationSettingsRepository settingsRepository;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.linear.oauth.client-id:}")
  private String clientId;

  @Value("${app.linear.oauth.client-secret:}")
  private String clientSecret;

  /** CSRF state store — same pattern as GitHubAppOAuthService (10-minute TTL). */
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
   * Build the Linear OAuth2 authorization URL and store the CSRF state.
   *
   * @param callbackBaseUrl scheme+host+port of the ShipFlow backend (auto-detected from request)
   * @return fully-formed Linear OAuth authorization URL
   */
  public String generateAuthorizationUrl(String callbackBaseUrl) {
    String state = UUID.randomUUID().toString();
    String redirectUri = callbackBaseUrl + "/api/import/linear/callback";
    pendingStates.put(state, new OAuthState(redirectUri, LocalDateTime.now().plusMinutes(10)));

    return LINEAR_AUTH_URL
        + "?response_type=code"
        + "&client_id=" + encode(clientId)
        + "&redirect_uri=" + encode(redirectUri)
        + "&scope=read"
        + "&state=" + state;
  }

  /**
   * Exchange the Linear authorization code for an access token and persist it.
   *
   * @return frontend redirect URL with {@code ?linear_connected=true} or
   *         {@code ?linear_error=...}
   */
  public String processCallback(String code, String state) {
    OAuthState oauthState = pendingStates.remove(state);
    if (oauthState == null || oauthState.expiresAt().isBefore(LocalDateTime.now())) {
      log.warn("Linear OAuth callback: invalid or expired state={}", state);
      return "/import?linear_error=invalid_state";
    }
    try {
      String accessToken = exchangeCodeForToken(code, oauthState.redirectUri());
      saveToken(accessToken);
      log.info("Linear OAuth: access token stored successfully");
      return "/import?tab=linear&linear_connected=true";
    } catch (Exception e) {
      log.error("Linear OAuth: token exchange failed", e);
      return "/import?tab=linear&linear_error=token_exchange_failed";
    }
  }

  /** Return current connection status for the organization. */
  @Transactional(readOnly = true)
  public LinearConnectionStatusDTO getConnectionStatus() {
    OrganizationSettings settings = getOrCreateSettings();
    boolean connected =
        settings.getLinearAccessToken() != null && !settings.getLinearAccessToken().isBlank();
    return LinearConnectionStatusDTO.builder()
        .configured(isConfigured())
        .connected(connected)
        .teamId(settings.getLinearTeamId())
        .teamName(settings.getLinearTeamName())
        .build();
  }

  /** Remove the stored Linear access token and selected team. */
  public void disconnect() {
    OrganizationSettings settings = getOrCreateSettings();
    settings.setLinearAccessToken(null);
    settings.setLinearTeamId(null);
    settings.setLinearTeamName(null);
    settings.setUpdatedAt(LocalDateTime.now());
    settingsRepository.save(settings);
    log.info("Linear OAuth: disconnected");
  }

  /** Return the stored Linear access token, or null if not connected. */
  @Transactional(readOnly = true)
  public String getStoredAccessToken() {
    return getOrCreateSettings().getLinearAccessToken();
  }

  /** Persist the selected Linear team so it survives restarts. */
  public void saveSelectedTeam(String teamId, String teamName) {
    OrganizationSettings settings = getOrCreateSettings();
    settings.setLinearTeamId(teamId);
    settings.setLinearTeamName(teamName);
    settings.setUpdatedAt(LocalDateTime.now());
    settingsRepository.save(settings);
    log.info("Linear OAuth: team selected id={} name={}", teamId, teamName);
  }

  // ── private ──────────────────────────────────────────────────────────────

  private String exchangeCodeForToken(String code, String redirectUri) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.set("Accept", "application/json");

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("code", code);
    body.add("redirect_uri", redirectUri);
    body.add("client_id", clientId);
    body.add("client_secret", clientSecret);
    body.add("grant_type", "authorization_code");

    ResponseEntity<String> resp =
        restTemplate.postForEntity(
            LINEAR_TOKEN_URL, new HttpEntity<>(body, headers), String.class);

    JsonNode json = objectMapper.readTree(resp.getBody());
    JsonNode tokenNode = json.get("access_token");
    if (tokenNode == null || tokenNode.isNull()) {
      throw new IllegalStateException("No access_token in Linear response: " + resp.getBody());
    }
    return tokenNode.asText();
  }

  private void saveToken(String accessToken) {
    OrganizationSettings settings = getOrCreateSettings();
    settings.setLinearAccessToken(accessToken);
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
}
