package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.admin.McpStatusDTO;
import com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.admin.RiskProfilesDTO;
import com.github.farzadsedaghatbin.shipflow.dto.admin.UpdateOrganizationSettingsRequest;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.IEmailNotificationService;
import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import com.github.farzadsedaghatbin.shipflow.service.mcp.McpConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for organization settings management. Only accessible by
 * ADMIN role.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organization Settings", description = "Manage organization-wide settings")
public class OrganizationSettingsController {

  private final OrganizationSettingsService settingsService;
  private final McpConfig mcpConfig;
  private final IEmailNotificationService emailService;
  private final UserRepository userRepository;

  @GetMapping("/settings")
  @Operation(summary = "Get organization settings")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<OrganizationSettingsDTO> getSettings() {
    log.info("Fetching organization settings");
    return ResponseEntity.ok(settingsService.getSettings());
  }

  @GetMapping("/settings/meeting-types")
  @Operation(summary = "Get meeting type configurations (accessible to all authenticated users)")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<java.util.List<com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.MeetingTypeConfig>> getMeetingTypes() {
    return ResponseEntity.ok(settingsService.getSettings().getMeetingTypes());
  }

  @PutMapping("/settings")
  @Operation(summary = "Update organization settings")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<OrganizationSettingsDTO> updateSettings(@AuthenticationPrincipal UserDetails userDetails,
      @RequestBody UpdateOrganizationSettingsRequest request) {
    log.info("Updating organization settings by user: {}", userDetails.getUsername());
    return ResponseEntity.ok(settingsService.updateSettings(request, userDetails.getUsername()));
  }

  @PostMapping("/settings/reset")
  @Operation(summary = "Reset organization settings to defaults")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<OrganizationSettingsDTO> resetSettings(@AuthenticationPrincipal UserDetails userDetails) {
    log.info("Resetting organization settings to defaults by user: {}", userDetails.getUsername());
    return ResponseEntity.ok(settingsService.resetToDefaults(userDetails.getUsername()));
  }

  @GetMapping("/settings/risk-profiles")
  @Operation(summary = "Get predefined risk calculation profiles")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<RiskProfilesDTO> getRiskProfiles() {
    log.info("Fetching predefined risk profiles");
    return ResponseEntity.ok(RiskProfilesDTO.getDefaultProfiles());
  }

  @GetMapping("/settings/mcp-status")
  @Operation(summary = "Get MCP server configuration status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<McpStatusDTO> getMcpStatus() {
    log.info("Fetching MCP server status");
    
    var githubConfig = mcpConfig.getGithub();
    var figmaConfig = mcpConfig.getFigma();
    
    McpStatusDTO status = McpStatusDTO.builder()
        .github(McpStatusDTO.McpServerStatus.builder()
            .enabled(githubConfig.isEnabled())
            .configured(githubConfig.getServerUrl() != null && !githubConfig.getServerUrl().isBlank())
            .serverUrlMasked(McpStatusDTO.maskServerUrl(githubConfig.getServerUrl()))
            .timeoutSeconds(githubConfig.getTimeoutSeconds())
            .build())
        .figma(McpStatusDTO.McpServerStatus.builder()
            .enabled(figmaConfig.isEnabled())
            .configured(figmaConfig.getServerUrl() != null && !figmaConfig.getServerUrl().isBlank())
            .serverUrlMasked(McpStatusDTO.maskServerUrl(figmaConfig.getServerUrl()))
            .timeoutSeconds(figmaConfig.getTimeoutSeconds())
            .build())
        .build();
    
    return ResponseEntity.ok(status);
  }

  @PostMapping("/settings/test-email")
  @Operation(summary = "Send a test email to the currently logged-in admin")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> sendTestEmail(
      @AuthenticationPrincipal UserDetails userDetails) {
    if (!emailService.isEnabled()) {
      return ResponseEntity.ok(Map.of("error", "Email not configured"));
    }
    User user = userRepository.findByUsername(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("User not found"));
    String email = user.getEmail();
    if (email == null || email.isBlank()) {
      return ResponseEntity.ok(Map.of("error", "Current user has no email address configured"));
    }
    String recipientName = user.getPerson() != null && user.getPerson().getName() != null
        ? user.getPerson().getName()
        : user.getUsername();
    emailService.sendTaskAssigned(email, recipientName, "Test Task — ShipFlow Email Working", "/dashboard");
    log.info("Test email sent to {} by admin {}", email, userDetails.getUsername());
    return ResponseEntity.ok(Map.of("message", "Test email sent to " + email));
  }
}
