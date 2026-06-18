package com.github.farzadsedaghatbin.shipflow.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.service.storage.provider.LocalFsStorageProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pure-unit tests for {@link LocalFsStorageProvider}. No Spring context — uses {@code @TempDir}.
 */
class LocalFsStorageProviderTest {

  @TempDir Path tempDir;

  private LocalFsStorageProvider provider;
  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    provider = new LocalFsStorageProvider(tempDir.toString());
  }

  // ── getType ───────────────────────────────────────────────────────────────

  @Test
  void getType_returnsLocalFs() {
    assertThat(provider.getType()).isEqualTo(StorageProviderType.LOCAL_FS);
  }

  // ── validateConfig ────────────────────────────────────────────────────────

  @Test
  void validateConfig_acceptsNull() {
    assertThatNoException().isThrownBy(() -> provider.validateConfig(null));
  }

  @Test
  void validateConfig_acceptsEmptyObject() throws Exception {
    assertThatNoException()
        .isThrownBy(() -> provider.validateConfig(mapper.readTree("{}")));
  }

  // ── store + retrieve ──────────────────────────────────────────────────────

  @Test
  void store_thenRetrieve_returnsSameBytes() throws IOException {
    byte[] content = "hello local fs".getBytes(StandardCharsets.UTF_8);
    String explicitKey = "tasks/123/abc.txt";

    StorePutContext ctx =
        StorePutContext.builder()
            .bucket("test-bucket")
            .key(explicitKey)
            .stream(new ByteArrayInputStream(content))
            .contentType("text/plain")
            .sizeBytes(content.length)
            .config(null)
            .build();

    StoredObjectRef ref = provider.store(ctx);

    // (a) returned key must equal the exact input key verbatim — no mangling
    assertThat(ref.getKey()).isEqualTo(explicitKey);
    assertThat(ref.getBucket()).isEqualTo("test-bucket");
    assertThat(ref.getContentType()).isEqualTo("text/plain");
    assertThat(ref.getSizeBytes()).isEqualTo(content.length);

    // (c) data file must exist at exactly tempDir/{key}
    Path expectedFile = tempDir.resolve(explicitKey);
    assertThat(Files.exists(expectedFile)).isTrue();

    // (b) retrieve round-trips bytes and (d) content-type via sidecar
    try (var stream = provider.retrieve(ref.getBucket(), ref.getKey(), null).getStream()) {
      byte[] retrieved = stream.readAllBytes();
      assertThat(retrieved).isEqualTo(content);
    }

    DownloadResource dl = provider.retrieve(ref.getBucket(), ref.getKey(), null);
    try (var stream = dl.getStream()) {
      assertThat(stream.readAllBytes()).isEqualTo(content);
    }
    assertThat(dl.getContentType()).isEqualTo("text/plain");
    assertThat(dl.getSizeBytes()).isEqualTo(content.length);
  }

  @Test
  void store_createsParentDirectories() {
    byte[] content = "nested".getBytes(StandardCharsets.UTF_8);
    String explicitKey = "a/b/c/file.txt";

    StorePutContext ctx =
        StorePutContext.builder()
            .bucket("bucket")
            .key(explicitKey)
            .stream(new ByteArrayInputStream(content))
            .contentType("text/plain")
            .sizeBytes(content.length)
            .config(null)
            .build();

    assertThatNoException().isThrownBy(() -> provider.store(ctx));

    // Verify file landed at the verbatim key path
    assertThat(Files.exists(tempDir.resolve(explicitKey))).isTrue();
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  void delete_removesFileFromDisk() throws IOException {
    byte[] content = "delete me".getBytes(StandardCharsets.UTF_8);
    String explicitKey = "del/file.bin";

    StorePutContext ctx =
        StorePutContext.builder()
            .bucket("bucket")
            .key(explicitKey)
            .stream(new ByteArrayInputStream(content))
            .contentType("application/octet-stream")
            .sizeBytes(content.length)
            .config(null)
            .build();

    StoredObjectRef ref = provider.store(ctx);

    // File must exist after store — close the stream properly
    try (var stream = provider.retrieve(ref.getBucket(), ref.getKey(), null).getStream()) {
      assertThat(stream.readAllBytes()).isEqualTo(content);
    }

    // Delete
    provider.delete(ref.getBucket(), ref.getKey(), null);

    // The underlying file should be gone at the verbatim key path
    Path stored = tempDir.resolve(ref.getKey());
    assertThat(Files.exists(stored)).isFalse();
  }

  @Test
  void delete_nonExistentKey_doesNotThrow() {
    assertThatNoException()
        .isThrownBy(() -> provider.delete("bucket", "no/such/file.bin", null));
  }

  // ── presignUrl ────────────────────────────────────────────────────────────

  @Test
  void presignUrl_returnsEmpty() {
    assertThat(provider.presignUrl("bucket", "key", 3600, null)).isEmpty();
  }

  @Test
  void supportsPresign_returnsFalse() {
    assertThat(provider.supportsPresign()).isFalse();
  }
}
