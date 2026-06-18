package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.storage.MigrationResultDTO;
import com.github.farzadsedaghatbin.shipflow.entity.TaskAttachment;
import com.github.farzadsedaghatbin.shipflow.entity.WikiAttachment;
import com.github.farzadsedaghatbin.shipflow.repository.TaskAttachmentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiAttachmentRepository;
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
  private final WikiAttachmentRepository wikiAttachmentRepository;

  /**
   * Migrates all attachments (tasks + wiki) to the currently active storage backend.
   * Totals are the sum of all per-entity-type migration passes.
   */
  public MigrationResultDTO migrateToActiveBackend() {
    MigrationResultDTO taskResult = migrateTaskAttachments();
    MigrationResultDTO wikiResult = migrateWikiAttachments();
    return MigrationResultDTO.builder()
        .migrated(taskResult.getMigrated() + wikiResult.getMigrated())
        .skipped(taskResult.getSkipped() + wikiResult.getSkipped())
        .failed(taskResult.getFailed() + wikiResult.getFailed())
        .total(taskResult.getTotal() + wikiResult.getTotal())
        .build();
  }

  // ── Per-entity migration methods ──────────────────────────────────────────

  private MigrationResultDTO migrateTaskAttachments() {
    // TaskAttachment currently has no deletedAt soft-delete column, so findAll() is correct here.
    // If soft-delete is added later, replace with a "not-deleted" query so logically-deleted
    // attachments are not migrated.
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

        // 4. Verify the stored copy is readable before deleting the source
        boolean verified = false;
        try {
          DownloadResource verify = objectStorageService.retrieve(active, ref.getKey());
          verified = verify != null;
        } catch (Exception verEx) {
          log.error(
              "Post-store verify failed for attachment id={} key={}: {}",
              att.getId(),
              ref.getKey(),
              verEx.getMessage());
        }

        if (!verified) {
          // Source is preserved; count this row as failed so the operator can retry
          failed++;
          log.error(
              "Verification failed for attachment id={} — source NOT deleted, row marked failed",
              att.getId());
          continue;
        }

        // 5. Best-effort delete of old object — ONLY AFTER verify succeeds
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

  MigrationResultDTO migrateWikiAttachments() {
    List<WikiAttachment> all = wikiAttachmentRepository.findByDeletedAtIsNull();
    StorageProviderType active = objectStorageService.activeProvider();
    int migrated = 0, skipped = 0, failed = 0;

    for (WikiAttachment att : all) {
      StorageProviderType currentProvider =
          att.getStorageProvider() != null ? att.getStorageProvider() : StorageProviderType.LOCAL_FS;
      String currentKey =
          (att.getStorageKey() != null && !att.getStorageKey().isBlank())
              ? att.getStorageKey()
              : att.getFileName();

      // Idempotent: skip rows already on the active provider
      if (currentProvider == active) {
        log.debug(
            "WikiAttachment {} already on active provider {}, skipping", att.getId(), active);
        skipped++;
        continue;
      }

      try {
        // 1. Retrieve from current backend
        DownloadResource resource = objectStorageService.retrieve(currentProvider, currentKey);

        // 2. Store to active backend
        String keyHint = "wiki/" + att.getPageId();
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
        wikiAttachmentRepository.save(att);

        // 4. Verify the stored copy is readable before deleting the source
        boolean verified = false;
        try {
          DownloadResource verify = objectStorageService.retrieve(active, ref.getKey());
          verified = verify != null;
        } catch (Exception verEx) {
          log.error(
              "Post-store verify failed for wiki attachment id={} key={}: {}",
              att.getId(),
              ref.getKey(),
              verEx.getMessage());
        }

        if (!verified) {
          failed++;
          log.error(
              "Verification failed for wiki attachment id={} — source NOT deleted, row marked failed",
              att.getId());
          continue;
        }

        // 5. Best-effort delete of old object — ONLY AFTER verify succeeds
        try {
          objectStorageService.delete(currentProvider, currentKey);
        } catch (Exception delEx) {
          log.warn(
              "Best-effort delete failed for wiki attachment id={} provider={}: {}",
              att.getId(),
              currentProvider,
              delEx.getMessage());
        }

        migrated++;
        log.info(
            "Migrated wiki attachment id={} from {} to {}", att.getId(), currentProvider, active);

      } catch (Exception ex) {
        failed++;
        log.error(
            "Failed to migrate wiki attachment id={} from provider={}: {}",
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
