package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.farzadsedaghatbin.shipflow.dto.storage.ConnectionTestResponse;
import com.github.farzadsedaghatbin.shipflow.dto.storage.MigrationResultDTO;
import com.github.farzadsedaghatbin.shipflow.dto.storage.StorageConfigDTO;
import com.github.farzadsedaghatbin.shipflow.dto.storage.UpdateStorageConfigRequest;
import com.github.farzadsedaghatbin.shipflow.entity.StorageConfig;
import com.github.farzadsedaghatbin.shipflow.service.StorageConfigService;
import com.github.farzadsedaghatbin.shipflow.service.StorageMigrationService;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.ConnectionStatus;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
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
  private final ObjectMapper objectMapper;

  @GetMapping
  @Operation(summary = "Get current storage configuration (secrets projected to booleans)")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<StorageConfigDTO> getConfig() {
    log.info("Fetching storage config");
    StorageConfig cfg = storageConfigService.getActiveConfig();
    return ResponseEntity.ok(buildDto(cfg));
  }

  @PutMapping
  @Operation(summary = "Update storage configuration (blank secrets are preserved from stored config)")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<StorageConfigDTO> updateConfig(@RequestBody UpdateStorageConfigRequest req) {
    log.info("Updating storage config, provider={}", req.getActiveProvider());
    ObjectNode merged = mergeSecrets(req.getConfig(), storageConfigService.getActiveConfig().getConfig());
    StorageProviderType provider =
        req.getActiveProvider() != null
            ? req.getActiveProvider()
            : storageConfigService.getActiveConfig().getActiveProvider();
    StorageConfig saved = storageConfigService.updateConfig(provider, merged);
    return ResponseEntity.ok(buildDto(saved));
  }

  @PostMapping("/test")
  @Operation(summary = "Test a storage configuration without persisting it")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ConnectionTestResponse> testConnection(
      @RequestBody UpdateStorageConfigRequest req) {
    log.info("Testing storage connection, provider={}", req.getActiveProvider());
    ObjectNode merged = mergeSecrets(req.getConfig(), storageConfigService.getActiveConfig().getConfig());
    StorageProviderType provider =
        req.getActiveProvider() != null
            ? req.getActiveProvider()
            : storageConfigService.getActiveConfig().getActiveProvider();
    ConnectionStatus status = storageConfigService.testConnection(provider, merged);
    return ResponseEntity.ok(
        ConnectionTestResponse.builder().ok(status.isOk()).message(status.getMessage()).build());
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

  // ── Private helpers ───────────────────────────────────────────────────────

  /** Builds a DTO from a StorageConfig — secrets projected to boolean presence only. */
  private StorageConfigDTO buildDto(StorageConfig cfg) {
    JsonNode node;
    try {
      node = objectMapper.readTree(cfg.getConfig() != null ? cfg.getConfig() : "{}");
    } catch (Exception e) {
      node = objectMapper.createObjectNode();
    }

    return StorageConfigDTO.builder()
        .activeProvider(cfg.getActiveProvider())
        .bucket(textOrNull(node, "bucket"))
        .endpoint(textOrNull(node, "endpoint"))
        .region(textOrNull(node, "region"))
        .pathStyleAccess(boolOrNull(node, "pathStyleAccess"))
        .hasAccessKey(hasNonBlank(node, "accessKey"))
        .hasSecretKey(hasNonBlank(node, "secretKey"))
        .build();
  }

  /**
   * Merges submitted config with stored config: blank/absent accessKey and secretKey in the request
   * are replaced with the values stored in the DB so admins don't need to re-enter secrets on every
   * save.
   */
  private ObjectNode mergeSecrets(ObjectNode submitted, String storedJson) {
    ObjectNode result = submitted != null ? submitted.deepCopy() : objectMapper.createObjectNode();
    JsonNode stored;
    try {
      stored = objectMapper.readTree(storedJson != null ? storedJson : "{}");
    } catch (Exception e) {
      stored = objectMapper.createObjectNode();
    }
    preserveIfBlank(result, stored, "accessKey");
    preserveIfBlank(result, stored, "secretKey");
    return result;
  }

  private void preserveIfBlank(ObjectNode target, JsonNode source, String field) {
    String submitted = target.has(field) ? target.get(field).asText("") : "";
    if (submitted.isBlank() && source.has(field)) {
      target.set(field, source.get(field));
    }
  }

  private static String textOrNull(JsonNode node, String field) {
    return node.has(field) ? node.get(field).asText(null) : null;
  }

  private static Boolean boolOrNull(JsonNode node, String field) {
    return node.has(field) ? node.get(field).asBoolean() : null;
  }

  private static boolean hasNonBlank(JsonNode node, String field) {
    return node.has(field) && !node.get(field).asText("").isBlank();
  }
}
