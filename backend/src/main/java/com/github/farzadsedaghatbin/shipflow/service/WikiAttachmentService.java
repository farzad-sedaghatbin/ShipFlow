package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiAttachmentDTO;
import com.github.farzadsedaghatbin.shipflow.entity.WikiAttachment;
import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.WikiAttachmentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiPageRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpaceRepository;
import com.github.farzadsedaghatbin.shipflow.service.storage.DownloadResource;
import com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageService;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageKeyGenerator;
import com.github.farzadsedaghatbin.shipflow.service.storage.StoredObjectRef;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikiAttachmentService {

  public record DownloadResult(Resource resource, String originalFileName, String contentType) {}

  private final WikiAttachmentRepository attachmentRepository;
  private final WikiPageRepository pageRepository;
  private final WikiSpaceRepository spaceRepository;
  private final WikiPermissionService wikiPermissionService;
  private final ObjectStorageService objectStorageService;

  @Transactional
  public WikiAttachmentDTO upload(Long pageId, MultipartFile file, Long userId) {
    WikiPage page = requirePage(pageId);
    WikiSpace space = requireSpace(page.getSpaceId());
    wikiPermissionService.requireWrite(userId, space);

    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File must not be empty");
    }

    String contentType = resolveContentType(file);

    StoredObjectRef ref;
    try {
      ref =
          objectStorageService.store(
              "wiki/" + pageId,
              file.getOriginalFilename(),
              contentType,
              file.getSize(),
              file.getInputStream());
    } catch (IOException e) {
      throw new RuntimeException("Failed to store wiki attachment: " + e.getMessage(), e);
    }

    WikiAttachment attachment =
        WikiAttachment.builder()
            .pageId(pageId)
            .storageProvider(objectStorageService.activeProvider())
            .storageKey(ref.getKey())
            .fileName(StorageKeyGenerator.sanitize(file.getOriginalFilename()))
            .contentType(contentType)
            .fileSize(file.getSize())
            .uploadedBy(userId)
            .build();

    attachment = attachmentRepository.save(attachment);
    log.info(
        "Wiki attachment uploaded: page={} file={} user={}",
        pageId,
        attachment.getFileName(),
        userId);
    return toDTO(attachment);
  }

  @Transactional(readOnly = true)
  public List<WikiAttachmentDTO> list(Long pageId, Long userId) {
    WikiPage page = requirePage(pageId);
    WikiSpace space = requireSpace(page.getSpaceId());
    wikiPermissionService.requireRead(userId, space);
    return attachmentRepository
        .findByPageIdAndDeletedAtIsNullOrderByCreatedAtDesc(pageId)
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public DownloadResult download(Long attachmentId, Long userId) {
    WikiAttachment att =
        attachmentRepository
            .findById(attachmentId)
            .filter(a -> a.getDeletedAt() == null)
            .orElseThrow(
                () -> new ResourceNotFoundException("Attachment not found: " + attachmentId));

    WikiPage page = requirePage(att.getPageId());
    WikiSpace space = requireSpace(page.getSpaceId());
    wikiPermissionService.requireRead(userId, space);

    Resource resource;
    try {
      DownloadResource dr =
          objectStorageService.retrieve(att.getStorageProvider(), att.getStorageKey());
      resource = new InputStreamResource(dr.getStream());
    } catch (ResourceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ResourceNotFoundException("File not found: " + att.getFileName());
    }
    return new DownloadResult(resource, att.getFileName(), att.getContentType());
  }

  @Transactional
  public void delete(Long attachmentId, Long userId) {
    WikiAttachment att =
        attachmentRepository
            .findById(attachmentId)
            .filter(a -> a.getDeletedAt() == null)
            .orElseThrow(
                () -> new ResourceNotFoundException("Attachment not found: " + attachmentId));

    WikiPage page = requirePage(att.getPageId());
    WikiSpace space = requireSpace(page.getSpaceId());
    wikiPermissionService.requireWrite(userId, space);

    att.setDeletedAt(OffsetDateTime.now());
    attachmentRepository.save(att);

    try {
      objectStorageService.delete(att.getStorageProvider(), att.getStorageKey());
    } catch (Exception e) {
      log.warn(
          "Best-effort delete from storage failed for key={}: {}",
          att.getStorageKey(),
          e.getMessage());
    }
  }

  private WikiPage requirePage(Long pageId) {
    return pageRepository
        .findById(pageId)
        .filter(p -> p.getDeletedAt() == null)
        .orElseThrow(() -> new ResourceNotFoundException("Page not found: " + pageId));
  }

  private WikiSpace requireSpace(Long spaceId) {
    return spaceRepository
        .findById(spaceId)
        .orElseThrow(() -> new ResourceNotFoundException("Space not found: " + spaceId));
  }

  private String resolveContentType(MultipartFile file) {
    String ct = file.getContentType();
    if (ct != null && !ct.isBlank()) return ct;
    String name = file.getOriginalFilename();
    if (name != null) {
      String ext =
          name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
      return switch (ext) {
        case "pdf" -> "application/pdf";
        case "docx" ->
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
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

  private WikiAttachmentDTO toDTO(WikiAttachment a) {
    return new WikiAttachmentDTO(
        a.getId(),
        a.getPageId(),
        a.getFileName(),
        a.getContentType(),
        a.getFileSize(),
        a.getUploadedBy(),
        a.getCreatedAt());
  }
}
