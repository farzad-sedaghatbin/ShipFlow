package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.DocumentUploadResponse;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.UploadedDocumentRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
      "jpg", "jpeg", "png", "gif", "webp", "svg", "mp4", "webm", "mov", "avi"
  );

  private final UploadedDocumentRepository documentRepository;
  private final LocalizationService localizationService;

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

      // Sanitize original filename to prevent path traversal
      String sanitizedFileName = sanitizeFileName(originalFileName);

      // Generate unique file name
      String uniqueFileName = UUID.randomUUID().toString() + "_" + sanitizedFileName;

      // Save file to disk
      Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
      Files.createDirectories(uploadPath);
      Path filePath = uploadPath.resolve(uniqueFileName);

      // Verify the resolved path is still within the upload directory (additional
      // security check)
      if (!filePath.normalize().startsWith(uploadPath)) {
        throw new SecurityException("Invalid file path detected");
      }

      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

      // Extract text from document
      String extractedText = extractText(file.getInputStream(), fileType);
      boolean textExtracted = extractedText != null && !extractedText.isEmpty();

      // Save document metadata to database
      UploadedDocument document = UploadedDocument.builder().fileName(uniqueFileName)
          .originalFileName(originalFileName).fileType(fileType).fileSize(file.getSize())
          .storagePath(uniqueFileName) // Store only filename, not absolute path
          .extractedText(extractedText).textExtracted(textExtracted)
          .entityType(entityType).entityId(entityId).uploaderId(uploaderId).uploaderUsername(uploaderUsername)
          .indexedForQA(false).build();

      document = documentRepository.save(document);

      // Index for Q&A if text was extracted and knowledge service is available
      if (textExtracted && knowledgeIngestionService != null) {
        try {
          knowledgeIngestionService.ingestDocument(document);
          document.setIndexedForQA(true);
          documentRepository.save(document);
        } catch (Exception e) {
          log.warn("Failed to index document for Q&A: {}", e.getMessage());
        }
      }

      log.info("Document uploaded successfully: {} ({})", originalFileName, fileType);

      return DocumentUploadResponse.builder().id(document.getId()).fileName(originalFileName).fileType(fileType)
          .fileSize(file.getSize()).extractedText(textExtracted ? extractedText : null)
          .storagePath(uniqueFileName).textExtracted(textExtracted).build();

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

      // Sanitize original filename to prevent path traversal
      String sanitizedFileName = sanitizeFileName(originalFileName);

      // Generate unique file name
      String uniqueFileName = UUID.randomUUID().toString() + "_" + sanitizedFileName;

      // Save file to disk
      Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
      Files.createDirectories(uploadPath);
      Path filePath = uploadPath.resolve(uniqueFileName);

      // Verify the resolved path is still within the upload directory
      if (!filePath.normalize().startsWith(uploadPath)) {
        throw new SecurityException("Invalid file path detected");
      }

      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

      // Save document metadata to database (no text extraction for media)
      UploadedDocument document = UploadedDocument.builder()
          .fileName(uniqueFileName)
          .originalFileName(originalFileName)
          .fileType(fileType)
          .fileSize(file.getSize())
          .storagePath(uniqueFileName)
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
          .storagePath(uniqueFileName)
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

    // Delete file from disk
    try {
      Path filePath = Paths.get(document.getStoragePath());
      Files.deleteIfExists(filePath);
    } catch (IOException e) {
      log.warn("Failed to delete file from disk: {}", e.getMessage());
    }

    documentRepository.delete(document);
  }

  /** Download a document. */
  public ResponseEntity<Resource> downloadDocument(Long id) {
    UploadedDocument document = getDocumentById(id);

    try {
      // Construct full path from upload directory and stored filename
      Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
      Path filePath = uploadPath.resolve(document.getStoragePath()).normalize();
      
      // Security check: ensure resolved path is within upload directory
      if (!filePath.startsWith(uploadPath)) {
        log.error("Security violation: Attempted path traversal for document ID {}: {}", id, document.getStoragePath());
        throw new ResourceNotFoundException(
            localizationService.getMessage("document.not.found", document.getOriginalFileName()));
      }
      
      Resource resource = new UrlResource(filePath.toUri());

      if (!resource.exists() || !resource.isReadable()) {
        log.warn("Document file not found or not readable. Document ID: {}, Original name: {}, Storage path: {}, Resolved path: {}", 
            id, document.getOriginalFileName(), document.getStoragePath(), filePath);
        throw new ResourceNotFoundException(
            localizationService.getMessage("document.not.found", document.getOriginalFileName()));
      }

      // Determine content type
      String contentType = determineContentType(document.getFileType());

      return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
          .header(HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"" + document.getOriginalFileName() + "\"")
          .body(resource);

    } catch (MalformedURLException e) {
      log.error("Malformed URL for document: {}", document.getOriginalFileName(), e);
      throw new ResourceNotFoundException(
          localizationService.getMessage("document.read.error", e.getMessage()), e);
    }
  }

  /**
   * Read the raw bytes of a stored document. Applies the same path-traversal guard as
   * {@link #downloadDocument(Long)}. Used by non-HTTP consumers (e.g. the MCP server) that need the
   * file content in-memory rather than as a streamed HTTP resource.
   */
  public byte[] getDocumentBytes(Long id) {
    UploadedDocument document = getDocumentById(id);
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
   * Sanitizes a filename to prevent path traversal attacks. Removes all path
   * separators and only keeps the filename portion.
   */
  private String sanitizeFileName(String filename) {
    if (filename == null || filename.isEmpty()) {
      return "unnamed";
    }

    // Get just the filename, removing any path components
    String name = Paths.get(filename).getFileName().toString();

    // Remove any remaining path traversal sequences and null bytes
    name = name.replaceAll("\\.\\.", "").replaceAll("[\\/\\\\]", "").replaceAll("\\x00", "").trim();

    // If nothing is left after sanitization, use a default name
    if (name.isEmpty()) {
      return "unnamed";
    }

    return name;
  }
}
