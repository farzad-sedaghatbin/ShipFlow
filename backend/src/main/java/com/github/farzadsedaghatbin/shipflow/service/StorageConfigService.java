package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
}
