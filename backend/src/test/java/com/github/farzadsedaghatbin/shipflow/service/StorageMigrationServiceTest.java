package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.storage.MigrationResultDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TaskAttachment;
import com.github.farzadsedaghatbin.shipflow.entity.WikiAttachment;
import com.github.farzadsedaghatbin.shipflow.repository.TaskAttachmentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiAttachmentRepository;
import com.github.farzadsedaghatbin.shipflow.service.storage.DownloadResource;
import com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageService;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import com.github.farzadsedaghatbin.shipflow.service.storage.StoredObjectRef;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageMigrationServiceTest {

  @Mock private ObjectStorageService objectStorageService;
  @Mock private TaskAttachmentRepository taskAttachmentRepository;
  @Mock private WikiAttachmentRepository wikiAttachmentRepository;

  private StorageMigrationService service;

  @BeforeEach
  void setUp() {
    service =
        new StorageMigrationService(
            objectStorageService, taskAttachmentRepository, wikiAttachmentRepository);
    // Default: no wiki attachments — prevents NPE in tests that only exercise task migration
    lenient().when(wikiAttachmentRepository.findByDeletedAtIsNull()).thenReturn(List.of());
  }

  private Task makeTask(Long id) {
    Task t = new Task();
    t.setId(id);
    return t;
  }

  private TaskAttachment makeAttachment(
      Long id, StorageProviderType provider, String key, Task task) {
    TaskAttachment att = new TaskAttachment();
    att.setId(id);
    att.setStorageProvider(provider);
    att.setStorageKey(key);
    att.setFilePath(key);
    att.setFileName("file.txt");
    att.setContentType("text/plain");
    att.setFileSize(100L);
    att.setTask(task);
    att.setCreatedAt(LocalDateTime.now());
    return att;
  }

  @Test
  @DisplayName("(a) Row already on active provider is skipped — no store or delete calls")
  void rowAlreadyOnActiveProvider_isSkipped() {
    when(objectStorageService.activeProvider()).thenReturn(StorageProviderType.S3);
    TaskAttachment att = makeAttachment(1L, StorageProviderType.S3, "existing-key", makeTask(10L));
    when(taskAttachmentRepository.findAll()).thenReturn(List.of(att));

    MigrationResultDTO result = service.migrateToActiveBackend();

    assertThat(result.getSkipped()).isEqualTo(1);
    assertThat(result.getMigrated()).isEqualTo(0);
    assertThat(result.getFailed()).isEqualTo(0);
    assertThat(result.getTotal()).isEqualTo(1);
    verify(objectStorageService, never()).store(any(), any(), any(), anyLong(), any());
    verify(objectStorageService, never()).delete(any(), any());
  }

  @Test
  @DisplayName("(b) Row on different provider: store called, row updated, OLD object deleted AFTER store")
  void rowOnDifferentProvider_migratedAndOldDeleted() {
    when(objectStorageService.activeProvider()).thenReturn(StorageProviderType.S3);
    Task task = makeTask(42L);
    TaskAttachment att = makeAttachment(2L, StorageProviderType.LOCAL_FS, "old-key", task);
    when(taskAttachmentRepository.findAll()).thenReturn(List.of(att));

    DownloadResource fakeResource =
        DownloadResource.builder()
            .stream(new ByteArrayInputStream(new byte[0]))
            .contentType("text/plain")
            .sizeBytes(100L)
            .filename("file.txt")
            .build();
    when(objectStorageService.retrieve(StorageProviderType.LOCAL_FS, "old-key"))
        .thenReturn(fakeResource);

    StoredObjectRef newRef =
        StoredObjectRef.builder()
            .key("new-key")
            .bucket("bucket")
            .contentType("text/plain")
            .sizeBytes(100L)
            .build();
    when(objectStorageService.store(eq("tasks/42"), any(), any(), anyLong(), any()))
        .thenReturn(newRef);

    MigrationResultDTO result = service.migrateToActiveBackend();

    assertThat(result.getMigrated()).isEqualTo(1);
    assertThat(result.getSkipped()).isEqualTo(0);
    assertThat(result.getFailed()).isEqualTo(0);

    // Row was updated to new provider and key
    assertThat(att.getStorageProvider()).isEqualTo(StorageProviderType.S3);
    assertThat(att.getStorageKey()).isEqualTo("new-key");
    assertThat(att.getFilePath()).isEqualTo("new-key");

    // Verify order: store → save → delete
    InOrder order = inOrder(objectStorageService, taskAttachmentRepository);
    order.verify(objectStorageService).store(eq("tasks/42"), any(), any(), anyLong(), any());
    order.verify(taskAttachmentRepository).save(att);
    order.verify(objectStorageService).delete(StorageProviderType.LOCAL_FS, "old-key");
  }

  @Test
  @DisplayName("(c) Store throws → row counted failed, source NOT deleted")
  void storeThrows_rowFailedSourceNotDeleted() {
    when(objectStorageService.activeProvider()).thenReturn(StorageProviderType.S3);
    Task task = makeTask(99L);
    TaskAttachment att = makeAttachment(3L, StorageProviderType.MINIO, "minio-key", task);
    when(taskAttachmentRepository.findAll()).thenReturn(List.of(att));

    DownloadResource fakeResource =
        DownloadResource.builder()
            .stream(new ByteArrayInputStream(new byte[0]))
            .contentType("text/plain")
            .sizeBytes(100L)
            .filename("file.txt")
            .build();
    when(objectStorageService.retrieve(StorageProviderType.MINIO, "minio-key"))
        .thenReturn(fakeResource);
    when(objectStorageService.store(any(), any(), any(), anyLong(), any()))
        .thenThrow(new RuntimeException("S3 connection refused"));

    MigrationResultDTO result = service.migrateToActiveBackend();

    assertThat(result.getFailed()).isEqualTo(1);
    assertThat(result.getMigrated()).isEqualTo(0);
    verify(objectStorageService, never()).delete(any(), any());
  }

  // ── Wiki attachment migration tests ──────────────────────────────────────

  private WikiAttachment makeWikiAttachment(
      Long id, Long pageId, StorageProviderType provider, String key) {
    WikiAttachment att = new WikiAttachment();
    att.setId(id);
    att.setPageId(pageId);
    att.setStorageProvider(provider);
    att.setStorageKey(key);
    att.setFileName("wiki-file.png");
    att.setContentType("image/png");
    att.setFileSize(512L);
    att.setUploadedBy(1L);
    att.setCreatedAt(OffsetDateTime.now());
    return att;
  }

  @Test
  @DisplayName("(d) Wiki attachment already on active provider is skipped")
  void wikiAttachment_alreadyOnActiveProvider_isSkipped() {
    when(objectStorageService.activeProvider()).thenReturn(StorageProviderType.S3);
    when(taskAttachmentRepository.findAll()).thenReturn(List.of());
    WikiAttachment att = makeWikiAttachment(10L, 5L, StorageProviderType.S3, "existing-wiki-key");
    when(wikiAttachmentRepository.findByDeletedAtIsNull()).thenReturn(List.of(att));

    MigrationResultDTO result = service.migrateToActiveBackend();

    assertThat(result.getSkipped()).isEqualTo(1);
    assertThat(result.getMigrated()).isEqualTo(0);
    assertThat(result.getFailed()).isEqualTo(0);
    assertThat(result.getTotal()).isEqualTo(1);
    verify(objectStorageService, never()).store(any(), any(), any(), anyLong(), any());
    verify(objectStorageService, never()).delete(any(), any());
  }

  @Test
  @DisplayName("(e) Wiki attachment on different provider: store→update→delete in order")
  void wikiAttachment_onDifferentProvider_migratedAndOldDeleted() {
    when(objectStorageService.activeProvider()).thenReturn(StorageProviderType.S3);
    when(taskAttachmentRepository.findAll()).thenReturn(List.of());
    WikiAttachment att =
        makeWikiAttachment(11L, 20L, StorageProviderType.LOCAL_FS, "old-wiki-key");
    when(wikiAttachmentRepository.findByDeletedAtIsNull()).thenReturn(List.of(att));

    DownloadResource fakeResource =
        DownloadResource.builder()
            .stream(new ByteArrayInputStream(new byte[0]))
            .contentType("image/png")
            .sizeBytes(512L)
            .filename("wiki-file.png")
            .build();
    when(objectStorageService.retrieve(StorageProviderType.LOCAL_FS, "old-wiki-key"))
        .thenReturn(fakeResource);

    StoredObjectRef newRef =
        StoredObjectRef.builder()
            .key("wiki/20/wiki-file.png")
            .bucket("bucket")
            .contentType("image/png")
            .sizeBytes(512L)
            .build();
    when(objectStorageService.store(eq("wiki/20"), any(), any(), anyLong(), any()))
        .thenReturn(newRef);

    MigrationResultDTO result = service.migrateToActiveBackend();

    assertThat(result.getMigrated()).isEqualTo(1);
    assertThat(result.getSkipped()).isEqualTo(0);
    assertThat(result.getFailed()).isEqualTo(0);
    assertThat(result.getTotal()).isEqualTo(1);

    assertThat(att.getStorageProvider()).isEqualTo(StorageProviderType.S3);
    assertThat(att.getStorageKey()).isEqualTo("wiki/20/wiki-file.png");

    // Verify copy-verify-before-delete order: store → save → delete
    InOrder order = inOrder(objectStorageService, wikiAttachmentRepository);
    order.verify(objectStorageService).store(eq("wiki/20"), any(), any(), anyLong(), any());
    order.verify(wikiAttachmentRepository).save(att);
    order.verify(objectStorageService).delete(StorageProviderType.LOCAL_FS, "old-wiki-key");
  }

  @Test
  @DisplayName("(f) Total counts sum task + wiki migration results")
  void totalCountsSumTaskAndWikiResults() {
    when(objectStorageService.activeProvider()).thenReturn(StorageProviderType.S3);

    // One task att already on S3 (skipped)
    TaskAttachment taskAtt =
        makeAttachment(1L, StorageProviderType.S3, "task-key", makeTask(10L));
    when(taskAttachmentRepository.findAll()).thenReturn(List.of(taskAtt));

    // One wiki att already on S3 (skipped)
    WikiAttachment wikiAtt =
        makeWikiAttachment(2L, 5L, StorageProviderType.S3, "wiki-key");
    when(wikiAttachmentRepository.findByDeletedAtIsNull()).thenReturn(List.of(wikiAtt));

    MigrationResultDTO result = service.migrateToActiveBackend();

    assertThat(result.getTotal()).isEqualTo(2);
    assertThat(result.getSkipped()).isEqualTo(2);
    assertThat(result.getMigrated()).isEqualTo(0);
    assertThat(result.getFailed()).isEqualTo(0);
  }
}
