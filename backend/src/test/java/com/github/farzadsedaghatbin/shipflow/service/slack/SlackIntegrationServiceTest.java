package com.github.farzadsedaghatbin.shipflow.service.slack;

import com.github.farzadsedaghatbin.shipflow.dto.slack.*;
import com.github.farzadsedaghatbin.shipflow.entity.slack.*;
import com.github.farzadsedaghatbin.shipflow.repository.slack.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SlackIntegrationService
 * Tests Slack configuration, notification sending, and channel management
 */
@ExtendWith(MockitoExtension.class)
class SlackIntegrationServiceTest {

    @Mock
    private SlackConfigurationRepository slackConfigRepository;

    @Mock
    private SlackChannelConfigRepository channelConfigRepository;

    @Mock
    private SlackNotificationHistoryRepository historyRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SlackIntegrationService slackService;

    private SlackConfiguration testConfig;
    private SlackChannelConfig testChannelConfig;

    @BeforeEach
    void setUp() {
        testConfig = SlackConfiguration.builder()
                .id(1L)
                .workspaceName("test-workspace")
                .webhookUrl("https://hooks.slack.com/services/TEST/URL")
                .defaultChannel("general")
                .isEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testChannelConfig = SlackChannelConfig.builder()
                .id(1L)
                .slackConfiguration(testConfig)
                .channelName("engineering")
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
    void createOrUpdateConfiguration_NewConfiguration_ShouldCreate() {
        // Given
        CreateSlackConfigurationRequest request = CreateSlackConfigurationRequest.builder()
                .workspaceName("new-workspace")
                .webhookUrl("https://hooks.slack.com/services/NEW/URL")
                .defaultChannel("general")
                .isEnabled(true)
                .build();

        when(slackConfigRepository.findByWorkspaceName("new-workspace")).thenReturn(Optional.empty());
        when(slackConfigRepository.save(any(SlackConfiguration.class))).thenReturn(testConfig);

        // When
        SlackConfigurationDTO result = slackService.createOrUpdateConfiguration(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getWorkspaceName()).isEqualTo("test-workspace");
        verify(slackConfigRepository).save(any(SlackConfiguration.class));
    }

    @Test
    void createOrUpdateConfiguration_ExistingConfiguration_ShouldUpdate() {
        // Given
        CreateSlackConfigurationRequest request = CreateSlackConfigurationRequest.builder()
                .workspaceName("test-workspace")
                .webhookUrl("https://hooks.slack.com/services/UPDATED/URL")
                .defaultChannel("updates")
                .isEnabled(false)
                .build();

        when(slackConfigRepository.findByWorkspaceName("test-workspace")).thenReturn(Optional.of(testConfig));
        when(slackConfigRepository.save(any(SlackConfiguration.class))).thenReturn(testConfig);

        // When
        SlackConfigurationDTO result = slackService.createOrUpdateConfiguration(request);

        // Then
        assertThat(result).isNotNull();
        verify(slackConfigRepository).save(any(SlackConfiguration.class));
    }

    @Test
    void getAllConfigurations_ShouldReturnAll() {
        // Given
        when(slackConfigRepository.findAll()).thenReturn(List.of(testConfig));

        // When
        List<SlackConfigurationDTO> results = slackService.getAllConfigurations();

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getWorkspaceName()).isEqualTo("test-workspace");
    }

