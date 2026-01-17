package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.slack.*;
import com.github.farzadsedaghatbin.shipflow.entity.slack.SlackNotificationHistory;
import com.github.farzadsedaghatbin.shipflow.service.slack.SlackIntegrationService;
import com.github.farzadsedaghatbin.shipflow.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/slack")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Slack Integration", description = "APIs for managing Slack integration and notifications")
public class SlackIntegrationController {

    private final SlackIntegrationService slackService;
    private final MessageService messageService;

    @PostMapping("/configurations")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create or update Slack workspace configuration",
               description = "Configure Slack workspace integration with webhook URL and default channel")
    public ResponseEntity<SlackConfigurationDTO> createConfiguration(
            @Valid @RequestBody CreateSlackConfigurationRequest request) {
        SlackConfigurationDTO config = slackService.createOrUpdateConfiguration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(config);
    }

    @GetMapping("/configurations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all Slack configurations")
    public ResponseEntity<List<SlackConfigurationDTO>> getAllConfigurations() {
        return ResponseEntity.ok(slackService.getAllConfigurations());
    }

    @GetMapping("/configurations/active")
    @Operation(summary = "Get active Slack configuration")
    public ResponseEntity<SlackConfigurationDTO> getActiveConfiguration() {
        Optional<SlackConfigurationDTO> config = slackService.getActiveConfiguration();
        return config.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/configurations/{configId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Slack configuration")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable Long configId) {
        slackService.deleteConfiguration(configId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/configurations/{configId}/channels")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create or update channel configuration",
               description = "Configure notification preferences for a specific Slack channel")
    public ResponseEntity<SlackChannelConfigDTO> createChannelConfig(
            @PathVariable Long configId,
            @Valid @RequestBody CreateSlackChannelConfigRequest request) {
        SlackChannelConfigDTO channelConfig = slackService.createOrUpdateChannelConfig(configId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(channelConfig);
    }

    @GetMapping("/configurations/{configId}/channels")
    @Operation(summary = "Get all channel configurations for a Slack workspace")
    public ResponseEntity<List<SlackChannelConfigDTO>> getChannelConfigs(@PathVariable Long configId) {
        return ResponseEntity.ok(slackService.getChannelConfigs(configId));
    }

    @DeleteMapping("/channels/{channelConfigId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete channel configuration")
    public ResponseEntity<Void> deleteChannelConfig(@PathVariable Long channelConfigId) {
        slackService.deleteChannelConfig(channelConfigId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/configurations/{configId}/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Send a test notification to Slack",
               description = "Test the Slack integration by sending a test message")
    public ResponseEntity<String> sendTestNotification(
            @PathVariable Long configId,
            @RequestBody TestSlackNotificationRequest request) {
        try {
            slackService.sendTestNotification(configId, request);
            return ResponseEntity.ok(messageService.getMessage("slack.notification.sent"));
        } catch (Exception e) {
            log.error("Failed to send test notification for config {}: {}", configId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(messageService.getMessage("slack.test.failed"));
        }
    }

    @GetMapping("/configurations/{configId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get notification history",
               description = "Retrieve the history of Slack notifications sent")
    public ResponseEntity<List<SlackNotificationHistory>> getNotificationHistory(@PathVariable Long configId) {
        return ResponseEntity.ok(slackService.getNotificationHistory(configId));
    }
}
