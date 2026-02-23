package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.inbound.CreateInboundWebhookConfigRequest;
import com.github.farzadsedaghatbin.shipflow.dto.inbound.InboundWebhookConfigDTO;
import com.github.farzadsedaghatbin.shipflow.service.inbound.InboundWebhookConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin CRUD endpoints for managing inbound webhook provider configurations.
 *
 * @since 0.6.0
 */
@RestController
@RequestMapping("/api/inbound-webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inbound Webhook Config",
    description = "Admin APIs for managing inbound webhook provider configurations")
public class InboundWebhookConfigController {

    private final InboundWebhookConfigService configService;

    @PostMapping("/configurations")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create or update an inbound webhook provider configuration")
    public ResponseEntity<InboundWebhookConfigDTO> createConfiguration(
        @Valid @RequestBody CreateInboundWebhookConfigRequest request) {
        InboundWebhookConfigDTO config = configService.createOrUpdate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(config);
    }

    @GetMapping("/configurations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all inbound webhook configurations")
    public ResponseEntity<List<InboundWebhookConfigDTO>> getAllConfigurations() {
        return ResponseEntity.ok(configService.getAll());
    }

    @GetMapping("/configurations/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get an inbound webhook configuration by ID")
    public ResponseEntity<InboundWebhookConfigDTO> getConfiguration(@PathVariable Long id) {
        return configService.getById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/configurations/enabled")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all enabled inbound webhook configurations")
    public ResponseEntity<List<InboundWebhookConfigDTO>> getEnabledConfigurations() {
        return ResponseEntity.ok(configService.getEnabled());
    }

    @PatchMapping("/configurations/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Toggle enabled/disabled for an inbound webhook configuration")
    public ResponseEntity<InboundWebhookConfigDTO> toggleConfiguration(@PathVariable Long id) {
        return ResponseEntity.ok(configService.toggleEnabled(id));
    }

    @DeleteMapping("/configurations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an inbound webhook configuration")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable Long id) {
        configService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
