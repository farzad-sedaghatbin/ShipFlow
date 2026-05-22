package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.JiraConnectionStatusDTO;
import com.github.farzadsedaghatbin.shipflow.entity.OrganizationSettings;
import com.github.farzadsedaghatbin.shipflow.repository.OrganizationSettingsRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Pure unit tests for JiraOAuthService — no Spring context.
 */
@ExtendWith(MockitoExtension.class)
class JiraOAuthServiceTest {

  @Mock private OrganizationSettingsRepository settingsRepository;
  @Mock private RestTemplate restTemplate;

  private JiraOAuthService service;

  @BeforeEach
  void setUp() {
    service = new JiraOAuthService(settingsRepository, restTemplate, new ObjectMapper());
    ReflectionTestUtils.setField(service, "clientId", "test-jira-client-id");
    ReflectionTestUtils.setField(service, "clientSecret", "test-jira-client-secret");
  }

  // ── isConfigured ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("isConfigured: returns true when both client-id and secret are set")
  void isConfigured_true() {
    assertThat(service.isConfigured()).isTrue();
  }

  @Test
  @DisplayName("isConfigured: returns false when client-id is blank")
  void isConfigured_false_blankClientId() {
    ReflectionTestUtils.setField(service, "clientId", "");
    assertThat(service.isConfigured()).isFalse();
  }

  @Test
  @DisplayName("isConfigured: returns false when client-secret is blank")
  void isConfigured_false_blankSecret() {
    ReflectionTestUtils.setField(service, "clientSecret", "");
    assertThat(service.isConfigured()).isFalse();
  }

  // ── generateAuthorizationUrl ──────────────────────────────────────────────

  @Test
  @DisplayName("generateAuthorizationUrl: returns Atlassian auth URL with required params")
  void generateAuthorizationUrl_containsRequiredParams() {
    String url = service.generateAuthorizationUrl("https://shipflow.dev");
    assertThat(url).startsWith("https://auth.atlassian.com/authorize");
    assertThat(url).contains("response_type=code");
    assertThat(url).contains("client_id=test-jira-client-id");
    assertThat(url).contains("audience=");
    assertThat(url).contains("scope=");
    assertThat(url).contains("state=");
    assertThat(url).contains("redirect_uri=");
    assertThat(url).contains("prompt=consent");
  }

  @Test
  @DisplayName("generateAuthorizationUrl: callback points to /api/import/jira/callback")
  void generateAuthorizationUrl_correctCallbackPath() {
    String url = service.generateAuthorizationUrl("https://shipflow.dev");
    // redirect_uri is URL-encoded; check the decoded form
    assertThat(url).contains("jira%2Fcallback");
  }

  // ── processCallback — invalid state ───────────────────────────────────────

  @Test
  @DisplayName("processCallback: returns error redirect for unknown state")
  void processCallback_unknownState() {
    String redirect = service.processCallback("some-code", "no-such-state");
    assertThat(redirect).contains("jira_error=invalid_state");
  }

  // ── getConnectionStatus ───────────────────────────────────────────────────

  @Test
  @DisplayName("getConnectionStatus: not connected when no token stored")
  void getConnectionStatus_notConnected() {
    OrganizationSettings settings =
        OrganizationSettings.builder().organizationName("Test").updatedBy("system").build();
    when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));

    JiraConnectionStatusDTO status = service.getConnectionStatus();

    assertThat(status.isConnected()).isFalse();
    assertThat(status.isConfigured()).isTrue();
    assertThat(status.getCloudId()).isNull();
    assertThat(status.getCloudName()).isNull();
  }

  @Test
  @DisplayName("getConnectionStatus: connected when access token and cloud id are present")
  void getConnectionStatus_connected() {
    OrganizationSettings settings =
        OrganizationSettings.builder()
            .organizationName("Test")
            .updatedBy("system")
            .jiraAccessToken("atl-token-xyz")
            .jiraCloudId("cloud-001")
            .jiraCloudName("Acme Jira")
            .build();
    when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));

    JiraConnectionStatusDTO status = service.getConnectionStatus();

    assertThat(status.isConnected()).isTrue();
    assertThat(status.getCloudId()).isEqualTo("cloud-001");
    assertThat(status.getCloudName()).isEqualTo("Acme Jira");
  }

  // ── disconnect ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("disconnect: clears all Jira token fields from settings")
  void disconnect_clearsTokens() {
    OrganizationSettings settings =
        OrganizationSettings.builder()
            .organizationName("Test")
            .updatedBy("system")
            .jiraAccessToken("atl-tok")
            .jiraRefreshToken("ref-tok")
            .jiraCloudId("c1")
            .jiraCloudName("Cloud One")
            .build();
    when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
    when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.disconnect();

    assertThat(settings.getJiraAccessToken()).isNull();
    assertThat(settings.getJiraRefreshToken()).isNull();
    assertThat(settings.getJiraCloudId()).isNull();
    assertThat(settings.getJiraCloudName()).isNull();
  }

  // ── getStoredAccessToken / getStoredCloudId ───────────────────────────────

  @Test
  @DisplayName("getStoredAccessToken: returns stored token")
  void getStoredAccessToken_returnsToken() {
    OrganizationSettings settings =
        OrganizationSettings.builder()
            .organizationName("Test")
            .updatedBy("system")
            .jiraAccessToken("my-token")
            .build();
    when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));

    assertThat(service.getStoredAccessToken()).isEqualTo("my-token");
  }

  @Test
  @DisplayName("getStoredCloudId: returns stored cloud id")
  void getStoredCloudId_returnsCloudId() {
    OrganizationSettings settings =
        OrganizationSettings.builder()
            .organizationName("Test")
            .updatedBy("system")
            .jiraCloudId("cloud-42")
            .build();
    when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));

    assertThat(service.getStoredCloudId()).isEqualTo("cloud-42");
  }

  // ── getOrCreate — no existing settings ───────────────────────────────────

  @Test
  @DisplayName("getConnectionStatus: creates new settings when none exist")
  void getConnectionStatus_createsSettingsIfAbsent() {
    when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
    when(settingsRepository.save(any()))
        .thenAnswer(inv -> inv.getArgument(0));

    JiraConnectionStatusDTO status = service.getConnectionStatus();

    verify(settingsRepository).save(any(OrganizationSettings.class));
    assertThat(status.isConnected()).isFalse();
  }
}
