package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.admin.UpdateOrganizationSettingsRequest;
import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for organization settings management.
 * Only accessible by ADMIN role.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organization Settings", description = "Manage organization-wide settings")
public class OrganizationSettingsController {

    private final OrganizationSettingsService settingsService;

    @GetMapping("/settings")
    @Operation(summary = "Get organization settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrganizationSettingsDTO> getSettings() {
        log.info("Fetching organization settings");
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping("/settings")
    @Operation(summary = "Update organization settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrganizationSettingsDTO> updateSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateOrganizationSettingsRequest request) {
        log.info("Updating organization settings by user: {}", userDetails.getUsername());
        return ResponseEntity.ok(settingsService.updateSettings(request, userDetails.getUsername()));
    }

    @PostMapping("/settings/reset")
    @Operation(summary = "Reset organization settings to defaults")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrganizationSettingsDTO> resetSettings(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Resetting organization settings to defaults by user: {}", userDetails.getUsername());
        return ResponseEntity.ok(settingsService.resetToDefaults(userDetails.getUsername()));
    }
}