    @Test
    void getActiveConfiguration_WhenExists_ShouldReturn() {
        // Given
        when(slackConfigRepository.findFirstByIsEnabledTrue()).thenReturn(Optional.of(testConfig));

        // When
        Optional<SlackConfigurationDTO> result = slackService.getActiveConfiguration();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getWorkspaceName()).isEqualTo("test-workspace");
    }

    @Test
    void getActiveConfiguration_WhenNotExists_ShouldReturnEmpty() {
        // Given
        when(slackConfigRepository.findFirstByIsEnabledTrue()).thenReturn(Optional.empty());

        // When
        Optional<SlackConfigurationDTO> result = slackService.getActiveConfiguration();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void deleteConfiguration_ShouldDelete() {
        // Given
        doNothing().when(slackConfigRepository).deleteById(1L);

        // When
        slackService.deleteConfiguration(1L);

        // Then
        verify(slackConfigRepository).deleteById(1L);
    }

    @Test
    void createOrUpdateChannelConfig_NewChannel_ShouldCreate() {
        // Given
        CreateSlackChannelConfigRequest request = CreateSlackChannelConfigRequest.builder()
                .channelName("new-channel")
                .notifyTaskAssigned(true)
                .build();

        when(slackConfigRepository.findById(1L)).thenReturn(Optional.of(testConfig));
        when(channelConfigRepository.findBySlackConfigurationIdAndChannelName(1L, "new-channel"))
                .thenReturn(Optional.empty());
        when(channelConfigRepository.save(any(SlackChannelConfig.class))).thenReturn(testChannelConfig);

        // When
        SlackChannelConfigDTO result = slackService.createOrUpdateChannelConfig(1L, request);

        // Then
        assertThat(result).isNotNull();
        verify(channelConfigRepository).save(any(SlackChannelConfig.class));
    }

    @Test
    void createOrUpdateChannelConfig_ExistingChannel_ShouldUpdate() {
        // Given
        CreateSlackChannelConfigRequest request = CreateSlackChannelConfigRequest.builder()
                .channelName("engineering")
                .notifyTaskBlocked(true)
                .build();

        when(slackConfigRepository.findById(1L)).thenReturn(Optional.of(testConfig));
        when(channelConfigRepository.findBySlackConfigurationIdAndChannelName(1L, "engineering"))
                .thenReturn(Optional.of(testChannelConfig));
        when(channelConfigRepository.save(any(SlackChannelConfig.class))).thenReturn(testChannelConfig);

        // When
        SlackChannelConfigDTO result = slackService.createOrUpdateChannelConfig(1L, request);

        // Then
        assertThat(result).isNotNull();
        verify(channelConfigRepository).save(any(SlackChannelConfig.class));
    }

    @Test
    void getChannelConfigs_ShouldReturnAll() {
        // Given
        when(channelConfigRepository.findBySlackConfigurationId(1L)).thenReturn(List.of(testChannelConfig));

        // When
        List<SlackChannelConfigDTO> results = slackService.getChannelConfigs(1L);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getChannelName()).isEqualTo("engineering");
    }

    @Test
    void deleteChannelConfig_ShouldDelete() {
        // Given
        doNothing().when(channelConfigRepository).deleteById(1L);

        // When
        slackService.deleteChannelConfig(1L);

        // Then
        verify(channelConfigRepository).deleteById(1L);
    }

    @Test
    void sendNotification_WhenNoActiveConfig_ShouldNotSend() {
        // Given
        when(slackConfigRepository.findFirstByIsEnabledTrue()).thenReturn(Optional.empty());

        // When
        slackService.sendNotification("TASK_ASSIGNED", "Test message", "general", "TASK", 1L);

        // Then
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void sendNotification_WhenNotificationDisabledForChannel_ShouldNotSend() {
        // Given
        testChannelConfig.setNotifyTaskBlocked(false);
        
        when(slackConfigRepository.findFirstByIsEnabledTrue()).thenReturn(Optional.of(testConfig));
        when(channelConfigRepository.findBySlackConfigurationIdAndChannelName(1L, "engineering"))
                .thenReturn(Optional.of(testChannelConfig));

        // When
        slackService.sendNotification("TASK_BLOCKED", "Test message", "engineering", "TASK", 1L);

        // Then
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    void sendNotification_Success_ShouldLogHistory() {
        // Given
        when(slackConfigRepository.findFirstByIsEnabledTrue()).thenReturn(Optional.of(testConfig));
        when(channelConfigRepository.findBySlackConfigurationIdAndChannelName(anyLong(), anyString()))
                .thenReturn(Optional.of(testChannelConfig));
        when(restTemplate.postForEntity(anyString(), any(), any())).thenReturn(null);
        when(historyRepository.save(any())).thenReturn(new SlackNotificationHistory());

        // When
        slackService.sendNotification("TASK_ASSIGNED", "Test message", "engineering", "TASK", 1L);

        // Then
        ArgumentCaptor<SlackNotificationHistory> historyCaptor = ArgumentCaptor.forClass(SlackNotificationHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        
        SlackNotificationHistory savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.getNotificationType()).isEqualTo("TASK_ASSIGNED");
        assertThat(savedHistory.getSuccess()).isTrue();
    }

    @Test
    void sendNotification_Failure_ShouldLogError() {
        // Given
        when(slackConfigRepository.findFirstByIsEnabledTrue()).thenReturn(Optional.of(testConfig));
        when(channelConfigRepository.findBySlackConfigurationIdAndChannelName(anyLong(), anyString()))
                .thenReturn(Optional.of(testChannelConfig));
        when(restTemplate.postForEntity(anyString(), any(), any()))
                .thenThrow(new RuntimeException("Connection failed"));
        when(historyRepository.save(any())).thenReturn(new SlackNotificationHistory());

        // When
        slackService.sendNotification("TASK_ASSIGNED", "Test message", "engineering", "TASK", 1L);

        // Then
        ArgumentCaptor<SlackNotificationHistory> historyCaptor = ArgumentCaptor.forClass(SlackNotificationHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        
        SlackNotificationHistory savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.getSuccess()).isFalse();
        assertThat(savedHistory.getErrorMessage()).contains("Connection failed");
    }

    @Test
    void getNotificationHistory_ShouldReturnHistory() {
        // Given
        SlackNotificationHistory history = SlackNotificationHistory.builder()
                .id(1L)
                .slackConfiguration(testConfig)
                .notificationType("TASK_ASSIGNED")
                .success(true)
                .sentAt(LocalDateTime.now())
                .build();

        when(historyRepository.findBySlackConfigurationIdOrderBySentAtDesc(1L))
                .thenReturn(List.of(history));

        // When
        List<SlackNotificationHistory> results = slackService.getNotificationHistory(1L);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNotificationType()).isEqualTo("TASK_ASSIGNED");
    }
}
