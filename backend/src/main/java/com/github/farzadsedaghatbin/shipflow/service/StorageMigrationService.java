package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.storage.MigrationResultDTO;
import com.github.farzadsedaghatbin.shipflow.entity.TaskAttachment;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.entity.WikiAttachment;
import com.github.farzadsedaghatbin.shipflow.repository.TaskAttachmentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UploadedDocumentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiAttachmentRepository;
import com.github.farzadsedaghatbin.shipflow.service.storage.DownloadResource;
import com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageService;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import com.github.farzadsedaghatbin.shipflow.service.storage.StoredObjectRef;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StorageMigrationService {

  private final ObjectStorageService objectStorageService;
  private final TaskAttachmentRepository taskAttachmentRepository;
  private final WikiAttachmentRepository wikiAttachmentRepository;
  private final UploadedDocumentRepository documentRepository;

  @Value("${app.upload.dir:uploads}")
  private String uploadDir;

  public StorageMigrationService(
      ObjectStorageService objectStorageService,
      TaskAttachmentRepository taskAttachmentRepository,
      WikiAttachmentRepository wikiAttachmentRepository,
      UploadedDocumentRepository documentRepository) {
    this.objectStorageService = objectStorageService;
    this.taskAttachmentRepository = taskAttachmentRepository;
    this.wikiAttachmentRepository = wikiAttachmentRepository;
    this.documentRepository = documentRepository;
  }

  /**
   * Migrates all attachments (tasks + wiki + documents) to the currently active storage backend.
   * Totals are the sum of all per-entity-type migration passes.
   */
  public MigrationResultDTO migrateToActiveBackend() {
    MigrationResultDTO taskResult = migrateTaskAttachments();
    MigrationResultDTO wikiResult = migrateWikiAttachments();
    MigrationResultDTO docResult = migrateDocuments();
    return MigrationResultDTO.builder()
        .migrated(taskResult.getMigrated() + wikiResult.getMigrated() + docResult.getMigrated())
        .skipped(taskResult.getSkipped() + wikiResult.getSkipped() + docResult.getSkipped())
        .failed(taskResult.getFailed() + wikiResult.getFailed() + docResult.getFailed())
        .total(taskResult.getTotal() + wikiResult.getTotal() + docResult.getTotal())
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
        String keyHint = "attachments/task/" + att.getTask().getId();
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
        String keyHint = "attachments/wiki/" + att.getPageId();
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

  MigrationResultDTO migrateDocuments() {
    List<UploadedDocument> all = documentRepository.findAll();
    StorageProviderType active = objectStorageService.activeProvider();
    int migrated = 0, skipped = 0, failed = 0;

    for (UploadedDocument doc : all) {
      boolean hasKey = doc.getStorageKey() != null && !doc.getStorageKey().isBlank();

      // ── Legacy row (raw filesystem, no storage key): copy disk bytes onto the active backend ──
      if (!hasKey) {
        if (doc.getStoragePath() == null || doc.getStoragePath().isBlank()) {
          // Nothing to migrate (no key, no path) — count as skipped.
          skipped++;
          continue;
        }
        try {
          Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
          Path filePath = uploadPath.resolve(doc.getStoragePath()).normalize();
          if (!filePath.startsWith(uploadPath)) {
            failed++;
            log.error("Refusing to migrate document id={} — storagePath escapes upload dir", doc.getId());
            continue;
          }
          byte[] bytes = Files.readAllBytes(filePath);
          StoredObjectRef ref =
              objectStorageService.storeWithoutValidation(
                  DocumentService.storageKeyHint(doc.getEntityType(), doc.getEntityId()),
                  doc.getOriginalFileName(),
                  contentTypeFor(doc.getFileType()),
                  bytes.length,
                  new ByteArrayInputStream(bytes));
          doc.setStorageProvider(active);
          doc.setStorageKey(ref.getKey());
          documentRepository.save(doc);
          migrated++;
          log.info("Migrated legacy document id={} from disk to {}", doc.getId(), active);
        } catch (Exception ex) {
          failed++;
          log.error("Failed to migrate legacy document id={}: {}", doc.getId(), ex.getMessage());
        }
        continue;
      }

      // ── SPI-managed row: re-store onto the active backend (mirrors the wiki/task pass) ──
      StorageProviderType currentProvider =
          doc.getStorageProvider() != null ? doc.getStorageProvider() : StorageProviderType.LOCAL_FS;

      if (currentProvider == active) {
        log.debug("Document {} already on active provider {}, skipping", doc.getId(), active);
        skipped++;
        continue;
      }

      try {
        // 1. Retrieve from current backend
        DownloadResource resource = objectStorageService.retrieve(currentProvider, doc.getStorageKey());

        // 2. Store to active backend
        StoredObjectRef ref =
            objectStorageService.storeWithoutValidation(
                DocumentService.storageKeyHint(doc.getEntityType(), doc.getEntityId()),
                doc.getOriginalFileName(),
                contentTypeFor(doc.getFileType()),
                doc.getFileSize() != null ? doc.getFileSize() : 0L,
                resource.getStream());

        // 3. Update row — only AFTER store succeeds
        String oldKey = doc.getStorageKey();
        doc.setStorageProvider(active);
        doc.setStorageKey(ref.getKey());
        documentRepository.save(doc);

        // 4. Verify the stored copy is readable before deleting the source
        boolean verified = false;
        try {
          DownloadResource verify = objectStorageService.retrieve(active, ref.getKey());
          verified = verify != null;
        } catch (Exception verEx) {
          log.error(
              "Post-store verify failed for document id={} key={}: {}",
              doc.getId(),
              ref.getKey(),
              verEx.getMessage());
        }

        if (!verified) {
          failed++;
          log.error(
              "Verification failed for document id={} — source NOT deleted, row marked failed",
              doc.getId());
          continue;
        }

        // 5. Best-effort delete of old object — ONLY AFTER verify succeeds
        try {
          objectStorageService.delete(currentProvider, oldKey);
        } catch (Exception delEx) {
          log.warn(
              "Best-effort delete failed for document id={} provider={}: {}",
              doc.getId(),
              currentProvider,
              delEx.getMessage());
        }

        migrated++;
        log.info("Migrated document id={} from {} to {}", doc.getId(), currentProvider, active);

      } catch (Exception ex) {
        failed++;
        log.error(
            "Failed to migrate document id={} from provider={}: {}",
            doc.getId(),
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

  /** Best-effort MIME type for a document file extension; recorded as object metadata. */
  private static String contentTypeFor(String fileType) {
    if (fileType == null) {
      return "application/octet-stream";
    }
    return switch (fileType.toLowerCase()) {
      case "pdf" -> "application/pdf";
      case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
      case "doc" -> "application/msword";
      case "txt" -> "text/plain";
      case "md", "markdown" -> "text/markdown";
      case "jpg", "jpeg" -> "image/jpeg";
      case "png" -> "image/png";
      case "gif" -> "image/gif";
      case "webp" -> "image/webp";
      case "svg" -> "image/svg+xml";
      case "mp4" -> "video/mp4";
      case "webm" -> "video/webm";
      case "mov" -> "video/quicktime";
      case "avi" -> "video/x-msvideo";
      default -> "application/octet-stream";
    };
  }
}
