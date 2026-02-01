package com.github.farzadsedaghatbin.shipflow.service.teams;

import com.github.farzadsedaghatbin.shipflow.dto.teams.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.FlowType;
import com.github.farzadsedaghatbin.shipflow.entity.teams.*;
import com.github.farzadsedaghatbin.shipflow.repository.teams.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;

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
    @Qualifier("webhookRestTemplate")
    private final RestTemplate webhookRestTemplate;

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
        FlowType flowType = FlowType.WEBHOOK; // Default to traditional webhook
        
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
                
                // Use channel-specific webhook and flow type if available
                if (channelConfig.get().getChannelWebhookUrl() != null) {
                    webhookUrl = channelConfig.get().getChannelWebhookUrl();
                }
                if (channelConfig.get().getFlowType() != null) {
                    flowType = channelConfig.get().getFlowType();
                }
            }
        }
        
        boolean success = false;
        String errorMessage = null;
        
        try {
            // Build appropriate payload based on flow type
            Map<String, Object> payload = buildWebhookPayload(webhookUrl, message, notificationType, entityType, entityId, flowType);
            
            // Send to Teams
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "ShipFlow-Teams-Integration/1.0");
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            // Use dedicated webhook RestTemplate with longer timeouts
            var response = webhookRestTemplate.postForEntity(webhookUrl, request, String.class);
            log.info("✅ Sent {} notification to Teams: {} (HTTP {})", notificationType, 
                    targetChannel != null ? targetChannel : "default", response.getStatusCode());
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
        
        // Validate webhook URL (this will throw exception if invalid)
        validateWebhookUrl(webhookUrl);
        
        String message = request.getMessage() != null ? request.getMessage() : "🚀 Test notification from ShipFlow - Your Teams integration is working!";

        try {
            // Default to Power Automate for URLs that look like Power Automate, otherwise webhook
            FlowType flowType = FlowType.WEBHOOK;
            if (webhookUrl.contains("powerplatform.com") || 
                webhookUrl.contains("powerautomate") || 
                webhookUrl.contains("logic.azure.com")) {
                flowType = FlowType.POWER_AUTOMATE_POST;
            }
            
            Map<String, Object> payload = buildWebhookPayload(webhookUrl, message, "TEST", null, null, flowType);
            
            log.debug("Sending Teams webhook payload: {}", payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "ShipFlow-Teams-Integration/1.0");
            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(payload, headers);

            // Use dedicated webhook RestTemplate with longer timeouts
            var response = webhookRestTemplate.postForEntity(webhookUrl, httpRequest, String.class);
            
            log.info("✅ Test notification sent successfully to Teams tenant: {} (HTTP {})", 
                    config.getTenantName(), response.getStatusCode());
                    
        } catch (ResourceAccessException e) {
            // Network/timeout issues
            log.error("❌ Network error sending Teams notification - this usually indicates connection timeout, DNS issues, or Teams service unavailability: {}", e.getMessage());
            throw new RuntimeException("Connection failed to Teams webhook. Please check: 1) Internet connectivity, 2) Webhook URL is correct, 3) Teams service is available. Error: " + e.getMessage());
        } catch (RestClientException e) {
            // HTTP errors (4xx, 5xx)
            log.error("❌ Teams webhook request failed: {}", e.getMessage(), e);
            
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            
            // Check for Logic App URL mistakenly used instead of Teams webhook
            if (errorMsg.contains("triggers/manual/paths") || errorMsg.contains("authorizationfailed")) {
                throw new RuntimeException("❌ WRONG URL TYPE: You're using an Azure Logic App URL instead of a Microsoft Teams incoming webhook URL. " +
                        "Please create a proper Teams incoming webhook: 1) Go to your Teams channel → More options (...) → Connectors → Incoming Webhook → Configure → Create. " +
                        "Teams webhook URLs look like 'https://outlook.office.com/webhook/...' not Logic App URLs.");
            } else if (errorMsg.contains("400")) {
                throw new RuntimeException("Teams rejected the webhook payload (400 Bad Request). The webhook URL might be expired or the message format is invalid.");
            } else if (errorMsg.contains("404")) {
                throw new RuntimeException("Teams webhook not found (404). Please check if the webhook URL is correct and still active.");
            } else if (errorMsg.contains("401")) {
                throw new RuntimeException("Authentication failed (401). The webhook URL might be expired or invalid. Please recreate your Teams incoming webhook.");
            } else {
                throw new RuntimeException("Teams webhook failed: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("❌ Unexpected error sending Teams notification: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error sending Teams notification: " + e.getMessage());
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
        if (request.getFlowType() != null) {
            config.setFlowType(request.getFlowType());
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
     * Build appropriate webhook payload based on the flow type
     */
    private Map<String, Object> buildWebhookPayload(String webhookUrl, String message, String notificationType, String entityType, Long entityId, FlowType flowType) {
        return switch (flowType) {
            case POWER_AUTOMATE_POST, POWER_AUTOMATE_THREAD -> {
                // Power Automate flows expect Adaptive Card format
                yield buildPowerAutomatePayload(message, notificationType, entityType, entityId);
            }
            case WEBHOOK -> {
                // Traditional Teams webhook - let's also use Adaptive Card format for consistency
                yield buildTeamsAdaptiveCard(message, notificationType, entityType, entityId);
            }
        };
    }
    
    /**
     * Build payload for Power Automate flows - Adaptive Card format
     */
    private Map<String, Object> buildPowerAutomatePayload(String message, String notificationType, String entityType, Long entityId) {
        // Build Adaptive Card directly (Power Automate expects this format)
        Map<String, Object> adaptiveCard = new LinkedHashMap<>();
        adaptiveCard.put("type", "AdaptiveCard");
        adaptiveCard.put("version", "1.3");
        
        // Create card body
        List<Map<String, Object>> body = new ArrayList<>();
        
        // Title block
        Map<String, Object> titleBlock = new LinkedHashMap<>();
        titleBlock.put("type", "TextBlock");
        titleBlock.put("text", getNotificationTitle(notificationType));
        titleBlock.put("weight", "Bolder");
        titleBlock.put("size", "Medium");
        titleBlock.put("color", "Accent");
        body.add(titleBlock);
        
        // Message block
        Map<String, Object> messageBlock = new LinkedHashMap<>();
        messageBlock.put("type", "TextBlock");
        messageBlock.put("text", message);
        messageBlock.put("wrap", true);
        messageBlock.put("spacing", "Medium");
        body.add(messageBlock);
        
        // Add entity information if available
        if (entityType != null && entityId != null) {
            Map<String, Object> entityBlock = new LinkedHashMap<>();
            entityBlock.put("type", "TextBlock");
            entityBlock.put("text", String.format("**Entity:** %s #%d", entityType, entityId));
            entityBlock.put("size", "Small");
            entityBlock.put("color", "Attention");
            entityBlock.put("spacing", "Small");
            body.add(entityBlock);
        }
        
        // Timestamp block
        Map<String, Object> timestampBlock = new LinkedHashMap<>();
        timestampBlock.put("type", "TextBlock");
        timestampBlock.put("text", String.format("🕐 %s • 🚀 ShipFlow", LocalDateTime.now().toString()));
        timestampBlock.put("size", "Small");
        timestampBlock.put("color", "Dark");
        timestampBlock.put("isSubtle", true);
        timestampBlock.put("spacing", "Small");
        body.add(timestampBlock);
        
        adaptiveCard.put("body", body);
        
        return adaptiveCard;
    }

    /**
     * Build Microsoft Teams Adaptive Card payload
     * Uses the MessageCard format for incoming webhooks
     */
    private Map<String, Object> buildTeamsAdaptiveCard(String message, String notificationType, String entityType, Long entityId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        
        // Use MessageCard format for Teams incoming webhooks
        payload.put("@type", "MessageCard");
        // Note: http://schema.org/extensions is the official Microsoft Teams schema URL
        // It must be HTTP (not HTTPS) as per Microsoft's MessageCard specification
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
            case "TASK_COMPLETED", "CYCLE_COOLDOWN", "CYCLE_STARTED" -> "00FF00"; // Green
            case "TASK_BLOCKED" -> "FF0000"; // Red
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
                .flowType(config.getFlowType())
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
    
    /**
     * Validate Teams webhook URL format and provide helpful error messages
     */
    private void validateWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Webhook URL cannot be empty");
        }
        
        if (!webhookUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Teams webhook URL must use HTTPS protocol");
        }
        
        // Check if this is a Power Automate URL
        boolean isPowerAutomateUrl = webhookUrl.contains("powerplatform.com") ||
                                     webhookUrl.contains("powerautomate/automations") ||
                                     webhookUrl.contains("logic.azure.com") ||
                                     webhookUrl.contains("/triggers/manual/paths/");
        
        // Check for traditional Teams webhook URL patterns  
        boolean isTraditionalTeamsUrl = webhookUrl.contains("outlook.office.com/webhook") ||
                                        webhookUrl.contains("outlook.office365.com/webhook") ||
                                        webhookUrl.contains(".webhook.office.com");
        
        if (!isPowerAutomateUrl && !isTraditionalTeamsUrl) {
            log.warn("⚠️  Webhook URL does not match expected Teams webhook or Power Automate patterns. Please verify it's correct: {}", 
                    webhookUrl.substring(0, Math.min(60, webhookUrl.length())) + "...");
            log.info("💡 Supported URLs: Teams webhooks (https://outlook.office.com/webhook/...) or Power Automate flows (https://...powerplatform.com/...)");
        }
        
        if (isPowerAutomateUrl) {
            log.info("🔄 Detected Power Automate URL - make sure your flow is configured to 'Accept from anyone' for anonymous access");
        }
    }
}
