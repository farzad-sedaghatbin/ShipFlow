package com.github.farzadsedaghatbin.shipflow.service.slack;

import com.github.farzadsedaghatbin.shipflow.dto.slack.*;
import com.github.farzadsedaghatbin.shipflow.entity.slack.*;
import com.github.farzadsedaghatbin.shipflow.repository.slack.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing Slack integration.
 * Handles configuration, sending notifications, and tracking history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SlackIntegrationService {

    private final SlackConfigurationRepository slackConfigRepository;
    private final SlackChannelConfigRepository channelConfigRepository;
    private final SlackNotificationHistoryRepository historyRepository;
    private final RestTemplate restTemplate;

    /**
     * Create or update Slack workspace configuration
     */
    public SlackConfigurationDTO createOrUpdateConfiguration(CreateSlackConfigurationRequest request) {
        Optional<SlackConfiguration> existing = slackConfigRepository.findByWorkspaceName(request.getWorkspaceName());
        
        SlackConfiguration config;
        if (existing.isPresent()) {
            config = existing.get();
            config.setWebhookUrl(request.getWebhookUrl());
            if (request.getDefaultChannel() != null) {
                config.setDefaultChannel(request.getDefaultChannel());
            }
            if (request.getIsEnabled() != null) {
                config.setIsEnabled(request.getIsEnabled());
            }
        } else {
            config = SlackConfiguration.builder()
                    .workspaceName(request.getWorkspaceName())
                    .webhookUrl(request.getWebhookUrl())
                    .defaultChannel(request.getDefaultChannel())
                    .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true)
                    .build();
        }
        
        SlackConfiguration saved = slackConfigRepository.save(config);
        log.info("Created/updated Slack configuration for workspace: {}", saved.getWorkspaceName());
        
        return toDTO(saved);
    }

    /**
     * Get all Slack configurations
     */
    @Transactional(readOnly = true)
    public List<SlackConfigurationDTO> getAllConfigurations() {
        return slackConfigRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get active Slack configuration
     */
    @Transactional(readOnly = true)
    public Optional<SlackConfigurationDTO> getActiveConfiguration() {
        return slackConfigRepository.findFirstByIsEnabledTrue()
                .map(this::toDTO);
    }

    /**
     * Delete Slack configuration
     */
    public void deleteConfiguration(Long configId) {
        slackConfigRepository.deleteById(configId);
        log.info("Deleted Slack configuration: {}", configId);
    }

    /**
     * Create or update channel configuration
     */
    public SlackChannelConfigDTO createOrUpdateChannelConfig(Long slackConfigId, CreateSlackChannelConfigRequest request) {
        SlackConfiguration slackConfig = slackConfigRepository.findById(slackConfigId)
                .orElseThrow(() -> new RuntimeException("Slack configuration not found with id: " + slackConfigId));
        
        Optional<SlackChannelConfig> existing = channelConfigRepository
                .findBySlackConfigurationIdAndChannelName(slackConfigId, request.getChannelName());
        
        SlackChannelConfig channelConfig;
        if (existing.isPresent()) {
            channelConfig = existing.get();
            updateChannelConfig(channelConfig, request);
        } else {
            channelConfig = SlackChannelConfig.builder()
                    .slackConfiguration(slackConfig)
                    .channelName(request.getChannelName())
                    .channelWebhookUrl(request.getChannelWebhookUrl())
                    .notifyTaskAssigned(request.getNotifyTaskAssigned() != null ? request.getNotifyTaskAssigned() : true)
                    .notifyTaskCompleted(request.getNotifyTaskCompleted() != null ? request.getNotifyTaskCompleted() : true)
                    .notifyTaskBlocked(request.getNotifyTaskBlocked() != null ? request.getNotifyTaskBlocked() : false)
                    .notifyPitchShaped(request.getNotifyPitchShaped() != null ? request.getNotifyPitchShaped() : true)
                    .notifyCycleStarted(request.getNotifyCycleStarted() != null ? request.getNotifyCycleStarted() : true)
                    .notifyCycleCooldown(request.getNotifyCycleCooldown() != null ? request.getNotifyCycleCooldown() : true)
                    .notifyBettingCompleted(request.getNotifyBettingCompleted() != null ? request.getNotifyBettingCompleted() : false)
                    .notifySprintStarted(request.getNotifySprintStarted() != null ? request.getNotifySprintStarted() : false)
                    .build();
        }
        
        SlackChannelConfig saved = channelConfigRepository.save(channelConfig);
        log.info("Created/updated channel configuration: {} for Slack config: {}", saved.getChannelName(), slackConfigId);
        
        return toChannelDTO(saved);
    }

    /**
     * Get all channel configurations for a Slack workspace
     */
    @Transactional(readOnly = true)
    public List<SlackChannelConfigDTO> getChannelConfigs(Long slackConfigId) {
        return channelConfigRepository.findBySlackConfigurationId(slackConfigId).stream()
                .map(this::toChannelDTO)
                .collect(Collectors.toList());
    }

    /**
     * Delete channel configuration
     */
    public void deleteChannelConfig(Long channelConfigId) {
        channelConfigRepository.deleteById(channelConfigId);
        log.info("Deleted channel configuration: {}", channelConfigId);
    }

    /**
     * Send a Slack notification
     */
    public void sendNotification(String notificationType, String message, String channel, String entityType, Long entityId) {
        Optional<SlackConfiguration> configOpt = slackConfigRepository.findFirstByIsEnabledTrue();
        
        if (configOpt.isEmpty()) {
            log.debug("No active Slack configuration found, skipping notification");
            return;
        }
        
        SlackConfiguration config = configOpt.get();
        
        // Determine which webhook URL to use
        String webhookUrl = config.getWebhookUrl();
        String targetChannel = channel != null ? channel : config.getDefaultChannel();
        
        // Check if there's a channel-specific configuration
        if (targetChannel != null) {
            Optional<SlackChannelConfig> channelConfig = channelConfigRepository
                    .findBySlackConfigurationIdAndChannelName(config.getId(), targetChannel);
            
            if (channelConfig.isPresent()) {
                // Check if this notification type is enabled for this channel
                if (!isNotificationEnabled(channelConfig.get(), notificationType)) {
                    log.debug("Notification type {} is disabled for channel {}", notificationType, targetChannel);
                    return;
                }
                
                // Use channel-specific webhook if available
                if (channelConfig.get().getChannelWebhookUrl() != null) {
                    webhookUrl = channelConfig.get().getChannelWebhookUrl();
                }
            }
        }
        
        boolean success = false;
        String errorMessage = null;
        
        try {
            // Build Slack message payload
            Map<String, Object> payload = buildSlackPayload(message, targetChannel);
            
            // Send to Slack
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            restTemplate.postForEntity(webhookUrl, request, String.class);
            success = true;
            log.info("Sent Slack notification: {} to channel: {}", notificationType, targetChannel);
            
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("Failed to send Slack notification: {}", e.getMessage(), e);
        }
        
        // Record in history
        SlackNotificationHistory history = SlackNotificationHistory.builder()
                .slackConfiguration(config)
                .channelName(targetChannel)
                .notificationType(notificationType)
                .messageText(message)
                .entityType(entityType)
                .entityId(entityId)
                .success(success)
                .errorMessage(errorMessage)
                .build();
        
        historyRepository.save(history);
    }

    /**
     * Send a test notification
     */
    public void sendTestNotification(Long configId, TestSlackNotificationRequest request) {
        SlackConfiguration config = slackConfigRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Slack configuration not found with id: " + configId));
        
        String webhookUrl = config.getWebhookUrl();
        String channel = request.getChannel() != null ? request.getChannel() : config.getDefaultChannel();
        String message = request.getMessage() != null ? request.getMessage() : "Test notification from ShapeUp Tracker";
        
        try {
            Map<String, Object> payload = buildSlackPayload(message, channel);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(payload, headers);
            
            restTemplate.postForEntity(webhookUrl, httpRequest, String.class);
            log.info("Sent test notification to Slack workspace: {}", config.getWorkspaceName());
            
        } catch (Exception e) {
            log.error("Failed to send test notification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send test notification: " + e.getMessage());
        }
    }

    /**
     * Get notification history
     */
    @Transactional(readOnly = true)
    public List<SlackNotificationHistory> getNotificationHistory(Long configId) {
        return historyRepository.findBySlackConfigurationIdOrderBySentAtDesc(configId);
    }

    // Helper methods
    
    private void updateChannelConfig(SlackChannelConfig config, CreateSlackChannelConfigRequest request) {
        if (request.getChannelWebhookUrl() != null) {
            config.setChannelWebhookUrl(request.getChannelWebhookUrl());
        }
        if (request.getNotifyTaskAssigned() != null) {
            config.setNotifyTaskAssigned(request.getNotifyTaskAssigned());
        }
        if (request.getNotifyTaskCompleted() != null) {
            config.setNotifyTaskCompleted(request.getNotifyTaskCompleted());
        }
        if (request.getNotifyTaskBlocked() != null) {
            config.setNotifyTaskBlocked(request.getNotifyTaskBlocked());
        }
        if (request.getNotifyPitchShaped() != null) {
            config.setNotifyPitchShaped(request.getNotifyPitchShaped());
        }
        if (request.getNotifyCycleStarted() != null) {
            config.setNotifyCycleStarted(request.getNotifyCycleStarted());
        }
        if (request.getNotifyCycleCooldown() != null) {
            config.setNotifyCycleCooldown(request.getNotifyCycleCooldown());
        }
        if (request.getNotifyBettingCompleted() != null) {
            config.setNotifyBettingCompleted(request.getNotifyBettingCompleted());
        }
        if (request.getNotifySprintStarted() != null) {
            config.setNotifySprintStarted(request.getNotifySprintStarted());
        }
    }

    private boolean isNotificationEnabled(SlackChannelConfig channelConfig, String notificationType) {
        return switch (notificationType) {
            case "TASK_ASSIGNED" -> channelConfig.getNotifyTaskAssigned();
            case "TASK_COMPLETED" -> channelConfig.getNotifyTaskCompleted();
            case "TASK_BLOCKED" -> channelConfig.getNotifyTaskBlocked();
            case "PITCH_SHAPED" -> channelConfig.getNotifyPitchShaped();
            case "CYCLE_STARTED" -> channelConfig.getNotifyCycleStarted();
            case "CYCLE_COOLDOWN" -> channelConfig.getNotifyCycleCooldown();
            case "BETTING_COMPLETED" -> channelConfig.getNotifyBettingCompleted();
            case "SPRINT_STARTED" -> channelConfig.getNotifySprintStarted();
            default -> true;
        };
    }

    private Map<String, Object> buildSlackPayload(String message, String channel) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("text", message);
        
        if (channel != null && !channel.isEmpty()) {
            payload.put("channel", channel.startsWith("#") ? channel : "#" + channel);
        }
        
        return payload;
    }

    private SlackConfigurationDTO toDTO(SlackConfiguration config) {
        return SlackConfigurationDTO.builder()
                .id(config.getId())
                .workspaceName(config.getWorkspaceName())
                .webhookUrl(config.getWebhookUrl())
                .defaultChannel(config.getDefaultChannel())
                .isEnabled(config.getIsEnabled())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private SlackChannelConfigDTO toChannelDTO(SlackChannelConfig config) {
        return SlackChannelConfigDTO.builder()
                .id(config.getId())
                .slackConfigId(config.getSlackConfiguration().getId())
                .channelName(config.getChannelName())
                .channelWebhookUrl(config.getChannelWebhookUrl())
                .notifyTaskAssigned(config.getNotifyTaskAssigned())
                .notifyTaskCompleted(config.getNotifyTaskCompleted())
                .notifyTaskBlocked(config.getNotifyTaskBlocked())
                .notifyPitchShaped(config.getNotifyPitchShaped())
                .notifyCycleStarted(config.getNotifyCycleStarted())
                .notifyCycleCooldown(config.getNotifyCycleCooldown())
                .notifyBettingCompleted(config.getNotifyBettingCompleted())
                .notifySprintStarted(config.getNotifySprintStarted())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
