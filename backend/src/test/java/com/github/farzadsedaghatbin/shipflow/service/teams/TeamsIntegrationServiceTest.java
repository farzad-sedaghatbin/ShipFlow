package com.github.farzadsedaghatbin.shipflow.service.teams;

import com.github.farzadsedaghatbin.shipflow.dto.teams.*;
import com.github.farzadsedaghatbin.shipflow.entity.teams.*;
import com.github.farzadsedaghatbin.shipflow.repository.teams.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TeamsIntegrationService
 */
@ExtendWith(MockitoExtension.class)
class TeamsIntegrationServiceTest {

    @Mock
    private TeamsConfigurationRepository teamsConfigRepository;

    @Mock
    private TeamsChannelConfigRepository channelConfigRepository;

    @Mock
    private TeamsNotificationHistoryRepository historyRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TeamsIntegrationService teamsService;

    private TeamsConfiguration testConfig;
    private TeamsChannelConfig testChannelConfig;

    @BeforeEach
    void setUp() {
        testConfig = TeamsConfiguration.builder()
                .id(1L)
                .tenantName("Test Tenant")
                .webhookUrl("https://outlook.office.com/webhook/test")
                .defaultChannel("General")
                .isEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testChannelConfig = TeamsChannelConfig.builder()
                .id(1L)
                .teamsConfiguration(testConfig)
                .channelName("dev-team")
                .notifyTaskAssigned(true)
                .notifyTaskCompleted(true)
                .notifyTaskBlocked(false)
                .notifyPitchShaped(true)
                .notifyCycleStarted(true)
                .notifyCycleCooldown(true)
                .notifyBettingCompleted(false)
                .notifySprintStarted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createConfiguration_NewConfiguration_ShouldCreateSuccessfully() {
        // Given
        CreateTeamsConfigurationRequest request = CreateTeamsConfigurationRequest.builder()
                .tenantName("New Tenant")
                .webhookUrl("https://outlook.office.com/webhook/new")
                .defaultChannel("General")
                .isEnabled(true)
                .build();

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
        CreateTeamsConfigurationRequest request = CreateTeamsConfigurationRequest.builder()
                .tenantName("Test Tenant")
                .webhookUrl("https://outlook.office.com/webhook/updated")
                .isEnabled(false)
                .build();

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
        CreateTeamsChannelConfigRequest request = CreateTeamsChannelConfigRequest.builder()
                .channelName("new-channel")
                .notifyTaskAssigned(true)
                .build();

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
        CreateTeamsChannelConfigRequest request = CreateTeamsChannelConfigRequest.builder()
                .channelName("test-channel")
                .build();

        when(teamsConfigRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> teamsService.createOrUpdateChannelConfig(999L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Teams configuration not found");
    }

    @Test
    void getChannelConfigs_ShouldReturnAllChannels() {
        // Given
        when(channelConfigRepository.findByTeamsConfigurationId(1L))
                .thenReturn(List.of(testChannelConfig));

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
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));
        when(historyRepository.save(any(TeamsNotificationHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        teamsService.sendNotification("TASK_ASSIGNED", "Test message", "General", "TASK", 123L);

        // Then
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
        verify(historyRepository).save(any(TeamsNotificationHistory.class));
    }

    @Test
    void sendNotification_NoActiveConfig_ShouldSkip() {
        // Given
        when(teamsConfigRepository.findFirstByIsEnabledTrue()).thenReturn(Optional.empty());

        // When
        teamsService.sendNotification("TASK_ASSIGNED", "Test message", null, null, null);

        // Then
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
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
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    void sendNotification_RestTemplateFails_ShouldRecordFailure() {
        // Given
        when(teamsConfigRepository.findFirstByIsEnabledTrue()).thenReturn(Optional.of(testConfig));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));
        when(historyRepository.save(any(TeamsNotificationHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        teamsService.sendNotification("TASK_ASSIGNED", "Test message", null, null, null);

        // Then
        verify(historyRepository).save(argThat(history -> 
            !history.getSuccess() && history.getErrorMessage() != null
        ));
    }

    @Test
    void sendTestNotification_ShouldSendSuccessfully() {
        // Given
        TestTeamsNotificationRequest request = TestTeamsNotificationRequest.builder()
                .message("Test notification")
                .build();

        when(teamsConfigRepository.findById(1L)).thenReturn(Optional.of(testConfig));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        // When
        teamsService.sendTestNotification(1L, request);

        // Then
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void sendTestNotification_ConfigNotFound_ShouldThrowException() {
        // Given
        TestTeamsNotificationRequest request = TestTeamsNotificationRequest.builder()
                .message("Test")
                .build();

        when(teamsConfigRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> teamsService.sendTestNotification(999L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Teams configuration not found");
    }

    @Test
    void getNotificationHistory_ShouldReturnHistory() {
        // Given
        TeamsNotificationHistory history = TeamsNotificationHistory.builder()
                .id(1L)
                .teamsConfiguration(testConfig)
                .channelName("General")
                .notificationType("TASK_ASSIGNED")
                .messageText("Test message")
                .sentAt(LocalDateTime.now())
                .success(true)
                .build();

        when(historyRepository.findTop50ByTeamsConfigurationIdOrderBySentAtDesc(1L))
                .thenReturn(List.of(history));

        // When
        List<TeamsNotificationHistoryDTO> results = teamsService.getNotificationHistory(1L);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNotificationType()).isEqualTo("TASK_ASSIGNED");
    }
}
