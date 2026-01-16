package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.teams.*;
import com.github.farzadsedaghatbin.shipflow.service.teams.TeamsIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "Microsoft Teams Integration", description = "APIs for managing Microsoft Teams integration and notifications")
public class TeamsIntegrationController {

    private final TeamsIntegrationService teamsService;

    @PostMapping("/configurations")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create or update Teams tenant configuration",
               description = "Configure Microsoft Teams integration with webhook URL and default channel")
    public ResponseEntity<TeamsConfigurationDTO> createConfiguration(
            @Valid @RequestBody CreateTeamsConfigurationRequest request) {
        TeamsConfigurationDTO config = teamsService.createOrUpdateConfiguration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(config);
    }

    @GetMapping("/configurations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all Teams configurations")
    public ResponseEntity<List<TeamsConfigurationDTO>> getAllConfigurations() {
        return ResponseEntity.ok(teamsService.getAllConfigurations());
    }

    @GetMapping("/configurations/active")
    @Operation(summary = "Get active Teams configuration")
    public ResponseEntity<TeamsConfigurationDTO> getActiveConfiguration() {
        Optional<TeamsConfigurationDTO> config = teamsService.getActiveConfiguration();
        return config.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/configurations/{configId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Teams configuration")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable Long configId) {
        teamsService.deleteConfiguration(configId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/configurations/{configId}/channels")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create or update channel configuration",
               description = "Configure notification preferences for a specific Teams channel")
    public ResponseEntity<TeamsChannelConfigDTO> createChannelConfig(
            @PathVariable Long configId,
            @Valid @RequestBody CreateTeamsChannelConfigRequest request) {
        TeamsChannelConfigDTO channelConfig = teamsService.createOrUpdateChannelConfig(configId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(channelConfig);
    }

    @GetMapping("/configurations/{configId}/channels")
    @Operation(summary = "Get all channel configurations for a Teams tenant")
    public ResponseEntity<List<TeamsChannelConfigDTO>> getChannelConfigs(@PathVariable Long configId) {
        return ResponseEntity.ok(teamsService.getChannelConfigs(configId));
    }

    @DeleteMapping("/channels/{channelConfigId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete channel configuration")
    public ResponseEntity<Void> deleteChannelConfig(@PathVariable Long channelConfigId) {
        teamsService.deleteChannelConfig(channelConfigId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/configurations/{configId}/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Send a test notification to Teams",
               description = "Test the Microsoft Teams integration by sending a test message")
    public ResponseEntity<String> sendTestNotification(
            @PathVariable Long configId,
            @RequestBody TestTeamsNotificationRequest request) {
        try {
            teamsService.sendTestNotification(configId, request);
            return ResponseEntity.ok("Test notification sent successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send test notification: " + e.getMessage());
        }
    }

    @GetMapping("/configurations/{configId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get notification history",
               description = "Retrieve the history of Teams notifications sent")
    public ResponseEntity<List<TeamsNotificationHistoryDTO>> getNotificationHistory(@PathVariable Long configId) {
        return ResponseEntity.ok(teamsService.getNotificationHistory(configId));
    }
}
