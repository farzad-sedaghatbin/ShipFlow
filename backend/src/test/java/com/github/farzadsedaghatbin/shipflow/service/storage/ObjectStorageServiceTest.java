package com.github.farzadsedaghatbin.shipflow.service.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.StorageConfig;
import com.github.farzadsedaghatbin.shipflow.service.StorageConfigService;
import com.github.farzadsedaghatbin.shipflow.service.storage.provider.LocalFsStorageProvider;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pure-unit tests for {@link ObjectStorageService}. No Spring context — uses {@code @TempDir} and
 * Mockito for {@link StorageConfigService}.
 */
class ObjectStorageServiceTest {

  @TempDir Path tempDir;

  private ObjectStorageService service;

  @BeforeEach
  void setUp() {
    LocalFsStorageProvider localFsProvider = new LocalFsStorageProvider(tempDir.toString());
    ObjectStorageRegistry registry = new ObjectStorageRegistry(List.of(localFsProvider));

    StorageConfig defaultConfig =
        StorageConfig.builder()
            .activeProvider(StorageProviderType.LOCAL_FS)
            .config("{}")
            .build();

    StorageConfigService storageConfigService = mock(StorageConfigService.class);
    when(storageConfigService.getActiveConfig()).thenReturn(defaultConfig);

    ObjectMapper objectMapper = new ObjectMapper();
    service = new ObjectStorageService(storageConfigService, registry, objectMapper);
  }

  // ── store + retrieve round-trip ───────────────────────────────────────────

  @Test
  void store_thenRetrieve_roundTripsBytes() throws Exception {
    byte[] content = "hello world".getBytes(UTF_8);
    StoredObjectRef ref =
        service.store(
            "test-hint", "hello.txt", "text/plain", content.length, new ByteArrayInputStream(content));

    assertThat(ref.getKey()).isNotNull();

    DownloadResource dl = service.retrieve(StorageProviderType.LOCAL_FS, ref.getKey());
    assertThat(dl.getStream().readAllBytes()).isEqualTo(content);
  }

  // ── validation — oversize ─────────────────────────────────────────────────

  @Test
  void store_oversizeFile_throwsIllegalArgumentException() {
    assertThatThrownBy(
            () ->
                service.store(
                    "hint",
                    "big.jpg",
                    "image/jpeg",
                    ObjectStorageService.MAX_FILE_SIZE + 1,
                    InputStream.nullInputStream()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("File exceeds the 10 MB limit");
  }

  // ── validation — disallowed content-type ─────────────────────────────────

  @Test
  void store_disallowedContentType_throwsIllegalArgumentException() {
    assertThatThrownBy(
            () ->
                service.store(
                    "hint", "video.mp4", "video/mp4", 100, InputStream.nullInputStream()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Unsupported file type 'video/mp4'. Allowed: images, PDF, DOCX, DOC, TXT, MD, JSON, ZIP");
  }

  // ── validation — JSON and ZIP are allowed ─────────────────────────────────

  @Test
  void store_jsonContentType_succeeds() throws Exception {
    byte[] content = "{\"a\":1}".getBytes(UTF_8);
    StoredObjectRef ref =
        service.store(
            "hint", "data.json", "application/json", content.length, new ByteArrayInputStream(content));

    assertThat(ref.getKey()).isNotNull();
  }

  @Test
  void store_zipContentType_succeeds() throws Exception {
    byte[] content = "pk-fake-zip-bytes".getBytes(UTF_8);
    StoredObjectRef ref =
        service.store(
            "hint", "archive.zip", "application/zip", content.length, new ByteArrayInputStream(content));

    assertThat(ref.getKey()).isNotNull();
  }

  // ── key generation ────────────────────────────────────────────────────────

  @Test
  void store_generatesNonNullKey() throws Exception {
    byte[] content = "data".getBytes(UTF_8);
    StoredObjectRef ref =
        service.store(
            "wiki/pages", "doc.pdf", "application/pdf", content.length, new ByteArrayInputStream(content));

    assertThat(ref.getKey()).isNotNull().startsWith("wiki/pages/");
  }

  // ── retrieve by returned key ──────────────────────────────────────────────

  @Test
  void retrieve_byReturnedKey_succeeds() throws Exception {
    byte[] content = "retrieve test".getBytes(UTF_8);
    StoredObjectRef ref =
        service.store(
            "docs", "file.txt", "text/plain", content.length, new ByteArrayInputStream(content));

    DownloadResource dl = service.retrieve(StorageProviderType.LOCAL_FS, ref.getKey());
    assertThat(dl.getStream().readAllBytes()).isEqualTo(content);
  }
}
