package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.LinearConnectionStatusDTO;
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
 * Pure unit tests for LinearOAuthService — no Spring context.
 */
@ExtendWith(MockitoExtension.class)
class LinearOAuthServiceTest {

  @Mock private OrganizationSettingsRepository settingsRepository;
  @Mock private RestTemplate restTemplate;

  private LinearOAuthService service;

  @BeforeEach
  void setUp() {
    service = new LinearOAuthService(settingsRepository, restTemplate, new ObjectMapper());
    // Inject @Value fields via ReflectionTestUtils
    ReflectionTestUtils.setField(service, "clientId", "test-client-id");
    ReflectionTestUtils.setField(service, "clientSecret", "test-client-secret");
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
  @DisplayName("generateAuthorizationUrl: returns URL containing required OAuth params")
  void generateAuthorizationUrl_containsRequiredParams() {
    String url = service.generateAuthorizationUrl("https://shipflow.dev");
    assertThat(url).startsWith("https://linear.app/oauth/authorize");
    assertThat(url).contains("response_type=code");
    assertThat(url).contains("client_id=test-client-id");
    assertThat(url).contains("scope=read");
    assertThat(url).contains("state=");
    assertThat(url).contains("redirect_uri=");
  }

  // ── processCallback — invalid state ───────────────────────────────────────

  @Test
  @DisplayName("processCallback: returns error redirect for unknown state")
  void processCallback_unknownState() {
    String redirect = service.processCallback("some-code", "no-such-state");
    assertThat(redirect).contains("linear_error=invalid_state");
  }

  // ── getConnectionStatus ───────────────────────────────────────────────────

  @Test
  @DisplayName("getConnectionStatus: not connected when no token stored")
  void getConnectionStatus_notConnected() {
    OrganizationSettings settings =
        OrganizationSettings.builder()
            .organizationName("Test")
            .updatedBy("system")
            .build();
    when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));

    LinearConnectionStatusDTO status = service.getConnectionStatus();

    assertThat(status.isConnected()).isFalse();
    assertThat(status.isConfigured()).isTrue();
    assertThat(status.getTeamId()).isNull();
  }

  @Test
  @DisplayName("getConnectionStatus: connected when access token is present")
  void getConnectionStatus_connected() {
    OrganizationSettings settings =
        OrganizationSettings.builder()
            .organizationName("Test")
            .updatedBy("system")
            .linearAccessToken("tok_abc123")
            .linearTeamId("team-1")
            .linearTeamName("Acme Team")
            .build();
    when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));

    LinearConnectionStatusDTO status = service.getConnectionStatus();

    assertThat(status.isConnected()).isTrue();
    assertThat(status.getTeamId()).isEqualTo("team-1");
    assertThat(status.getTeamName()).isEqualTo("Acme Team");
  }

  // ── disconnect ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("disconnect: clears token and team from settings")
  void disconnect_clearsToken() {
    OrganizationSettings settings =
        OrganizationSettings.builder()
            .organizationName("Test")
            .updatedBy("system")
            .linearAccessToken("tok_abc")
            .linearTeamId("t1")
            .linearTeamName("Team One")
            .build();
    when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
    when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.disconnect();

    assertThat(settings.getLinearAccessToken()).isNull();
    assertThat(settings.getLinearTeamId()).isNull();
    assertThat(settings.getLinearTeamName()).isNull();
  }

  // ── saveSelectedTeam ──────────────────────────────────────────────────────

  @Test
  @DisplayName("saveSelectedTeam: persists teamId and teamName")
  void saveSelectedTeam_persists() {
    OrganizationSettings settings =
        OrganizationSettings.builder()
            .organizationName("Test")
            .updatedBy("system")
            .build();
    when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
    when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.saveSelectedTeam("team-99", "Super Team");

    assertThat(settings.getLinearTeamId()).isEqualTo("team-99");
    assertThat(settings.getLinearTeamName()).isEqualTo("Super Team");
  }
}
