package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.DocumentUploadResponse;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.repository.UploadedDocumentRepository;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling document uploads and text extraction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final UploadedDocumentRepository documentRepository;

    @Autowired(required = false)
    private KnowledgeIngestionService knowledgeIngestionService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-size:10485760}") // 10MB default
    private long maxFileSize;

    /**
     * Upload a document and extract its text content.
     */
    @Transactional
    public DocumentUploadResponse uploadDocument(MultipartFile file, String entityType, Long entityId,
                                                  Long uploaderId, String uploaderUsername) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return DocumentUploadResponse.builder()
                        .textExtracted(false)
                        .errorMessage("File is empty")
                        .build();
            }

            if (file.getSize() > maxFileSize) {
                return DocumentUploadResponse.builder()
                        .textExtracted(false)
                        .errorMessage("File size exceeds maximum allowed size")
                        .build();
            }

            String originalFileName = file.getOriginalFilename();
            String fileType = getFileType(originalFileName);

            // Validate file type
            if (!isAllowedFileType(fileType)) {
                return DocumentUploadResponse.builder()
                        .fileName(originalFileName)
                        .fileType(fileType)
                        .textExtracted(false)
                        .errorMessage("Unsupported file type. Allowed: PDF, DOCX, DOC, TXT, MD")
                        .build();
            }

            // Generate unique file name
            String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

            // Save file to disk
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Extract text from document
            String extractedText = extractText(file.getInputStream(), fileType);
            boolean textExtracted = extractedText != null && !extractedText.isEmpty();

            // Save document metadata to database
            UploadedDocument document = UploadedDocument.builder()
                    .fileName(uniqueFileName)
                    .originalFileName(originalFileName)
                    .fileType(fileType)
                    .fileSize(file.getSize())
                    .storagePath(filePath.toString())
                    .extractedText(extractedText)
                    .textExtracted(textExtracted)
                    .entityType(entityType)
                    .entityId(entityId)
                    .uploaderId(uploaderId)
                    .uploaderUsername(uploaderUsername)
                    .indexedForQA(false)
                    .build();

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

            return DocumentUploadResponse.builder()
                    .id(document.getId())
                    .fileName(originalFileName)
                    .fileType(fileType)
                    .fileSize(file.getSize())
                    .extractedText(textExtracted ? extractedText : null)
                    .storagePath(uniqueFileName)
                    .textExtracted(textExtracted)
                    .build();

        } catch (Exception e) {
            log.error("Error uploading document: {}", e.getMessage(), e);
            return DocumentUploadResponse.builder()
                    .textExtracted(false)
                    .errorMessage("Error uploading document: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Extract text from a document based on its file type.
     */
    public String extractText(InputStream inputStream, String fileType) {
        try {
            switch (fileType.toLowerCase()) {
                case "pdf":
                    return extractTextFromPdf(inputStream);
                case "docx":
                    return extractTextFromDocx(inputStream);
                case "doc":
                    return extractTextFromDoc(inputStream);
                case "txt":
                case "md":
                case "markdown":
                    return extractTextFromPlainText(inputStream);
                default:
                    log.warn("Unsupported file type for text extraction: {}", fileType);
                    return null;
            }
        } catch (Exception e) {
            log.error("Error extracting text from {} file: {}", fileType, e.getMessage());
            return null;
        }
    }

    /**
     * Extract text from a MultipartFile without saving it.
     * Useful for extracting pitch data before creating a pitch.
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
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
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
        return fileType != null && 
               (fileType.equals("pdf") || fileType.equals("docx") || 
                fileType.equals("doc") || fileType.equals("txt") || 
                fileType.equals("md") || fileType.equals("markdown"));
    }

    /**
     * Get all documents for an entity.
     */
    public List<UploadedDocument> getDocumentsByEntity(String entityType, Long entityId) {
        return documentRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Get document by ID.
     */
    public UploadedDocument getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
    }

    /**
     * Delete a document.
     */
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

    /**
     * Download a document.
     */
    public ResponseEntity<Resource> downloadDocument(Long id) {
        UploadedDocument document = getDocumentById(id);
        
        try {
            Path filePath = Paths.get(document.getStoragePath());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found or not readable: " + document.getOriginalFileName());
            }
            
            // Determine content type
            String contentType = determineContentType(document.getFileType());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                    .body(resource);
                    
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file: " + e.getMessage(), e);
        }
    }

    private String determineContentType(String fileType) {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc" -> "application/msword";
            case "txt" -> "text/plain";
            case "md" -> "text/markdown";
            default -> "application/octet-stream";
        };
    }

    /**
     * Get documents that haven't been indexed for Q&A.
     */
    public List<UploadedDocument> getPendingDocuments() {
        return documentRepository.findByIndexedForQAFalse();
    }

    /**
     * Index pending documents for Q&A.
     */
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
}
