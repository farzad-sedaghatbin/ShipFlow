package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.TaskAttachmentDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TaskAttachment;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.TaskAttachmentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles file upload, retrieval, and deletion for task attachments.
 *
 * <p>Allowed content types cover the most common work artefacts:
 * images (screenshots, mockups), PDFs, Word docs, plain text, and Markdown.
 * Videos are intentionally excluded to keep storage lean.
 *
 * <p>Only the uploader or an ADMIN may delete an attachment; everyone with
 * BACKLOG READ permission may download.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskAttachmentService {

  private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB

  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
      "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml",
      "application/pdf",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "application/msword",
      "text/plain",
      "text/markdown"
  );

  private final TaskAttachmentRepository attachmentRepository;
  private final TaskRepository taskRepository;
  private final UserRepository userRepository;

  @Value("${app.upload.dir:uploads}")
  private String uploadDir;

  // ── Upload ────────────────────────────────────────────────────────────────

  @Transactional
  public TaskAttachmentDTO uploadAttachment(Long taskId, MultipartFile file) {
    Task task = taskRepository.findById(taskId)
        .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

    validateFile(file);

    User uploader = getCurrentUser();

    String originalName = sanitize(file.getOriginalFilename());
    String storedName = UUID.randomUUID() + "_" + originalName;

    Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    try {
      Files.createDirectories(uploadPath);
      Path dest = uploadPath.resolve(storedName);
      if (!dest.normalize().startsWith(uploadPath)) {
        throw new IllegalArgumentException("Invalid file path detected");
      }
      Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException("Failed to store attachment: " + e.getMessage(), e);
    }

    TaskAttachment attachment = TaskAttachment.builder()
        .task(task)
        .fileName(originalName)
        .filePath(storedName)
        .fileSize(file.getSize())
        .contentType(resolveContentType(file))
        .uploadedBy(uploader)
        .build();

    attachment = attachmentRepository.save(attachment);
    log.info("Attachment uploaded: task={} file={} user={}", taskId, originalName, uploader.getUsername());
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
    try {
      Path file = Paths.get(uploadDir).toAbsolutePath().normalize()
          .resolve(attachment.getFilePath()).normalize();
      Resource resource = new UrlResource(file.toUri());
      if (!resource.exists() || !resource.isReadable()) {
        throw new ResourceNotFoundException("File not found on disk: " + attachment.getFileName());
      }
      return new DownloadResult(resource, attachment.getFileName(), attachment.getContentType());
    } catch (java.net.MalformedURLException e) {
      throw new RuntimeException("Could not resolve file path", e);
    }
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

    // Remove from disk (best-effort; DB record always removed)
    try {
      Path file = Paths.get(uploadDir).toAbsolutePath().normalize()
          .resolve(attachment.getFilePath()).normalize();
      Files.deleteIfExists(file);
    } catch (IOException e) {
      log.warn("Could not delete attachment file from disk: {}", attachment.getFilePath(), e);
    }

    attachmentRepository.delete(attachment);
    log.info("Attachment deleted: id={} task={} user={}", attachmentId, taskId, currentUser.getUsername());
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private TaskAttachment resolveAttachment(Long taskId, Long attachmentId) {
    TaskAttachment a = attachmentRepository.findById(attachmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));
    if (!a.getTask().getId().equals(taskId)) {
      throw new ResourceNotFoundException("Attachment " + attachmentId + " does not belong to task " + taskId);
    }
    return a;
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File must not be empty");
    }
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File exceeds the 10 MB limit");
    }
    String ct = resolveContentType(file);
    if (!ALLOWED_CONTENT_TYPES.contains(ct)) {
      throw new IllegalArgumentException(
          "Unsupported file type '" + ct + "'. Allowed: images, PDF, DOCX, DOC, TXT, MD");
    }
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

  private String sanitize(String name) {
    if (name == null || name.isBlank()) return "attachment";
    // Strip directory separators and null bytes
    String safe = name.replaceAll("[/\\\\:\\*\\?\"<>|\\x00]", "_");
    // Truncate to 200 characters to stay well within VARCHAR(255) and filesystem limits
    return safe.length() > 200 ? safe.substring(0, 200) : safe;
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
