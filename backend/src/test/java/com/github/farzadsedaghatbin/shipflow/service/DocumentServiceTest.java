package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.DocumentUploadResponse;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.repository.UploadedDocumentRepository;
import com.github.farzadsedaghatbin.shipflow.service.storage.DownloadResource;
import com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageService;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import com.github.farzadsedaghatbin.shipflow.service.storage.StoredObjectRef;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for DocumentService. */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

  @Mock
  private UploadedDocumentRepository documentRepository;

  @Mock
  private KnowledgeIngestionService knowledgeIngestionService;

  @Mock
  private LocalizationService localizationService;

  @Mock
  private ObjectStorageService objectStorageService;

  @Mock
  private org.springframework.context.ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private DocumentService documentService;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    lenient().when(localizationService.getMessage(anyString(), any(Object[].class))).thenAnswer(i -> {
      String key = i.getArgument(0);
      if (key.contains("file.not.found"))
        return "File not found or not readable";
      if (key.contains("document.not.found"))
        return "File not found or not readable";
      return key;
    });
    lenient().when(localizationService.getMessage(anyString())).thenAnswer(i -> {
      String key = i.getArgument(0);
      if (key.contains("file.not.found"))
        return "File not found or not readable";
      if (key.contains("document.not.found"))
        return "File not found or not readable";
      return key;
    });

    ReflectionTestUtils.setField(documentService, "uploadDir", tempDir.toString());
    ReflectionTestUtils.setField(documentService, "maxFileSize", 10485760L); // 10MB
  }

  private StoredObjectRef storedRef(String key) {
    return StoredObjectRef.builder().key(key).bucket("bucket").contentType("application/pdf")
        .sizeBytes(10L).build();
  }

  @Test
  void uploadDocument_withPdfFile_shouldExtractTextAndSave() throws IOException {
    // Given
    String fileName = "test-document.pdf";
    byte[] content = "PDF content".getBytes();
    MockMultipartFile file = new MockMultipartFile("file", fileName, "application/pdf", content);

    when(objectStorageService.activeProvider()).thenReturn(StorageProviderType.LOCAL_FS);
    when(objectStorageService.storeWithoutValidation(eq("documents/pitch/1"), eq(fileName), any(), anyLong(), any()))
        .thenReturn(storedRef("documents/pitch/1/uuid_" + fileName));

    UploadedDocument savedDoc = UploadedDocument.builder().id(1L).fileName("uuid_" + fileName)
        .originalFileName(fileName).fileType("pdf").fileSize((long) content.length).textExtracted(true).build();

    when(documentRepository.save(any(UploadedDocument.class))).thenReturn(savedDoc);

    // When
    DocumentUploadResponse response = documentService.uploadDocument(file, "PITCH", 1L, 1L, "testuser");

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getFileName()).contains(fileName);
    assertThat(response.getFileType()).isEqualTo("pdf");
    verify(documentRepository).save(any(UploadedDocument.class));
  }

  @Test
  void uploadDocument_shouldRouteToObjectStorageAndPersistStorageKey() throws IOException {
    // Given
    String fileName = "notes.txt";
    byte[] content = "hello world".getBytes();
    MockMultipartFile file = new MockMultipartFile("file", fileName, "text/plain", content);

    when(objectStorageService.activeProvider()).thenReturn(StorageProviderType.S3);
    when(objectStorageService.storeWithoutValidation(eq("documents/pitch/1"), eq(fileName), any(), anyLong(), any()))
        .thenReturn(storedRef("documents/pitch/1/uuid_notes.txt"));
    when(documentRepository.save(any(UploadedDocument.class))).thenAnswer(i -> i.getArgument(0));

    // When
    documentService.uploadDocument(file, "PITCH", 1L, 1L, "testuser");

    // Then — file is stored through the SPI (never written to disk) and the key is persisted
    verify(objectStorageService).storeWithoutValidation(eq("documents/pitch/1"), eq(fileName), any(), anyLong(), any());
    verify(documentRepository).save(argThat(doc -> "documents/pitch/1/uuid_notes.txt".equals(doc.getStorageKey())
        && doc.getStorageProvider() == StorageProviderType.S3 && doc.getStoragePath() == null));
  }

  @Test
  void uploadDocument_defersQaIndexingToBackgroundEvent() throws IOException {
    // Given a text document that extracts successfully
    MockMultipartFile file =
        new MockMultipartFile("file", "notes.txt", "text/plain", "indexable text content".getBytes());
    when(objectStorageService.activeProvider()).thenReturn(StorageProviderType.S3);
    when(objectStorageService.storeWithoutValidation(any(), any(), any(), anyLong(), any()))
        .thenReturn(storedRef("documents/pitch/1/uuid_notes.txt"));
    when(documentRepository.save(any(UploadedDocument.class))).thenAnswer(i -> {
      UploadedDocument d = i.getArgument(0);
      if (d.getId() == null) d.setId(99L);
      return d;
    });

    // When
    documentService.uploadDocument(file, "PITCH", 1L, 1L, "testuser");

    // Then — indexing is NOT run inline (it would block the upload response on slow embeddings);
    // instead an event is published for the async AFTER_COMMIT listener.
    verify(knowledgeIngestionService, never()).ingestDocument(any());
    verify(eventPublisher)
        .publishEvent(any(com.github.farzadsedaghatbin.shipflow.event.DocumentUploadedEvent.class));
  }

  @Test
  void uploadMediaAttachment_shouldRouteToObjectStorageWithBugAttachmentsKeyHint() throws IOException {
    // Given a video — exceeds the façade allowlist, so it must use the non-validating store path
    String fileName = "clip.mp4";
    byte[] content = new byte[1024];
    MockMultipartFile file = new MockMultipartFile("file", fileName, "video/mp4", content);

    when(objectStorageService.activeProvider()).thenReturn(StorageProviderType.LOCAL_FS);
    when(objectStorageService.storeWithoutValidation(eq("attachments/bug/7"), eq(fileName), any(), anyLong(), any()))
        .thenReturn(storedRef("attachments/bug/7/uuid_clip.mp4"));
    when(documentRepository.save(any(UploadedDocument.class))).thenAnswer(i -> i.getArgument(0));

    // When
    DocumentUploadResponse response =
        documentService.uploadMediaAttachment(file, "BUG_REPORT", 7L, 1L, "testuser", 0);

    // Then
    assertThat(response.getErrorMessage()).isNull();
    verify(objectStorageService).storeWithoutValidation(eq("attachments/bug/7"), eq(fileName), any(), anyLong(), any());
    verify(documentRepository).save(argThat(doc -> "attachments/bug/7/uuid_clip.mp4".equals(doc.getStorageKey())
        && doc.getStoragePath() == null));
  }

  @Test
  void storageKeyHint_groupsByCategory() {
    assertThat(DocumentService.storageKeyHint("PITCH", 5L)).isEqualTo("documents/pitch/5");
    assertThat(DocumentService.storageKeyHint("MEETING", 9L)).isEqualTo("documents/meeting/9");
    assertThat(DocumentService.storageKeyHint("BUG_REPORT", 7L)).isEqualTo("attachments/bug/7");
    assertThat(DocumentService.storageKeyHint(null, 3L)).isEqualTo("documents/misc/3");
  }

  @Test
  void uploadDocument_withEmptyFile_shouldReturnError() {
    // Given
    MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

    // When
    DocumentUploadResponse response = documentService.uploadDocument(emptyFile, "PITCH", 1L, 1L, "testuser");

    // Then
    assertThat(response).isNotNull();
    assertThat(response.isTextExtracted()).isFalse();
    assertThat(response.getErrorMessage()).contains("File is empty");
    verify(documentRepository, never()).save(any());
  }

  @Test
  void uploadDocument_withUnsupportedFileType_shouldReturnError() {
    // Given
    MockMultipartFile file = new MockMultipartFile("file", "test.exe", "application/x-executable",
        "content".getBytes());

    // When
    DocumentUploadResponse response = documentService.uploadDocument(file, "PITCH", 1L, 1L, "testuser");

    // Then
    assertThat(response).isNotNull();
    assertThat(response.isTextExtracted()).isFalse();
    assertThat(response.getErrorMessage()).contains("Unsupported file type");
    verify(documentRepository, never()).save(any());
  }

  @Test
  void uploadDocument_withFileTooLarge_shouldReturnError() {
    // Given
    ReflectionTestUtils.setField(documentService, "maxFileSize", 100L); // Very small limit
    byte[] largeContent = new byte[200];
    MockMultipartFile file = new MockMultipartFile("file", "large.txt", "text/plain", largeContent);

    // When
    DocumentUploadResponse response = documentService.uploadDocument(file, "PITCH", 1L, 1L, "testuser");

    // Then
    assertThat(response).isNotNull();
    assertThat(response.isTextExtracted()).isFalse();
    assertThat(response.getErrorMessage()).contains("File size exceeds maximum");
    verify(documentRepository, never()).save(any());
  }

  @Test
  void getDocumentsByEntity_shouldReturnDocuments() {
    // Given
    UploadedDocument doc1 = UploadedDocument.builder().id(1L).originalFileName("doc1.pdf").build();
    UploadedDocument doc2 = UploadedDocument.builder().id(2L).originalFileName("doc2.pdf").build();
    when(documentRepository.findByEntityTypeAndEntityId("PITCH", 1L)).thenReturn(List.of(doc1, doc2));

    // When
    List<UploadedDocument> documents = documentService.getDocumentsByEntity("PITCH", 1L);

    // Then
    assertThat(documents).hasSize(2);
    assertThat(documents).containsExactly(doc1, doc2);
  }

  @Test
  void getDocumentById_whenExists_shouldReturnDocument() {
    // Given
    UploadedDocument doc = UploadedDocument.builder().id(1L).originalFileName("doc.pdf").build();
    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

    // When
    UploadedDocument result = documentService.getDocumentById(1L);

    // Then
    assertThat(result).isEqualTo(doc);
  }

  @Test
  void getDocumentById_whenNotExists_shouldThrowException() {
    // Given
    when(documentRepository.findById(999L)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> documentService.getDocumentById(999L)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Document not found");
  }

  @Test
  void deleteDocument_withStorageKey_shouldDeleteViaSpi() {
    // Given
    UploadedDocument doc = UploadedDocument.builder().id(1L).storageProvider(StorageProviderType.S3)
        .storageKey("documents/uuid_file.pdf").build();
    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

    // When
    documentService.deleteDocument(1L);

    // Then — removal goes through the SPI, not the filesystem
    verify(objectStorageService).delete(StorageProviderType.S3, "documents/uuid_file.pdf");
    verify(documentRepository).delete(doc);
  }

  @Test
  void downloadDocument_withStorageKey_shouldRetrieveViaSpi() {
    // Given
    UploadedDocument doc = UploadedDocument.builder().id(1L).originalFileName("report.pdf").fileType("pdf")
        .storageProvider(StorageProviderType.S3).storageKey("documents/uuid_report.pdf").build();
    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

    DownloadResource dr = DownloadResource.builder()
        .stream(new ByteArrayInputStream("PDF bytes".getBytes()))
        .contentType("application/pdf").sizeBytes(9L).filename("report.pdf").build();
    when(objectStorageService.retrieve(StorageProviderType.S3, "documents/uuid_report.pdf")).thenReturn(dr);

    // When
    ResponseEntity<Resource> response = documentService.downloadDocument(1L);

    // Then
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
    assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("report.pdf");
    assertThat(response.getHeaders().getContentLength()).isEqualTo(9L);
    verify(objectStorageService).retrieve(StorageProviderType.S3, "documents/uuid_report.pdf");
  }

  @Test
  void getDocumentBytes_withStorageKey_shouldReadViaSpi() throws IOException {
    // Given
    UploadedDocument doc = UploadedDocument.builder().id(1L).originalFileName("d.pdf").fileType("pdf")
        .storageProvider(StorageProviderType.MINIO).storageKey("documents/uuid_d.pdf").build();
    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

    DownloadResource dr = DownloadResource.builder()
        .stream(new ByteArrayInputStream("payload".getBytes()))
        .contentType("application/pdf").sizeBytes(7L).filename("d.pdf").build();
    when(objectStorageService.retrieve(StorageProviderType.MINIO, "documents/uuid_d.pdf")).thenReturn(dr);

    // When
    byte[] bytes = documentService.getDocumentBytes(1L);

    // Then
    assertThat(new String(bytes)).isEqualTo("payload");
    verify(objectStorageService).retrieve(StorageProviderType.MINIO, "documents/uuid_d.pdf");
  }

  @Test
  void deleteDocument_shouldDeleteFileAndRecord() throws IOException {
    // Given
    Path testFile = tempDir.resolve("test-file.txt");
    Files.write(testFile, "content".getBytes());

    UploadedDocument doc = UploadedDocument.builder().id(1L).storagePath(testFile.toString()).build();

    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

    // When
    documentService.deleteDocument(1L);

    // Then
    verify(documentRepository).delete(doc);
    assertThat(Files.exists(testFile)).isFalse();
  }

  @Test
  void downloadDocument_withValidDocument_shouldReturnResource() throws IOException {
    // Given
    Path testFile = tempDir.resolve("download-test.pdf");
    Files.write(testFile, "PDF content".getBytes());

    UploadedDocument doc = UploadedDocument.builder().id(1L).originalFileName("document.pdf").fileType("pdf")
        .storagePath(testFile.toString()).build();

    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

    // When
    ResponseEntity<Resource> response = documentService.downloadDocument(1L);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
    assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("attachment")
        .contains("document.pdf");
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().exists()).isTrue();
  }

  @Test
  void downloadDocument_withDocxFile_shouldReturnCorrectContentType() throws IOException {
    // Given
    Path testFile = tempDir.resolve("document.docx");
    Files.write(testFile, "DOCX content".getBytes());

    UploadedDocument doc = UploadedDocument.builder().id(1L).originalFileName("report.docx").fileType("docx")
        .storagePath(testFile.toString()).build();

    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

    // When
    ResponseEntity<Resource> response = documentService.downloadDocument(1L);

    // Then
    assertThat(response.getHeaders().getContentType()).isEqualTo(
        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
  }

  @Test
  void downloadDocument_withTextFile_shouldReturnTextPlainContentType() throws IOException {
    // Given
    Path testFile = tempDir.resolve("notes.txt");
    Files.write(testFile, "Text content".getBytes());

    UploadedDocument doc = UploadedDocument.builder().id(1L).originalFileName("notes.txt").fileType("txt")
        .storagePath(testFile.toString()).build();

    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

    // When
    ResponseEntity<Resource> response = documentService.downloadDocument(1L);

    // Then
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  void downloadDocument_whenFileNotFound_shouldThrowException() {
    // Given
    UploadedDocument doc = UploadedDocument.builder().id(1L).originalFileName("missing.pdf").fileType("pdf")
        .storagePath(tempDir.resolve("non-existent.pdf").toString()).build();

    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

    // When/Then
    assertThatThrownBy(() -> documentService.downloadDocument(1L)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("File not found or not readable");
  }

  @Test
  void extractTextFromFile_withTextFile_shouldExtractContent() throws IOException {
    // Given
    String textContent = "This is a test document";
    MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", textContent.getBytes());

    // When
    String extracted = documentService.extractTextFromFile(file);

    // Then
    assertThat(extracted).isEqualTo(textContent);
  }

  @Test
  void getPendingDocuments_shouldReturnUnindexedDocuments() {
    // Given
    UploadedDocument doc1 = UploadedDocument.builder().id(1L).indexedForQA(false).build();
    UploadedDocument doc2 = UploadedDocument.builder().id(2L).indexedForQA(false).build();
    when(documentRepository.findByIndexedForQAFalse()).thenReturn(List.of(doc1, doc2));

    // When
    List<UploadedDocument> pending = documentService.getPendingDocuments();

    // Then
    assertThat(pending).hasSize(2);
    assertThat(pending).containsExactly(doc1, doc2);
  }

  @Test
  void linkDocumentToEntity_whenDocumentExists_shouldUpdateEntity() {
    // Given
    UploadedDocument doc = UploadedDocument.builder().id(1L).fileName("test.pdf").originalFileName("test.pdf")
        .entityType("PITCH").entityId(0L).build();

    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
    when(documentRepository.save(any(UploadedDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // When
    documentService.linkDocumentToEntity(1L, "PITCH", 123L);

    // Then
    verify(documentRepository).save(
        argThat(document -> document.getEntityType().equals("PITCH") && document.getEntityId().equals(123L)));
  }

  @Test
  void linkDocumentToEntity_whenDocumentNotFound_shouldThrowException() {
    // Given
    when(documentRepository.findById(999L)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> documentService.linkDocumentToEntity(999L, "PITCH", 123L))
        .isInstanceOf(RuntimeException.class).hasMessageContaining("Document not found");
  }
}
