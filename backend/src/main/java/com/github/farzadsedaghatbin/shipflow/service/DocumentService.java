package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.DocumentUploadResponse;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.event.DocumentUploadedEvent;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.UploadedDocumentRepository;
import com.github.farzadsedaghatbin.shipflow.service.storage.DownloadResource;
import com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageService;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageKeyGenerator;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import com.github.farzadsedaghatbin.shipflow.service.storage.StoredObjectRef;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Service for handling document uploads and text extraction. */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

  /**
   * Mapping of file extensions to MIME content types. Centralized to maintain
   * consistency across the codebase.
   */
  private static final Map<String, String> CONTENT_TYPE_MAP = Map.ofEntries(
      // Documents
      Map.entry("pdf", "application/pdf"),
      Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
      Map.entry("doc", "application/msword"),
      Map.entry("txt", "text/plain"),
      Map.entry("md", "text/markdown"),
      // Images
      Map.entry("jpg", "image/jpeg"),
      Map.entry("jpeg", "image/jpeg"),
      Map.entry("png", "image/png"),
      Map.entry("gif", "image/gif"),
      Map.entry("webp", "image/webp"),
      Map.entry("svg", "image/svg+xml"),
      // Videos
      Map.entry("mp4", "video/mp4"),
      Map.entry("webm", "video/webm"),
      Map.entry("mov", "video/quicktime"),
      Map.entry("avi", "video/x-msvideo")
  );

  /**
   * Allowed media file extensions for bug report attachments.
   */
  private static final java.util.Set<String> ALLOWED_MEDIA_TYPES = java.util.Set.of(
      "jpg", "jpeg", "png", "gif", "webp", "svg", "mp4", "webm", "mov", "avi", "txt"
  );

  private final UploadedDocumentRepository documentRepository;
  private final LocalizationService localizationService;
  private final ObjectStorageService objectStorageService;
  private final org.springframework.context.ApplicationEventPublisher eventPublisher;

  /**
   * Object-storage key prefix for an uploaded document/attachment, grouped by category. Shared with
   * {@link StorageMigrationService} so migrated objects land under the same structure (single source
   * of truth — no drift).
   */
  public static String storageKeyHint(String entityType, Long entityId) {
    String type = entityType == null ? "misc" : entityType.trim().toUpperCase();
    if ("BUG_REPORT".equals(type)) {
      return "attachments/bug/" + entityId;
    }
    return "documents/" + type.toLowerCase() + "/" + entityId;
  }

  @Autowired(required = false)
  private KnowledgeIngestionService knowledgeIngestionService;

  @Value("${app.upload.dir:uploads}")
  private String uploadDir;

  @Value("${app.upload.max-size:10485760}") // 10MB default
  private long maxFileSize;

  /** Upload a document and extract its text content. */
  @Transactional
  public DocumentUploadResponse uploadDocument(MultipartFile file, String entityType, Long entityId, Long uploaderId,
      String uploaderUsername) {
    try {
      // Validate file
      if (file.isEmpty()) {
        return DocumentUploadResponse.builder().textExtracted(false).errorMessage("File is empty").build();
      }

      if (file.getSize() > maxFileSize) {
        return DocumentUploadResponse.builder().textExtracted(false)
            .errorMessage("File size exceeds maximum allowed size").build();
      }

      String originalFileName = file.getOriginalFilename();
      String fileType = getFileType(originalFileName);

      // Validate file type
      if (!isAllowedFileType(fileType)) {
        return DocumentUploadResponse.builder().fileName(originalFileName).fileType(fileType)
            .textExtracted(false).errorMessage("Unsupported file type. Allowed: PDF, DOCX, DOC, TXT, MD")
            .build();
      }

      // Buffer the bytes once: the stream is consumed by BOTH the storage backend and text
      // extraction, so we cannot reuse a single InputStream for both.
      byte[] bytes = file.getBytes();
      String contentType = resolveContentType(file, fileType);

      // Persist through the object-storage SPI (LOCAL_FS / S3 / MinIO). DocumentService has
      // already enforced its own size + extension policy above, so we use the non-validating
      // entry point — the façade's stricter allowlist would otherwise reject some doc types.
      StoredObjectRef ref = objectStorageService.storeWithoutValidation(
          storageKeyHint(entityType, entityId), originalFileName, contentType, bytes.length,
          new ByteArrayInputStream(bytes));

      // Extract text from document (separate stream — the store consumed its own copy)
      String extractedText = extractText(new ByteArrayInputStream(bytes), fileType);
      boolean textExtracted = extractedText != null && !extractedText.isEmpty();

      // Save document metadata to database. storagePath is left null for new rows; reads/deletes
      // resolve via storageProvider + storageKey.
      UploadedDocument document = UploadedDocument.builder()
          .fileName(StorageKeyGenerator.sanitize(originalFileName))
          .originalFileName(originalFileName).fileType(fileType).fileSize(file.getSize())
          .storageProvider(objectStorageService.activeProvider()).storageKey(ref.getKey())
          .extractedText(extractedText).textExtracted(textExtracted)
          .entityType(entityType).entityId(entityId).uploaderId(uploaderId).uploaderUsername(uploaderUsername)
          .indexedForQA(false).build();

      document = documentRepository.save(document);

      // Index for Q&A in the BACKGROUND, after this upload transaction commits. Embedding
      // generation is slow (a large PDF can take a minute+); running it inline blocked the upload
      // response so long the UI appeared frozen — the file was already in storage but nothing came
      // back. DocumentKnowledgeListener handles the event off the request thread.
      if (textExtracted) {
        eventPublisher.publishEvent(new DocumentUploadedEvent(document.getId()));
      }

      log.info("Document uploaded successfully: {} ({})", originalFileName, fileType);

      return DocumentUploadResponse.builder().id(document.getId()).fileName(originalFileName).fileType(fileType)
          .fileSize(file.getSize()).extractedText(textExtracted ? extractedText : null)
          .storagePath(ref.getKey()).textExtracted(textExtracted).build();

    } catch (Exception e) {
      log.error("Error uploading document: {}", e.getMessage(), e);
      return DocumentUploadResponse.builder().textExtracted(false)
          .errorMessage("Error uploading document: " + e.getMessage()).build();
    }
  }

  /**
   * Upload a media attachment (image/video) for bug reports.
   * Does not extract text or index for Q&A.
   *
   * @param file              The media file to upload
   * @param entityType        The entity type (e.g., "BUG_REPORT")
   * @param entityId          The entity ID
   * @param uploaderId        The uploader user ID
   * @param uploaderUsername  The uploader username
   * @param maxMediaSize      Maximum allowed file size (0 for default)
   * @return Upload response with file details
   */
  @Transactional
  public DocumentUploadResponse uploadMediaAttachment(MultipartFile file, String entityType, Long entityId,
      Long uploaderId, String uploaderUsername, long maxMediaSize) {
    try {
      // Validate file
      if (file.isEmpty()) {
        return DocumentUploadResponse.builder().textExtracted(false).errorMessage("File is empty").build();
      }

      long effectiveMaxSize = maxMediaSize > 0 ? maxMediaSize : maxFileSize * 5; // 50MB default for media
      if (file.getSize() > effectiveMaxSize) {
        return DocumentUploadResponse.builder().textExtracted(false)
            .errorMessage("File size exceeds maximum allowed size (" + (effectiveMaxSize / 1024 / 1024) + "MB)")
            .build();
      }

      String originalFileName = file.getOriginalFilename();
      String fileType = getFileType(originalFileName);

      // Validate file type for media
      if (!isAllowedMediaType(fileType)) {
        return DocumentUploadResponse.builder().fileName(originalFileName).fileType(fileType)
            .textExtracted(false)
            .errorMessage("Unsupported media file type. Allowed: JPG, JPEG, PNG, GIF, WEBP, SVG, MP4, WEBM, MOV, AVI")
            .build();
      }

      String contentType = resolveContentType(file, fileType);

      // Persist through the object-storage SPI. Media (images + video, up to ~50MB) exceeds the
      // façade's image/PDF/doc allowlist and 10MB limit, so we use the non-validating entry point;
      // the size + media-type policy has already been enforced above.
      StoredObjectRef ref = objectStorageService.storeWithoutValidation(
          storageKeyHint(entityType, entityId), originalFileName, contentType, file.getSize(),
          file.getInputStream());

      // Save document metadata to database (no text extraction for media). storagePath is left
      // null for new rows; reads/deletes resolve via storageProvider + storageKey.
      UploadedDocument document = UploadedDocument.builder()
          .fileName(StorageKeyGenerator.sanitize(originalFileName))
          .originalFileName(originalFileName)
          .fileType(fileType)
          .fileSize(file.getSize())
          .storageProvider(objectStorageService.activeProvider())
          .storageKey(ref.getKey())
          .extractedText(null)
          .textExtracted(false)
          .entityType(entityType)
          .entityId(entityId)
          .uploaderId(uploaderId)
          .uploaderUsername(uploaderUsername)
          .indexedForQA(false)
          .build();

      document = documentRepository.save(document);

      log.info("Media attachment uploaded successfully: {} ({})", originalFileName, fileType);

      return DocumentUploadResponse.builder()
          .id(document.getId())
          .fileName(originalFileName)
          .fileType(fileType)
          .fileSize(file.getSize())
          .storagePath(ref.getKey())
          .textExtracted(false)
          .build();

    } catch (Exception e) {
      log.error("Error uploading media attachment: {}", e.getMessage(), e);
      return DocumentUploadResponse.builder()
          .textExtracted(false)
          .errorMessage("Error uploading media attachment: " + e.getMessage())
          .build();
    }
  }

  /**
   * Check if a file type is an allowed media type for bug attachments.
   */
  private boolean isAllowedMediaType(String fileType) {
    return fileType != null && ALLOWED_MEDIA_TYPES.contains(fileType.toLowerCase());
  }

  /**
   * Fix storage paths for existing documents that have absolute paths.
   * This method extracts just the filename from absolute paths.
   * Can be called on application startup or as an admin endpoint.
   */
  @Transactional
  public int fixDocumentStoragePaths() {
    int fixedCount = 0;
    List<UploadedDocument> allDocuments = documentRepository.findAll();
    
    for (UploadedDocument document : allDocuments) {
      String storagePath = document.getStoragePath();
      
      // Check if storage path contains directory separators (indicating absolute path)
      if (storagePath != null && (storagePath.contains("/") || storagePath.contains("\\"))) {
        // Extract just the filename
        Path path = Paths.get(storagePath);
        String filename = path.getFileName().toString();
        
        log.info("Fixing storage path for document ID {}: {} -> {}", 
            document.getId(), storagePath, filename);
        
        document.setStoragePath(filename);
        documentRepository.save(document);
        fixedCount++;
      }
    }
    
    log.info("Fixed storage paths for {} documents", fixedCount);
    return fixedCount;
  }

  /** Extract text from a document based on its file type. */
  public String extractText(InputStream inputStream, String fileType) {
    try {
      switch (fileType.toLowerCase()) {
        case "pdf" :
          return extractTextFromPdf(inputStream);
        case "docx" :
          return extractTextFromDocx(inputStream);
        case "doc" :
          return extractTextFromDoc(inputStream);
        case "txt" :
        case "md" :
        case "markdown" :
          return extractTextFromPlainText(inputStream);
        default :
          log.warn("Unsupported file type for text extraction: {}", fileType);
          return null;
      }
    } catch (Exception e) {
      log.error("Error extracting text from {} file: {}", fileType, e.getMessage());
      return null;
    }
  }

  /**
   * Extract text from a MultipartFile without saving it. Useful for extracting
   * pitch data before creating a pitch.
   */
  public String extractTextFromFile(MultipartFile file) {
    try {
      if (file.isEmpty()) {
        return null;
      }

      String originalFileName = file.getOriginalFilename();
      String fileType = getFileType(originalFileName);

      if (!isAllowedFileType(fileType)) {
        log.warn("Unsupported file type: {}", fileType);
        return null;
      }

      return extractText(file.getInputStream(), fileType);
    } catch (Exception e) {
      log.error("Error extracting text from file: {}", e.getMessage());
      return null;
    }
  }

  private String extractTextFromPdf(InputStream inputStream) throws IOException {
    byte[] bytes = inputStream.readAllBytes();
    try (PDDocument document = Loader.loadPDF(bytes)) {
      PDFTextStripper stripper = new PDFTextStripper();
      return stripper.getText(document).trim();
    }
  }

  private String extractTextFromDocx(InputStream inputStream) throws IOException {
    try (XWPFDocument document = new XWPFDocument(inputStream)) {
      StringBuilder text = new StringBuilder();
      for (XWPFParagraph paragraph : document.getParagraphs()) {
        text.append(paragraph.getText()).append("\n");
      }
      return text.toString().trim();
    }
  }

  private String extractTextFromDoc(InputStream inputStream) throws IOException {
    // For .doc files, we'd need Apache POI HWPF, but DOCX is more common
    // For now, treat as plain text which may not work well
    log.warn("Legacy .doc format may not be fully supported");
    return extractTextFromPlainText(inputStream);
  }

  private String extractTextFromPlainText(InputStream inputStream) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.joining("\n")).trim();
    }
  }

  private String getFileType(String fileName) {
    if (fileName == null || !fileName.contains(".")) {
      return "unknown";
    }
    return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
  }

  private boolean isAllowedFileType(String fileType) {
    return fileType != null && (fileType.equals("pdf") || fileType.equals("docx") || fileType.equals("doc")
        || fileType.equals("txt") || fileType.equals("md") || fileType.equals("markdown"));
  }

  /** Get all documents for an entity. */
  public List<UploadedDocument> getDocumentsByEntity(String entityType, Long entityId) {
    return documentRepository.findByEntityTypeAndEntityId(entityType, entityId);
  }

  /** Get document by ID. */
  public UploadedDocument getDocumentById(Long id) {
    return documentRepository.findById(id).orElseThrow(() -> new RuntimeException("Document not found: " + id));
  }

  /** Delete a document. */
  @Transactional
  public void deleteDocument(Long id) {
    UploadedDocument document = getDocumentById(id);

    if (document.getStorageKey() != null && !document.getStorageKey().isBlank()) {
      // New row: best-effort delete through the object-storage SPI.
      try {
        StorageProviderType provider = document.getStorageProvider() != null
            ? document.getStorageProvider() : StorageProviderType.LOCAL_FS;
        objectStorageService.delete(provider, document.getStorageKey());
      } catch (Exception e) {
        log.warn("Failed to delete object from storage backend: {}", e.getMessage());
      }
    } else if (document.getStoragePath() != null) {
      // Legacy row: delete the file directly from disk.
      try {
        Path filePath = Paths.get(document.getStoragePath());
        Files.deleteIfExists(filePath);
      } catch (IOException e) {
        log.warn("Failed to delete file from disk: {}", e.getMessage());
      }
    }

    documentRepository.delete(document);
  }

  /** Download a document. */
  public ResponseEntity<Resource> downloadDocument(Long id) {
    UploadedDocument document = getDocumentById(id);
    String contentType = determineContentType(document.getFileType());

    Resource resource;
    long contentLength = -1;
    if (document.getStorageKey() != null && !document.getStorageKey().isBlank()) {
      // New row: stream from the object-storage SPI.
      StorageProviderType provider = document.getStorageProvider() != null
          ? document.getStorageProvider() : StorageProviderType.LOCAL_FS;
      try {
        DownloadResource dr = objectStorageService.retrieve(provider, document.getStorageKey());
        resource = new InputStreamResource(dr.getStream());
        contentLength = dr.getSizeBytes();
      } catch (ResourceNotFoundException e) {
        throw e;
      } catch (Exception e) {
        log.warn("Failed to retrieve document ID {} from storage: {}", id, e.getMessage());
        throw new ResourceNotFoundException(
            localizationService.getMessage("document.not.found", document.getOriginalFileName()));
      }
    } else {
      // Legacy row: serve from the local filesystem (storagePath fallback).
      resource = legacyDiskResource(id, document);
    }

    ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + document.getOriginalFileName() + "\"");
    // InputStreamResource.contentLength() always returns -1, so Content-Length must be set
    // explicitly for object-storage-backed rows (UrlResource in the legacy branch already
    // reports its real length automatically). Browsers need Content-Length to compute video
    // duration/seekability — without it a video attachment appears stuck at 0:00.
    if (contentLength >= 0) {
      builder = builder.contentLength(contentLength);
    }
    return builder.body(resource);
  }

  /**
   * Resolves a legacy document on the local filesystem, applying a path-traversal guard. Used only
   * for rows uploaded before the object-storage SPI migration (storageKey null).
   */
  private Resource legacyDiskResource(Long id, UploadedDocument document) {
    try {
      Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
      Path filePath = uploadPath.resolve(document.getStoragePath()).normalize();

      // Security check: ensure resolved path is within upload directory
      if (!filePath.startsWith(uploadPath)) {
        log.error("Security violation: Attempted path traversal for document ID {}: {}", id,
            document.getStoragePath());
        throw new ResourceNotFoundException(
            localizationService.getMessage("document.not.found", document.getOriginalFileName()));
      }

      Resource resource = new UrlResource(filePath.toUri());

      if (!resource.exists() || !resource.isReadable()) {
        log.warn("Document file not found or not readable. Document ID: {}, Original name: {}, "
            + "Storage path: {}, Resolved path: {}", id, document.getOriginalFileName(),
            document.getStoragePath(), filePath);
        throw new ResourceNotFoundException(
            localizationService.getMessage("document.not.found", document.getOriginalFileName()));
      }
      return resource;
    } catch (MalformedURLException e) {
      log.error("Malformed URL for document: {}", document.getOriginalFileName(), e);
      throw new ResourceNotFoundException(
          localizationService.getMessage("document.read.error", e.getMessage()), e);
    }
  }

  /**
   * Read the raw bytes of a stored document. Routes through the object-storage SPI for new rows and
   * falls back to the local filesystem (with a path-traversal guard) for legacy rows. Used by
   * non-HTTP consumers (e.g. the MCP server) that need the file content in-memory rather than as a
   * streamed HTTP resource.
   */
  public byte[] getDocumentBytes(Long id) {
    UploadedDocument document = getDocumentById(id);

    if (document.getStorageKey() != null && !document.getStorageKey().isBlank()) {
      StorageProviderType provider = document.getStorageProvider() != null
          ? document.getStorageProvider() : StorageProviderType.LOCAL_FS;
      try (InputStream in = objectStorageService.retrieve(provider, document.getStorageKey()).getStream()) {
        return in.readAllBytes();
      } catch (ResourceNotFoundException e) {
        throw e;
      } catch (Exception e) {
        throw new ResourceNotFoundException(
            localizationService.getMessage("document.read.error", e.getMessage()), e);
      }
    }

    // Legacy row: read from the local filesystem with a path-traversal guard.
    Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    Path filePath = uploadPath.resolve(document.getStoragePath()).normalize();
    if (!filePath.startsWith(uploadPath)) {
      log.error("Security violation: Attempted path traversal for document ID {}: {}", id,
          document.getStoragePath());
      throw new ResourceNotFoundException(
          localizationService.getMessage("document.not.found", document.getOriginalFileName()));
    }
    try {
      return Files.readAllBytes(filePath);
    } catch (IOException e) {
      throw new ResourceNotFoundException(
          localizationService.getMessage("document.read.error", e.getMessage()), e);
    }
  }

  /**
   * Returns the MIME content type for a file extension (e.g. "png" → "image/png"), or
   * "application/octet-stream" if unknown. Public so non-HTTP consumers (MCP server) can label
   * binary content.
   */
  public String getContentType(String fileType) {
    return determineContentType(fileType);
  }

  /**
   * Determines the MIME content type based on file extension.
   *
   * @param fileType
   *            The file extension (e.g., "pdf", "docx")
   * @return The MIME content type, or "application/octet-stream" if unknown
   */
  private String determineContentType(String fileType) {
    return CONTENT_TYPE_MAP.getOrDefault(fileType.toLowerCase(), "application/octet-stream");
  }

  /** Link an existing document to an entity. */
  @Transactional
  public void linkDocumentToEntity(Long documentId, String entityType, Long entityId) {
    UploadedDocument document = getDocumentById(documentId);
    document.setEntityType(entityType);
    document.setEntityId(entityId);
    documentRepository.save(document);
  }

  /** Get documents that haven't been indexed for Q&A. */
  public List<UploadedDocument> getPendingDocuments() {
    return documentRepository.findByIndexedForQAFalse();
  }

  /** Index pending documents for Q&A. */
  @Transactional
  public int indexPendingDocuments() {
    if (knowledgeIngestionService == null) {
      return 0;
    }

    List<UploadedDocument> pendingDocs = getPendingDocuments();
    int indexed = 0;

    for (UploadedDocument doc : pendingDocs) {
      if (doc.isTextExtracted() && doc.getExtractedText() != null) {
        try {
          knowledgeIngestionService.ingestDocument(doc);
          doc.setIndexedForQA(true);
          documentRepository.save(doc);
          indexed++;
        } catch (Exception e) {
          log.warn("Failed to index document {}: {}", doc.getId(), e.getMessage());
        }
      }
    }

    return indexed;
  }

  /**
   * Resolves the MIME content type to record against a stored object. Prefers the type declared by
   * the upload client; falls back to mapping the file extension (covers documents, images, and
   * video) and finally {@code application/octet-stream}.
   */
  private String resolveContentType(MultipartFile file, String fileType) {
    String ct = file.getContentType();
    if (ct != null && !ct.isBlank()) {
      return ct;
    }
    return fileType == null
        ? "application/octet-stream"
        : CONTENT_TYPE_MAP.getOrDefault(fileType.toLowerCase(), "application/octet-stream");
  }
}
