package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.storage.MigrationResultDTO;
import com.github.farzadsedaghatbin.shipflow.entity.TaskAttachment;
import com.github.farzadsedaghatbin.shipflow.repository.TaskAttachmentRepository;
import com.github.farzadsedaghatbin.shipflow.service.storage.DownloadResource;
import com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageService;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import com.github.farzadsedaghatbin.shipflow.service.storage.StoredObjectRef;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageMigrationService {

  private final ObjectStorageService objectStorageService;
  private final TaskAttachmentRepository taskAttachmentRepository;

  /**
   * Migrates all attachments to the currently active storage backend.
   * Extension point: add migrateWikiAttachments() in Task 14 and sum its results here.
   */
  public MigrationResultDTO migrateToActiveBackend() {
    MigrationResultDTO taskResult = migrateTaskAttachments();
    // Future: MigrationResultDTO wikiResult = migrateWikiAttachments();
    // Return sum of all entity-type migrations when wiki is added.
    return taskResult;
  }

  // ── Per-entity migration methods ──────────────────────────────────────────

  private MigrationResultDTO migrateTaskAttachments() {
    List<TaskAttachment> all = taskAttachmentRepository.findAll();
    StorageProviderType active = objectStorageService.activeProvider();
    int migrated = 0, skipped = 0, failed = 0;

    for (TaskAttachment att : all) {
      // Determine current provider (default LOCAL_FS for legacy rows)
      StorageProviderType currentProvider =
          att.getStorageProvider() != null ? att.getStorageProvider() : StorageProviderType.LOCAL_FS;
      // Determine current key (fall back to filePath for LOCAL_FS legacy rows)
      String currentKey =
          (att.getStorageKey() != null && !att.getStorageKey().isBlank())
              ? att.getStorageKey()
              : att.getFilePath();

      // Idempotent: skip rows already on the active provider
      if (currentProvider == active) {
        log.debug("Attachment {} already on active provider {}, skipping", att.getId(), active);
        skipped++;
        continue;
      }

      try {
        // 1. Retrieve from current backend
        DownloadResource resource = objectStorageService.retrieve(currentProvider, currentKey);

        // 2. Store to active backend
        String keyHint = "tasks/" + att.getTask().getId();
        StoredObjectRef ref =
            objectStorageService.store(
                keyHint,
                att.getFileName(),
                att.getContentType(),
                att.getFileSize(),
                resource.getStream());

        // 3. Update row — only AFTER store succeeds
        att.setStorageProvider(active);
        att.setStorageKey(ref.getKey());
        att.setFilePath(ref.getKey());
        taskAttachmentRepository.save(att);

        // 4. Best-effort delete of old object — AFTER row update succeeds
        try {
          objectStorageService.delete(currentProvider, currentKey);
        } catch (Exception delEx) {
          // Non-fatal: log but don't fail the migration for this row
          log.warn(
              "Best-effort delete failed for attachment id={} provider={}: {}",
              att.getId(),
              currentProvider,
              delEx.getMessage());
        }

        migrated++;
        log.info("Migrated attachment id={} from {} to {}", att.getId(), currentProvider, active);

      } catch (Exception ex) {
        failed++;
        // NEVER log secrets — only ids/provider
        log.error(
            "Failed to migrate attachment id={} from provider={}: {}",
            att.getId(),
            currentProvider,
            ex.getMessage());
      }
    }

    return MigrationResultDTO.builder()
        .migrated(migrated)
        .skipped(skipped)
        .failed(failed)
        .total(all.size())
        .build();
  }
}
