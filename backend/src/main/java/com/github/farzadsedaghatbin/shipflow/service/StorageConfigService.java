package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.farzadsedaghatbin.shipflow.dto.storage.ConnectionTestResponse;
import com.github.farzadsedaghatbin.shipflow.dto.storage.StorageConfigDTO;
import com.github.farzadsedaghatbin.shipflow.dto.storage.UpdateStorageConfigRequest;
import com.github.farzadsedaghatbin.shipflow.entity.StorageConfig;
import com.github.farzadsedaghatbin.shipflow.repository.StorageConfigRepository;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.ConnectionStatus;
import com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageRegistry;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Manages the singleton {@link StorageConfig} row: fetches the active config (creating a default
 * if none exists), validates and updates the active provider, and delegates connection tests to the
 * registered {@link com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageProvider}.
 *
 * <p>Secret-merge and DTO-projection live here so controllers never reference the entity directly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageConfigService {

  private final StorageConfigRepository repo;
  private final ObjectStorageRegistry registry;
  private final ObjectMapper objectMapper;

  /**
   * Returns the active {@link StorageConfig} row. If no row exists, a default LOCAL_FS config is
   * persisted and returned.
   */
  public StorageConfig getActiveConfig() {
    return repo.findFirstByDeletedAtIsNullOrderByIdAsc()
        .orElseGet(
            () -> {
              OffsetDateTime now = OffsetDateTime.now();
              StorageConfig defaults =
                  StorageConfig.builder()
                      .activeProvider(StorageProviderType.LOCAL_FS)
                      .config("{}")
                      .createdAt(now)
                      .updatedAt(now)
                      .build();
              log.info("No storage config found — persisting default LOCAL_FS config");
              return repo.save(defaults);
            });
  }

  /**
   * Returns the active config projected as a secret-safe DTO. Secrets (accessKey, secretKey) are
   * never returned; only boolean presence flags are set.
   */
  public StorageConfigDTO getActiveConfigDto() {
    return buildDto(getActiveConfig());
  }

  /**
   * Merges blank/absent secrets from the stored config, validates via the provider, saves, and
   * returns the secret-safe DTO.
   *
   * @param req incoming update request (may have blank/absent accessKey or secretKey)
   * @return secret-safe {@link StorageConfigDTO}
   */
  public StorageConfigDTO updateFromRequest(UpdateStorageConfigRequest req) {
    StorageConfig current = getActiveConfig();
    ObjectNode merged = mergeSecrets(req.getConfig(), current.getConfig());
    StorageProviderType provider =
        req.getActiveProvider() != null ? req.getActiveProvider() : current.getActiveProvider();
    StorageConfig saved = updateConfig(provider, merged);
    return buildDto(saved);
  }

  /**
   * Merges blank/absent secrets from the stored config and delegates a connectivity test to the
   * provider — nothing is persisted.
   *
   * @param req request containing provider and config (may have blank/absent secrets)
   * @return {@link ConnectionTestResponse} indicating success or failure
   */
  public ConnectionTestResponse testFromRequest(UpdateStorageConfigRequest req) {
    StorageConfig current = getActiveConfig();
    ObjectNode merged = mergeSecrets(req.getConfig(), current.getConfig());
    StorageProviderType provider =
        req.getActiveProvider() != null ? req.getActiveProvider() : current.getActiveProvider();
    ConnectionStatus status = testConnection(provider, merged);
    return ConnectionTestResponse.builder().ok(status.isOk()).message(status.getMessage()).build();
  }

  /**
   * Validates {@code config} against the provider, then upserts (updates the single active row or
   * creates one if absent).
   *
   * @param provider the target storage backend
   * @param config provider-specific configuration node
   * @return the saved {@link StorageConfig}
   */
  public StorageConfig updateConfig(StorageProviderType provider, JsonNode config) {
    registry.get(provider).validateConfig(config);

    StorageConfig current = getActiveConfig();
    current.setActiveProvider(provider);
    current.setConfig(config.toString());
    return repo.save(current);
  }

  /**
   * Delegates a connectivity test to the registered provider without persisting anything.
   *
   * @param provider the storage backend to test
   * @param config provider-specific configuration to test against
   * @return a {@link ConnectionStatus} indicating success or failure
   */
  public ConnectionStatus testConnection(StorageProviderType provider, JsonNode config) {
    return registry.get(provider).testConnection(config);
  }

  // ── DTO helpers ───────────────────────────────────────────────────────────

  /** Builds a secret-safe DTO from a StorageConfig — secrets projected to boolean presence only. */
  public StorageConfigDTO buildDto(StorageConfig cfg) {
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

  // ── Secret-merge helpers ──────────────────────────────────────────────────

  /**
   * Merges submitted config with stored config: blank/absent accessKey and secretKey in the request
   * are replaced with the values stored in the DB so admins don't need to re-enter secrets on every
   * save. Non-blank submitted values override the stored ones.
   *
   * <p>Secret values are NEVER logged.
   */
  public ObjectNode mergeSecrets(ObjectNode submitted, String storedJson) {
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

  // ── JSON node utilities ───────────────────────────────────────────────────

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
