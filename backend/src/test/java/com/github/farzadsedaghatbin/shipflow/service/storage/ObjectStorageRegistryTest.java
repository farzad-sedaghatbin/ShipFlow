package com.github.farzadsedaghatbin.shipflow.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.ConnectionStatus;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.InvalidConfigException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ObjectStorageRegistry} — no Spring context, pure unit.
 */
class ObjectStorageRegistryTest {

  /** Fake LOCAL_FS provider for testing. */
  static class FakeLocalFsProvider implements ObjectStorageProvider {

    @Override
    public StorageProviderType getType() {
      return StorageProviderType.LOCAL_FS;
    }

    @Override
    public void validateConfig(JsonNode config) throws InvalidConfigException {
      // no-op for tests
    }

    @Override
    public StoredObjectRef store(StorePutContext ctx) {
      return StoredObjectRef.builder()
          .bucket("local")
          .key(ctx.getKey())
          .contentType(ctx.getContentType())
          .sizeBytes(0L)
          .build();
    }

    @Override
    public DownloadResource retrieve(String bucket, String key, JsonNode config) {
      return DownloadResource.builder()
          .stream(InputStream.nullInputStream())
          .contentType("application/octet-stream")
          .sizeBytes(0L)
          .filename(key)
          .build();
    }

    @Override
    public void delete(String bucket, String key, JsonNode config) {
      // no-op
    }
  }

  @Test
  void get_returnsRegisteredProvider() {
    ObjectStorageRegistry registry = new ObjectStorageRegistry(List.of(new FakeLocalFsProvider()));
    ObjectStorageProvider provider = registry.get(StorageProviderType.LOCAL_FS);
    assertThat(provider).isInstanceOf(FakeLocalFsProvider.class);
    assertThat(provider.getType()).isEqualTo(StorageProviderType.LOCAL_FS);
  }

  @Test
  void get_throwsIllegalStateExceptionForUnregisteredType() {
    ObjectStorageRegistry registry = new ObjectStorageRegistry(List.of(new FakeLocalFsProvider()));
    assertThatThrownBy(() -> registry.get(StorageProviderType.S3))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("S3");
  }

  @Test
  void isAvailable_trueForRegisteredType() {
    ObjectStorageRegistry registry = new ObjectStorageRegistry(List.of(new FakeLocalFsProvider()));
    assertThat(registry.isAvailable(StorageProviderType.LOCAL_FS)).isTrue();
  }

  @Test
  void isAvailable_falseForUnregisteredType() {
    ObjectStorageRegistry registry = new ObjectStorageRegistry(List.of(new FakeLocalFsProvider()));
    assertThat(registry.isAvailable(StorageProviderType.MINIO)).isFalse();
  }

  @Test
  void defaultTestConnection_returnsOk() {
    FakeLocalFsProvider provider = new FakeLocalFsProvider();
    ConnectionStatus status = provider.testConnection(null);
    assertThat(status.isOk()).isTrue();
  }

  @Test
  void defaultPresignUrl_returnsEmpty() {
    FakeLocalFsProvider provider = new FakeLocalFsProvider();
    assertThat(provider.presignUrl("bucket", "key", 3600, null)).isEmpty();
  }

  @Test
  void defaultSupportsPresign_returnsFalse() {
    FakeLocalFsProvider provider = new FakeLocalFsProvider();
    assertThat(provider.supportsPresign()).isFalse();
  }
}
