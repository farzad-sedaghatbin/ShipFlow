package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.TaskAttachmentDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TaskAttachment;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.TaskAttachmentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.storage.DownloadResource;
import com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageService;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import com.github.farzadsedaghatbin.shipflow.service.storage.StoredObjectRef;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles file upload, retrieval, and deletion for task attachments.
 *
 * <p>Validation (size, MIME type) and key generation are delegated to {@link ObjectStorageService}.
 * Allowed content types cover the most common work artefacts: images (screenshots, mockups), PDFs,
 * Word docs, plain text, and Markdown. Videos are intentionally excluded to keep storage lean.
 *
 * <p>Only the uploader or an ADMIN may delete an attachment; everyone with BACKLOG READ permission
 * may download.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskAttachmentService {

  private final TaskAttachmentRepository attachmentRepository;
  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final ObjectStorageService objectStorageService;

  // ── Upload ────────────────────────────────────────────────────────────────

  @Transactional
  public TaskAttachmentDTO uploadAttachment(Long taskId, MultipartFile file) {
    Task task = taskRepository.findById(taskId)
        .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File must not be empty");
    }

    String contentType = resolveContentType(file);

    // Delegate validation + persistence to the façade (throws IllegalArgumentException on violation)
    StoredObjectRef ref;
    try {
      ref = objectStorageService.store(
          "tasks/" + taskId,
          file.getOriginalFilename(),
          contentType,
          file.getSize(),
          file.getInputStream());
    } catch (IOException e) {
      throw new RuntimeException("Failed to store attachment: " + e.getMessage(), e);
    }

    User uploader = getCurrentUser();

    TaskAttachment attachment = TaskAttachment.builder()
        .task(task)
        .fileName(resolveOriginalName(file))
        .filePath(ref.getKey()) // populate legacy column for backward compatibility
        .storageProvider(objectStorageService.activeProvider())
        .storageKey(ref.getKey())
        .fileSize(file.getSize())
        .contentType(contentType)
        .uploadedBy(uploader)
        .build();

    attachment = attachmentRepository.save(attachment);
    log.info("Attachment uploaded: task={} file={} user={}", taskId, attachment.getFileName(),
        uploader.getUsername());
    return toDTO(attachment);
  }

  // ── List ──────────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<TaskAttachmentDTO> getAttachments(Long taskId) {
    if (!taskRepository.existsById(taskId)) {
      throw new ResourceNotFoundException("Task not found: " + taskId);
    }
    return attachmentRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
        .stream().map(this::toDTO).collect(Collectors.toList());
  }

  // ── Download ──────────────────────────────────────────────────────────────

  /** Carries both the file resource and its original metadata for the response. */
  public record DownloadResult(Resource resource, String originalFileName, String contentType) {}

  @Transactional(readOnly = true)
  public DownloadResult downloadAttachment(Long taskId, Long attachmentId) {
    TaskAttachment attachment = resolveAttachment(taskId, attachmentId);

    // Determine provider and key — fall back to LOCAL_FS + filePath for legacy rows
    StorageProviderType provider =
        attachment.getStorageProvider() != null
            ? attachment.getStorageProvider()
            : StorageProviderType.LOCAL_FS;
    String key =
        attachment.getStorageKey() != null && !attachment.getStorageKey().isBlank()
            ? attachment.getStorageKey()
            : attachment.getFilePath();

    DownloadResource dr = objectStorageService.retrieve(provider, key);

    // Use the entity's stored contentType and originalFileName (NOT the provider's)
    // to preserve correct metadata for legacy files that have no sidecar.
    Resource resource;
    try {
      resource = new InputStreamResource(dr.getStream());
    } catch (Exception e) {
      throw new ResourceNotFoundException("File not found: " + attachment.getFileName());
    }
    return new DownloadResult(resource, attachment.getFileName(), attachment.getContentType());
  }

  // ── Delete ────────────────────────────────────────────────────────────────

  @Transactional
  public void deleteAttachment(Long taskId, Long attachmentId) {
    TaskAttachment attachment = resolveAttachment(taskId, attachmentId);
    User currentUser = getCurrentUser();

    boolean isUploader = attachment.getUploadedBy() != null
        && attachment.getUploadedBy().getId().equals(currentUser.getId());
    boolean isAdmin = UserRole.ADMIN.equals(currentUser.getRole());

    if (!isUploader && !isAdmin) {
      throw new AccessDeniedException("Only the uploader or an ADMIN may delete this attachment");
    }

    // Determine provider and key — fall back to LOCAL_FS + filePath for legacy rows
    StorageProviderType provider =
        attachment.getStorageProvider() != null
            ? attachment.getStorageProvider()
            : StorageProviderType.LOCAL_FS;
    String key =
        attachment.getStorageKey() != null && !attachment.getStorageKey().isBlank()
            ? attachment.getStorageKey()
            : attachment.getFilePath();

    // Best-effort removal from storage backend
    objectStorageService.delete(provider, key);

    attachmentRepository.delete(attachment);
    log.info("Attachment deleted: id={} task={} user={}", attachmentId, taskId,
        currentUser.getUsername());
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private TaskAttachment resolveAttachment(Long taskId, Long attachmentId) {
    TaskAttachment a = attachmentRepository.findById(attachmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));
    if (!a.getTask().getId().equals(taskId)) {
      throw new ResourceNotFoundException(
          "Attachment " + attachmentId + " does not belong to task " + taskId);
    }
    return a;
  }

  private String resolveOriginalName(MultipartFile file) {
    String name = file.getOriginalFilename();
    return (name != null && !name.isBlank()) ? name : "attachment";
  }

  private String resolveContentType(MultipartFile file) {
    String ct = file.getContentType();
    if (ct != null && !ct.isBlank()) return ct;
    // Fall back to extension-based detection
    String name = file.getOriginalFilename();
    if (name != null) {
      String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
      return switch (ext) {
        case "pdf" -> "application/pdf";
        case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        case "doc" -> "application/msword";
        case "txt" -> "text/plain";
        case "md" -> "text/markdown";
        case "jpg", "jpeg" -> "image/jpeg";
        case "png" -> "image/png";
        case "gif" -> "image/gif";
        case "webp" -> "image/webp";
        case "svg" -> "image/svg+xml";
        default -> "application/octet-stream";
      };
    }
    return "application/octet-stream";
  }

  private User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
  }

  private TaskAttachmentDTO toDTO(TaskAttachment a) {
    return TaskAttachmentDTO.builder()
        .id(a.getId())
        .taskId(a.getTask().getId())
        .fileName(a.getFileName())
        .fileSize(a.getFileSize())
        .contentType(a.getContentType())
        .uploadedById(a.getUploadedBy() != null ? a.getUploadedBy().getId() : null)
        .uploadedByUsername(a.getUploadedBy() != null ? a.getUploadedBy().getUsername() : null)
        .createdAt(a.getCreatedAt())
        .build();
  }
}
