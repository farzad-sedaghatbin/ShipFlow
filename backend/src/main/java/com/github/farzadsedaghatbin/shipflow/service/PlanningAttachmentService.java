package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.DocumentUploadResponse;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Image/video attachments on Epics and Initiatives.
 *
 * <p>Epics and initiatives only had a text description; this lets people attach screenshots,
 * mock-ups and short clips to them. Files are stored as {@code UploadedDocument} rows through the
 * same media path bug reports use ({@link DocumentService#uploadMediaAttachment}), keyed by
 * {@code entityType} {@code EPIC}/{@code INITIATIVE}, so storage (LOCAL_FS/S3/MinIO) and the
 * object-key convention are shared and no migration is needed. Media is never text-extracted or
 * indexed for Q&amp;A.
 *
 * <p>Access follows the owning project: viewing needs project access, adding or removing needs
 * contributor rights. A non-admin cannot reach an epic/initiative that has no project.
 */
@Service
@RequiredArgsConstructor
public class PlanningAttachmentService {

  public static final String EPIC = "EPIC";
  public static final String INITIATIVE = "INITIATIVE";

  /** Per-file cap (images and short clips); the generic media default of 50 MB is too generous here. */
  static final long MAX_BYTES = 20L * 1024 * 1024;

  private final DocumentService documentService;
  private final EpicRepository epicRepository;
  private final InitiativeRepository initiativeRepository;
  private final ProjectPermissionService projectPermissionService;

  /** Normalises {@code epic}/{@code Initiative}/... to the stored entity type, or rejects it. */
  public static String normalizeType(String type) {
    String t = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
    if (!EPIC.equals(t) && !INITIATIVE.equals(t)) {
      throw new IllegalArgumentException("Attachments are supported on epics and initiatives only");
    }
    return t;
  }

  @Transactional(readOnly = true)
  public List<UploadedDocument> list(String type, Long id) {
    String t = normalizeType(type);
    projectPermissionService.requireProjectAccess(resolveProjectId(t, id));
    return documentService.getDocumentsByEntity(t, id);
  }

  @Transactional
  public DocumentUploadResponse upload(String type, Long id, MultipartFile file, Long uploaderId,
      String uploaderUsername) {
    String t = normalizeType(type);
    projectPermissionService.requireContributor(resolveProjectId(t, id));
    return documentService.uploadMediaAttachment(file, t, id, uploaderId, uploaderUsername, MAX_BYTES);
  }

  @Transactional
  public void delete(String type, Long id, Long attachmentId) {
    String t = normalizeType(type);
    projectPermissionService.requireContributor(resolveProjectId(t, id));
    UploadedDocument doc = documentService.getDocumentById(attachmentId);
    // The id in the URL must really be an attachment of THIS epic/initiative, otherwise a
    // contributor on one project could delete any document by guessing its id.
    if (!t.equals(doc.getEntityType()) || !id.equals(doc.getEntityId())) {
      throw new ResourceNotFoundException("Attachment not found on this " + t.toLowerCase(Locale.ROOT));
    }
    documentService.deleteDocument(attachmentId);
  }

  private Long resolveProjectId(String type, Long id) {
    Project project = EPIC.equals(type)
        ? epicRepository.findByIdNotDeleted(id)
            .orElseThrow(() -> notFound(type, id)).getProject()
        : initiativeRepository.findByIdNotDeleted(id)
            .orElseThrow(() -> notFound(type, id)).getProject();
    if (project == null) {
      throw new AccessDeniedException("This " + type.toLowerCase(Locale.ROOT) + " has no project");
    }
    return project.getId();
  }

  private static ResourceNotFoundException notFound(String type, Long id) {
    return new ResourceNotFoundException(type.toLowerCase(Locale.ROOT) + " " + id + " not found");
  }
}
