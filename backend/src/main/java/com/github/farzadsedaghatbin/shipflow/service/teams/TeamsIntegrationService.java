package com.github.farzadsedaghatbin.shipflow.service.teams;

import com.github.farzadsedaghatbin.shipflow.dto.teams.*;
import com.github.farzadsedaghatbin.shipflow.entity.teams.*;
import com.github.farzadsedaghatbin.shipflow.repository.teams.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing Microsoft Teams integration.
 * Handles configuration, sending notifications, and tracking history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TeamsIntegrationService {

    private final TeamsConfigurationRepository teamsConfigRepository;
    private final TeamsChannelConfigRepository channelConfigRepository;
    private final TeamsNotificationHistoryRepository historyRepository;
    private final RestTemplate restTemplate;

    /**
     * Create or update Teams tenant configuration
     */
    public TeamsConfigurationDTO createOrUpdateConfiguration(CreateTeamsConfigurationRequest request) {
        Optional<TeamsConfiguration> existing = teamsConfigRepository.findByTenantName(request.getTenantName());
        
        TeamsConfiguration config;
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
            config = TeamsConfiguration.builder()
                    .tenantName(request.getTenantName())
                    .webhookUrl(request.getWebhookUrl())
                    .defaultChannel(request.getDefaultChannel())
                    .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true)
                    .build();
        }
        
        TeamsConfiguration saved = teamsConfigRepository.save(config);
        log.info("Created/updated Teams configuration for tenant: {}", saved.getTenantName());
        
        return toDTO(saved);
    }

    /**
     * Get all Teams configurations
     */
    @Transactional(readOnly = true)
    public List<TeamsConfigurationDTO> getAllConfigurations() {
        return teamsConfigRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get active Teams configuration
     */
    @Transactional(readOnly = true)
    public Optional<TeamsConfigurationDTO> getActiveConfiguration() {
        return teamsConfigRepository.findFirstByIsEnabledTrue()
                .map(this::toDTO);
    }

    /**
     * Delete Teams configuration
     */
    public void deleteConfiguration(Long configId) {
        teamsConfigRepository.deleteById(configId);
        log.info("Deleted Teams configuration: {}", configId);
    }

    /**
     * Create or update channel configuration
     */
    public TeamsChannelConfigDTO createOrUpdateChannelConfig(Long teamsConfigId, CreateTeamsChannelConfigRequest request) {
        TeamsConfiguration teamsConfig = teamsConfigRepository.findById(teamsConfigId)
                .orElseThrow(() -> new RuntimeException("Teams configuration not found with id: " + teamsConfigId));
        
        Optional<TeamsChannelConfig> existing = channelConfigRepository
                .findByTeamsConfigurationIdAndChannelName(teamsConfigId, request.getChannelName());
        
        TeamsChannelConfig channelConfig;
        if (existing.isPresent()) {
            channelConfig = existing.get();
            updateChannelConfig(channelConfig, request);
        } else {
            channelConfig = TeamsChannelConfig.builder()
                    .teamsConfiguration(teamsConfig)
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
        
        TeamsChannelConfig saved = channelConfigRepository.save(channelConfig);
        log.info("Created/updated channel configuration: {} for Teams config: {}", saved.getChannelName(), teamsConfigId);
        
        return toChannelDTO(saved);
    }

    /**
     * Get all channel configurations for a Teams tenant
     */
    @Transactional(readOnly = true)
    public List<TeamsChannelConfigDTO> getChannelConfigs(Long teamsConfigId) {
        return channelConfigRepository.findByTeamsConfigurationId(teamsConfigId).stream()
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
     * Send a Microsoft Teams notification using Adaptive Card format
     */
    public void sendNotification(String notificationType, String message, String channel, String entityType, Long entityId) {
        Optional<TeamsConfiguration> configOpt = teamsConfigRepository.findFirstByIsEnabledTrue();
        
        if (configOpt.isEmpty()) {
            log.debug("No active Teams configuration found, skipping notification");
            return;
        }
        
        TeamsConfiguration config = configOpt.get();
        
        // Determine which webhook URL to use
        String webhookUrl = config.getWebhookUrl();
        String targetChannel = channel != null ? channel : config.getDefaultChannel();
        
        // Check if there's a channel-specific configuration
        if (targetChannel != null) {
            Optional<TeamsChannelConfig> channelConfig = channelConfigRepository
                    .findByTeamsConfigurationIdAndChannelName(config.getId(), targetChannel);
            
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
            // Build Teams Adaptive Card payload
            Map<String, Object> payload = buildTeamsAdaptiveCard(message, notificationType, entityType, entityId);
            
            // Send to Teams
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            restTemplate.postForEntity(webhookUrl, request, String.class);
            success = true;
            log.info("Sent Teams notification: {} to channel: {}", notificationType, targetChannel);
            
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("Failed to send Teams notification: {}", e.getMessage(), e);
        }
        
        // Record in history
        TeamsNotificationHistory history = TeamsNotificationHistory.builder()
                .teamsConfiguration(config)
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
    public void sendTestNotification(Long configId, TestTeamsNotificationRequest request) {
        TeamsConfiguration config = teamsConfigRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Teams configuration not found with id: " + configId));
        String webhookUrl = config.getWebhookUrl();
        String message = request.getMessage() != null ? request.getMessage() : "Test notification from ShipFlow";

        try {
            Map<String, Object> payload = buildTeamsAdaptiveCard(message, "TEST", null, null);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(webhookUrl, httpRequest, String.class);
            log.info("Sent test notification to Teams tenant: {}", config.getTenantName());
        } catch (Exception e) {
            log.error("Failed to send test notification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send test notification: " + e.getMessage());
        }
    }

    /**
     * Get notification history
     */
    @Transactional(readOnly = true)
    public List<TeamsNotificationHistoryDTO> getNotificationHistory(Long configId) {
        return historyRepository.findTop50ByTeamsConfigurationIdOrderBySentAtDesc(configId).stream()
                .map(this::toHistoryDTO)
                .collect(Collectors.toList());
    }

    // Helper methods
    
    private void updateChannelConfig(TeamsChannelConfig config, CreateTeamsChannelConfigRequest request) {
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

    private boolean isNotificationEnabled(TeamsChannelConfig channelConfig, String notificationType) {
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

    /**
     * Build Microsoft Teams Adaptive Card payload
     * Uses the MessageCard format for incoming webhooks
     */
    private Map<String, Object> buildTeamsAdaptiveCard(String message, String notificationType, String entityType, Long entityId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        
        // Use MessageCard format for Teams incoming webhooks
        payload.put("@type", "MessageCard");
        payload.put("@context", "http://schema.org/extensions");
        payload.put("themeColor", getThemeColor(notificationType));
        payload.put("summary", "ShipFlow Notification");
        
        // Title section
        payload.put("title", getNotificationTitle(notificationType));
        
        // Message text
        payload.put("text", message);
        
        // Add sections with facts if entity info is available
        if (entityType != null && entityId != null) {
            List<Map<String, Object>> sections = new ArrayList<>();
            Map<String, Object> section = new LinkedHashMap<>();
            
            List<Map<String, String>> facts = new ArrayList<>();
            facts.add(Map.of("name", "Type", "value", entityType));
            facts.add(Map.of("name", "ID", "value", String.valueOf(entityId)));
            facts.add(Map.of("name", "Time", "value", LocalDateTime.now().toString()));
            
            section.put("facts", facts);
            sections.add(section);
            payload.put("sections", sections);
        }
        
        return payload;
    }

    private String getThemeColor(String notificationType) {
        return switch (notificationType) {
            case "TASK_COMPLETED", "CYCLE_COOLDOWN" -> "00FF00"; // Green
            case "TASK_BLOCKED", "CYCLE_STARTED" -> "FF0000"; // Red
            case "PITCH_SHAPED", "BETTING_COMPLETED" -> "0078D4"; // Blue
            case "TASK_ASSIGNED", "SPRINT_STARTED" -> "FFA500"; // Orange
            case "TEST" -> "9B59B6"; // Purple
            default -> "0078D4"; // Default Teams blue
        };
    }

    private String getNotificationTitle(String notificationType) {
        return switch (notificationType) {
            case "TASK_ASSIGNED" -> "📋 Task Assigned";
            case "TASK_COMPLETED" -> "✅ Task Completed";
            case "TASK_BLOCKED" -> "🚫 Task Blocked";
            case "PITCH_SHAPED" -> "💡 Pitch Shaped";
            case "CYCLE_STARTED" -> "🚀 Cycle Started";
            case "CYCLE_COOLDOWN" -> "❄️ Cycle Cooldown";
            case "BETTING_COMPLETED" -> "🎲 Betting Completed";
            case "SPRINT_STARTED" -> "🏃 Sprint Started";
            case "TEST" -> "🧪 Test Notification";
            default -> "📢 ShipFlow Notification";
        };
    }

    private TeamsConfigurationDTO toDTO(TeamsConfiguration config) {
        return TeamsConfigurationDTO.builder()
                .id(config.getId())
                .tenantName(config.getTenantName())
                .webhookUrl(config.getWebhookUrl())
                .defaultChannel(config.getDefaultChannel())
                .isEnabled(config.getIsEnabled())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private TeamsChannelConfigDTO toChannelDTO(TeamsChannelConfig config) {
        return TeamsChannelConfigDTO.builder()
                .id(config.getId())
                .teamsConfigId(config.getTeamsConfiguration().getId())
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

    private TeamsNotificationHistoryDTO toHistoryDTO(TeamsNotificationHistory history) {
        return TeamsNotificationHistoryDTO.builder()
                .id(history.getId())
                .teamsConfigId(history.getTeamsConfiguration().getId())
                .channelName(history.getChannelName())
                .notificationType(history.getNotificationType())
                .messageText(history.getMessageText())
                .entityType(history.getEntityType())
                .entityId(history.getEntityId())
                .sentAt(history.getSentAt())
                .success(history.getSuccess())
                .errorMessage(history.getErrorMessage())
                .build();
    }
}
