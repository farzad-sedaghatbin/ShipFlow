package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.storage.ConnectionTestResponse;
import com.github.farzadsedaghatbin.shipflow.dto.storage.MigrationResultDTO;
import com.github.farzadsedaghatbin.shipflow.dto.storage.StorageConfigDTO;
import com.github.farzadsedaghatbin.shipflow.dto.storage.UpdateStorageConfigRequest;
import com.github.farzadsedaghatbin.shipflow.service.StorageConfigService;
import com.github.farzadsedaghatbin.shipflow.service.StorageMigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/storage")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Storage Config", description = "Manage object storage configuration (admin only)")
public class StorageConfigController {

  private final StorageConfigService storageConfigService;
  private final StorageMigrationService storageMigrationService;

  @GetMapping
  @Operation(summary = "Get current storage configuration (secrets projected to booleans)")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<StorageConfigDTO> getConfig() {
    log.info("Fetching storage config");
    return ResponseEntity.ok(storageConfigService.getActiveConfigDto());
  }

  @PutMapping
  @Operation(summary = "Update storage configuration (blank secrets are preserved from stored config)")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<StorageConfigDTO> updateConfig(@RequestBody UpdateStorageConfigRequest req) {
    log.info("Updating storage config, provider={}", req.getActiveProvider());
    return ResponseEntity.ok(storageConfigService.updateFromRequest(req));
  }

  @PostMapping("/test")
  @Operation(summary = "Test a storage configuration without persisting it")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ConnectionTestResponse> testConnection(
      @RequestBody UpdateStorageConfigRequest req) {
    log.info("Testing storage connection, provider={}", req.getActiveProvider());
    return ResponseEntity.ok(storageConfigService.testFromRequest(req));
  }

  @PostMapping("/migrate")
  @Operation(summary = "Migrate all attachments to the currently active storage backend")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MigrationResultDTO> migrate() {
    log.info("Starting storage migration to active backend");
    MigrationResultDTO result = storageMigrationService.migrateToActiveBackend();
    log.info("Migration complete: {}", result);
    return ResponseEntity.ok(result);
  }
}
