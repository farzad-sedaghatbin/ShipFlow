package com.github.farzadsedaghatbin.shipflow.service.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.ConnectionStatus;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.InvalidConfigException;
import java.util.Optional;

/**
 * SPI for pluggable object-storage backends.
 *
 * <p>Mirrors the {@code KnowledgeSourceProvider} pattern — one Spring bean per backend; the
 * {@link ObjectStorageRegistry} auto-collects all implementations.
 */
public interface ObjectStorageProvider {

  StorageProviderType getType();

  void validateConfig(JsonNode config) throws InvalidConfigException;

  default ConnectionStatus testConnection(JsonNode config) {
    return ConnectionStatus.ok();
  }

  StoredObjectRef store(StorePutContext ctx);

  DownloadResource retrieve(String bucket, String key, JsonNode config);

  void delete(String bucket, String key, JsonNode config);

  /**
   * Returns a pre-signed URL valid for {@code expirySeconds}. Only supported by S3/MinIO backends;
   * LOCAL_FS should return {@link Optional#empty()}.
   */
  default Optional<String> presignUrl(
      String bucket, String key, long expirySeconds, JsonNode config) {
    return Optional.empty();
  }

  default boolean supportsPresign() {
    return false;
  }
}
