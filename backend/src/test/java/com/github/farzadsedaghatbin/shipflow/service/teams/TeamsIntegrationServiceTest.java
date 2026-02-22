package com.github.farzadsedaghatbin.shipflow.service.teams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.teams.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.FlowType;
import com.github.farzadsedaghatbin.shipflow.entity.teams.*;
import com.github.farzadsedaghatbin.shipflow.repository.teams.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/** Unit tests for TeamsIntegrationService */
@ExtendWith(MockitoExtension.class)
class TeamsIntegrationServiceTest {

  @Mock
  private TeamsConfigurationRepository teamsConfigRepository;

  @Mock
  private TeamsChannelConfigRepository channelConfigRepository;

  @Mock
  private TeamsNotificationHistoryRepository historyRepository;

  @Mock
  private RestTemplate webhookRestTemplate;

  private TeamsIntegrationService teamsService;

  private TeamsConfiguration testConfig;
  private TeamsChannelConfig testChannelConfig;

  @BeforeEach
  void setUp() {
    // Manually create service with webhook RestTemplate mock
    teamsService = new TeamsIntegrationService(teamsConfigRepository, channelConfigRepository, historyRepository,
        webhookRestTemplate);

    testConfig = TeamsConfiguration.builder().id(1L).tenantName("Test Tenant")
        .webhookUrl("https://outlook.office.com/webhook/test").defaultChannel("General").isEnabled(true)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

    testChannelConfig = TeamsChannelConfig.builder().id(1L).teamsConfiguration(testConfig).channelName("dev-team")
        .flowType(FlowType.WEBHOOK).notifyTaskAssigned(true).notifyTaskCompleted(true).notifyTaskBlocked(false)
        .notifyPitchShaped(true).notifyCycleStarted(true).notifyCycleCooldown(true)
        .notifyBettingCompleted(false).notifySprintStarted(false).createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now()).build();
  }

  @Test
  void createConfiguration_NewConfiguration_ShouldCreateSuccessfully() {
    // Given
    CreateTeamsConfigurationRequest request = CreateTeamsConfigurationRequest.builder().tenantName("New Tenant")
        .webhookUrl("https://outlook.office.com/webhook/new").defaultChannel("General").isEnabled(true).build();

    when(teamsConfigRepository.findByTenantName("New Tenant")).thenReturn(Optional.empty());
    when(teamsConfigRepository.save(any(TeamsConfiguration.class))).thenReturn(testConfig);

    // When
    TeamsConfigurationDTO result = teamsService.createOrUpdateConfiguration(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getTenantName()).isEqualTo("Test Tenant");
    verify(teamsConfigRepository).save(any(TeamsConfiguration.class));
  }

  @Test
  void createConfiguration_ExistingConfiguration_ShouldUpdate() {
    // Given
    CreateTeamsConfigurationRequest request = CreateTeamsConfigurationRequest.builder().tenantName("Test Tenant")
        .webhookUrl("https://outlook.office.com/webhook/updated").isEnabled(false).build();

    when(teamsConfigRepository.findByTenantName("Test Tenant")).thenReturn(Optional.of(testConfig));
    when(teamsConfigRepository.save(any(TeamsConfiguration.class))).thenReturn(testConfig);

    // When
    TeamsConfigurationDTO result = teamsService.createOrUpdateConfiguration(request);

    // Then
    assertThat(result).isNotNull();
    verify(teamsConfigRepository).save(testConfig);
  }

  @Test
  void getAllConfigurations_ShouldReturnAllConfigurations() {
    // Given
    when(teamsConfigRepository.findAll()).thenReturn(List.of(testConfig));

    // When
    List<TeamsConfigurationDTO> results = teamsService.getAllConfigurations();

    // Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getTenantName()).isEqualTo("Test Tenant");
  }

  @Test
  void getActiveConfiguration_WhenExists_ShouldReturnConfiguration() {
    // Given
    when(teamsConfigRepository.findFirstByIsEnabledTrue()).thenReturn(Optional.of(testConfig));

    // When
    Optional<TeamsConfigurationDTO> result = teamsService.getActiveConfiguration();

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getTenantName()).isEqualTo("Test Tenant");
  }

  @Test
  void getActiveConfiguration_WhenNotExists_ShouldReturnEmpty() {
    // Given
    when(teamsConfigRepository.findFirstByIsEnabledTrue()).thenReturn(Optional.empty());

    // When
    Optional<TeamsConfigurationDTO> result = teamsService.getActiveConfiguration();

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void deleteConfiguration_ShouldCallRepository() {
    // When
    teamsService.deleteConfiguration(1L);

    // Then
    verify(teamsConfigRepository).deleteById(1L);
  }

  @Test
  void createChannelConfig_NewChannel_ShouldCreateSuccessfully() {
    // Given
    CreateTeamsChannelConfigRequest request = CreateTeamsChannelConfigRequest.builder().channelName("new-channel")
        .flowType(FlowType.POWER_AUTOMATE_POST).notifyTaskAssigned(true).build();

    when(teamsConfigRepository.findById(1L)).thenReturn(Optional.of(testConfig));
    when(channelConfigRepository.findByTeamsConfigurationIdAndChannelName(1L, "new-channel"))
        .thenReturn(Optional.empty());
    when(channelConfigRepository.save(any(TeamsChannelConfig.class))).thenReturn(testChannelConfig);

    // When
    TeamsChannelConfigDTO result = teamsService.createOrUpdateChannelConfig(1L, request);

    // Then
    assertThat(result).isNotNull();
    verify(channelConfigRepository).save(any(TeamsChannelConfig.class));
  }

  @Test
  void createChannelConfig_ConfigNotFound_ShouldThrowException() {
    // Given
    CreateTeamsChannelConfigRequest request = CreateTeamsChannelConfigRequest.builder().channelName("test-channel")
        .build();

    when(teamsConfigRepository.findById(999L)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> teamsService.createOrUpdateChannelConfig(999L, request))
        .isInstanceOf(RuntimeException.class).hasMessageContaining("Teams configuration not found");
  }

  @Test
  void getChannelConfigs_ShouldReturnAllChannels() {
    // Given
    when(channelConfigRepository.findByTeamsConfigurationId(1L)).thenReturn(List.of(testChannelConfig));

    // When
    List<TeamsChannelConfigDTO> results = teamsService.getChannelConfigs(1L);

    // Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getChannelName()).isEqualTo("dev-team");
  }

  @Test
  void deleteChannelConfig_ShouldCallRepository() {
    // When
    teamsService.deleteChannelConfig(1L);

    // Then
    verify(channelConfigRepository).deleteById(1L);
  }

  @Test
  void sendNotification_WithActiveConfig_ShouldSendSuccessfully() {
    // Given
    when(teamsConfigRepository.findFirstByIsEnabledTrue()).thenReturn(Optional.of(testConfig));
    when(channelConfigRepository.findByTeamsConfigurationIdAndChannelName(1L, "General"))
        .thenReturn(Optional.of(testChannelConfig));
    when(webhookRestTemplate.postForEntity(any(java.net.URI.class), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok("OK"));
    when(historyRepository.save(any(TeamsNotificationHistory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    teamsService.sendNotification("TASK_ASSIGNED", "Test message", "General", "TASK", 123L);

    // Then
    verify(webhookRestTemplate).postForEntity(any(java.net.URI.class), any(HttpEntity.class), eq(String.class));
    verify(historyRepository).save(any(TeamsNotificationHistory.class));
  }

  @Test
  void sendNotification_NoActiveConfig_ShouldSkip() {
    // Given
    when(teamsConfigRepository.findFirstByIsEnabledTrue()).thenReturn(Optional.empty());

    // When
    teamsService.sendNotification("TASK_ASSIGNED", "Test message", null, null, null);

    // Then
    verify(webhookRestTemplate, never()).postForEntity(any(java.net.URI.class), any(), any());
    verify(historyRepository, never()).save(any());
  }

  @Test
  void sendNotification_DisabledNotificationType_ShouldSkip() {
    // Given
    testChannelConfig.setNotifyTaskBlocked(false);
    when(teamsConfigRepository.findFirstByIsEnabledTrue()).thenReturn(Optional.of(testConfig));
    when(channelConfigRepository.findByTeamsConfigurationIdAndChannelName(1L, "General"))
        .thenReturn(Optional.of(testChannelConfig));

    // When
    teamsService.sendNotification("TASK_BLOCKED", "Test message", "General", null, null);

    // Then
    verify(webhookRestTemplate, never()).postForEntity(any(java.net.URI.class), any(), any());
  }

  @Test
  void sendNotification_RestTemplateFails_ShouldRecordFailure() {
    // Given
    when(teamsConfigRepository.findFirstByIsEnabledTrue()).thenReturn(Optional.of(testConfig));
    when(webhookRestTemplate.postForEntity(any(java.net.URI.class), any(HttpEntity.class), eq(String.class)))
        .thenThrow(new RuntimeException("Network error"));
    when(historyRepository.save(any(TeamsNotificationHistory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    teamsService.sendNotification("TASK_ASSIGNED", "Test message", null, null, null);

    // Then
    verify(historyRepository).save(argThat(history -> !history.getSuccess() && history.getErrorMessage() != null));
  }

  @Test
  void sendTestNotification_ShouldSendSuccessfully() {
    // Given
    TestTeamsNotificationRequest request = TestTeamsNotificationRequest.builder().message("Test notification")
        .build();

    when(teamsConfigRepository.findById(1L)).thenReturn(Optional.of(testConfig));
    when(webhookRestTemplate.postForEntity(any(java.net.URI.class), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok("OK"));

    // When
    teamsService.sendTestNotification(1L, request);

    // Then
    verify(webhookRestTemplate).postForEntity(any(java.net.URI.class), any(HttpEntity.class), eq(String.class));
  }

  @Test
  void sendTestNotification_ConfigNotFound_ShouldThrowException() {
    // Given
    TestTeamsNotificationRequest request = TestTeamsNotificationRequest.builder().message("Test").build();

    when(teamsConfigRepository.findById(999L)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> teamsService.sendTestNotification(999L, request))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Teams configuration not found");
  }

  @Test
  void getNotificationHistory_ShouldReturnHistory() {
    // Given
    TeamsNotificationHistory history = TeamsNotificationHistory.builder().id(1L).teamsConfiguration(testConfig)
        .channelName("General").notificationType("TASK_ASSIGNED").messageText("Test message")
        .sentAt(LocalDateTime.now()).success(true).build();

    when(historyRepository.findTop50ByTeamsConfigurationIdOrderBySentAtDesc(1L)).thenReturn(List.of(history));

    // When
    List<TeamsNotificationHistoryDTO> results = teamsService.getNotificationHistory(1L);

    // Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getNotificationType()).isEqualTo("TASK_ASSIGNED");
  }

  @Test
  void createChannelConfig_WithFlowType_ShouldSetCorrectFlowType() {
    // Given
    CreateTeamsChannelConfigRequest request = CreateTeamsChannelConfigRequest.builder()
        .channelName("power-automate-channel").flowType(FlowType.POWER_AUTOMATE_THREAD).notifyTaskAssigned(true)
        .build();

    TeamsChannelConfig savedConfig = TeamsChannelConfig.builder().id(2L).teamsConfiguration(testConfig)
        .channelName("power-automate-channel").flowType(FlowType.POWER_AUTOMATE_THREAD).notifyTaskAssigned(true)
        .notifyTaskCompleted(false).notifyTaskBlocked(false).notifyPitchShaped(false).notifyCycleStarted(false)
        .notifyCycleCooldown(false).notifyBettingCompleted(false).notifySprintStarted(false)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

    when(teamsConfigRepository.findById(1L)).thenReturn(Optional.of(testConfig));
    when(channelConfigRepository.findByTeamsConfigurationIdAndChannelName(1L, "power-automate-channel"))
        .thenReturn(Optional.empty());
    when(channelConfigRepository.save(any(TeamsChannelConfig.class))).thenReturn(savedConfig);

    // When
    TeamsChannelConfigDTO result = teamsService.createOrUpdateChannelConfig(1L, request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getFlowType()).isEqualTo(FlowType.POWER_AUTOMATE_THREAD);
    verify(channelConfigRepository).save(any(TeamsChannelConfig.class));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void sendTestNotification_PowerAutomate_ShouldSendAdaptiveCard() {
    // Given - Power Automate URL
    String powerAutomateUrl = "https://default300eebd4b8694d1a8df6e0a23ad188.d7.environment.api.powerplatform.com/powerautomate/automations/direct/workflows/7c0029b148734b5981552a0d53a30348/triggers/manual/paths/invoke?api-version=1&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=qsqEJmEOgq_ELpcPu3sPxVnhO0aAASIQbYi4s2tgb6A";

    TeamsConfiguration powerAutomateConfig = TeamsConfiguration.builder().id(1L).tenantName("Power Automate Tenant")
        .webhookUrl(powerAutomateUrl).defaultChannel("General").isEnabled(true).build();

    TestTeamsNotificationRequest request = TestTeamsNotificationRequest.builder()
        .message("🚀 Test notification from ShipFlow - Your Teams integration is working!").build();

    when(teamsConfigRepository.findById(1L)).thenReturn(Optional.of(powerAutomateConfig));
    when(webhookRestTemplate.postForEntity(any(java.net.URI.class), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok("OK"));

    // When
    teamsService.sendTestNotification(1L, request);

    // Then - Verify the request was sent with correct payload
    ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    ArgumentCaptor<java.net.URI> uriCaptor = ArgumentCaptor.forClass(java.net.URI.class);

    verify(webhookRestTemplate).postForEntity(uriCaptor.capture(), entityCaptor.capture(), eq(String.class));

    // Verify URL (URI now, not String)
    assertThat(uriCaptor.getValue().toString()).isEqualTo(powerAutomateUrl);

    // Verify payload structure - Power Automate requires Adaptive Card format
    HttpEntity<Map<String, Object>> capturedEntity = (HttpEntity<Map<String, Object>>) entityCaptor.getValue();
    Map<String, Object> payload = capturedEntity.getBody();

    assertThat(payload).isNotNull();
    assertThat(payload.get("type")).isEqualTo("AdaptiveCard");
    assertThat(payload.get("version")).isEqualTo("1.3");
    assertThat(payload.get("body")).isInstanceOf(java.util.List.class);

    // Verify headers
    assertThat(capturedEntity.getHeaders().getContentType().toString()).contains("application/json");
    assertThat(capturedEntity.getHeaders().get("User-Agent")).contains("ShipFlow-Teams-Integration/1.0");
  }

  @Test
  void sendTestNotification_PowerAutomate401_ShouldThrowHelpfulError() {
    // Given - Power Automate URL returning 401
    String powerAutomateUrl = "https://default300eebd4b8694d1a8df6e0a23ad188.d7.environment.api.powerplatform.com/powerautomate/workflows/test";

    TeamsConfiguration config = TeamsConfiguration.builder().id(1L).tenantName("Test Tenant")
        .webhookUrl(powerAutomateUrl).isEnabled(true).build();

    TestTeamsNotificationRequest request = TestTeamsNotificationRequest.builder().message("Test").build();

    when(teamsConfigRepository.findById(1L)).thenReturn(Optional.of(config));

    // Simulate 401 Unauthorized with AuthorizationFailed error
    String errorBody = "{\"error\":{\"code\":\"AuthorizationFailed\",\"message\":\"You do not have permissions to perform action 'run' on scope '/triggers/manual/paths/'\"}}";
    when(webhookRestTemplate.postForEntity(any(java.net.URI.class), any(HttpEntity.class), eq(String.class)))
        .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized",
            org.springframework.http.HttpHeaders.EMPTY, errorBody.getBytes(), null));

    // When/Then - Should throw with helpful message
    assertThatThrownBy(() -> teamsService.sendTestNotification(1L, request)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("POWER AUTOMATE SECURITY")
        .hasMessageContaining("flow is rejecting anonymous requests")
        .hasMessageContaining("Who can trigger the flow").hasMessageContaining("Select 'Anyone'");
  }

  @Test
  void sendTestNotification_PowerAutomate404_ShouldThrowHelpfulError() {
    // Given - Power Automate URL returning 404
    String powerAutomateUrl = "https://prod-123.powerplatform.com/workflows/deleted-flow";

    TeamsConfiguration config = TeamsConfiguration.builder().id(1L).tenantName("Test Tenant")
        .webhookUrl(powerAutomateUrl).isEnabled(true).build();

    TestTeamsNotificationRequest request = TestTeamsNotificationRequest.builder().message("Test").build();

    when(teamsConfigRepository.findById(1L)).thenReturn(Optional.of(config));
    when(webhookRestTemplate.postForEntity(any(java.net.URI.class), any(HttpEntity.class), eq(String.class)))
        .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found",
            org.springframework.http.HttpHeaders.EMPTY, "".getBytes(), null));

    // When/Then
    assertThatThrownBy(() -> teamsService.sendTestNotification(1L, request)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("POWER AUTOMATE FLOW")
        .hasMessageContaining("URL appears to be invalid or the flow was deleted");
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void sendTestNotification_TraditionalWebhook_ShouldSendMessageCard() {
    // Given - Traditional Teams webhook URL
    String webhookUrl = "https://outlook.office.com/webhook/abc123/IncomingWebhook/xyz789";

    TeamsConfiguration config = TeamsConfiguration.builder().id(1L).tenantName("Test Tenant").webhookUrl(webhookUrl)
        .isEnabled(true).build();

    TestTeamsNotificationRequest request = TestTeamsNotificationRequest.builder().message("Test notification")
        .build();

    when(teamsConfigRepository.findById(1L)).thenReturn(Optional.of(config));
    when(webhookRestTemplate.postForEntity(any(java.net.URI.class), any(HttpEntity.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok("1"));

    // When
    teamsService.sendTestNotification(1L, request);

    // Then
    ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(webhookRestTemplate).postForEntity(any(java.net.URI.class), entityCaptor.capture(), eq(String.class));

    HttpEntity<Map<String, Object>> capturedEntity = (HttpEntity<Map<String, Object>>) entityCaptor.getValue();
    Map<String, Object> payload = capturedEntity.getBody();

    // Traditional webhook should use MessageCard format
    assertThat(payload).isNotNull();
    assertThat(payload.get("@type")).isEqualTo("MessageCard");
    assertThat(payload.get("@context")).isEqualTo("http://schema.org/extensions");
  }

  @Test
  void sendTestNotification_InvalidWebhookUrl_ShouldThrowError() {
    // Given - Invalid URL
    TeamsConfiguration config = TeamsConfiguration.builder().id(1L).tenantName("Test Tenant")
        .webhookUrl("not-a-valid-url").isEnabled(true).build();

    TestTeamsNotificationRequest request = TestTeamsNotificationRequest.builder().message("Test").build();

    when(teamsConfigRepository.findById(1L)).thenReturn(Optional.of(config));

    // When/Then
    assertThatThrownBy(() -> teamsService.sendTestNotification(1L, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Teams webhook URL must use HTTPS protocol");
  }
}
