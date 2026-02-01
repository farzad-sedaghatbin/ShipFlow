package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.DocumentUploadResponse;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.repository.UploadedDocumentRepository;
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

  @Mock private UploadedDocumentRepository documentRepository;

  @Mock private KnowledgeIngestionService knowledgeIngestionService;

  @Mock private LocalizationService localizationService;

  @InjectMocks private DocumentService documentService;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    lenient()
        .when(localizationService.getMessage(anyString(), any(Object[].class)))
        .thenAnswer(
            i -> {
              String key = i.getArgument(0);
              if (key.contains("file.not.found")) return "File not found or not readable";
              if (key.contains("document.not.found")) return "File not found or not readable";
              return key;
            });
    lenient()
        .when(localizationService.getMessage(anyString()))
        .thenAnswer(
            i -> {
              String key = i.getArgument(0);
              if (key.contains("file.not.found")) return "File not found or not readable";
              if (key.contains("document.not.found")) return "File not found or not readable";
              return key;
            });

    ReflectionTestUtils.setField(documentService, "uploadDir", tempDir.toString());
    ReflectionTestUtils.setField(documentService, "maxFileSize", 10485760L); // 10MB
  }

  @Test
  void uploadDocument_withPdfFile_shouldExtractTextAndSave() throws IOException {
    // Given
    String fileName = "test-document.pdf";
    byte[] content = "PDF content".getBytes();
    MockMultipartFile file = new MockMultipartFile("file", fileName, "application/pdf", content);

    UploadedDocument savedDoc =
        UploadedDocument.builder()
            .id(1L)
            .fileName("uuid_" + fileName)
            .originalFileName(fileName)
            .fileType("pdf")
            .fileSize((long) content.length)
            .textExtracted(true)
            .build();

    when(documentRepository.save(any(UploadedDocument.class))).thenReturn(savedDoc);

    // When
    DocumentUploadResponse response =
        documentService.uploadDocument(file, "PITCH", 1L, 1L, "testuser");

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getFileName()).contains(fileName);
    assertThat(response.getFileType()).isEqualTo("pdf");
    verify(documentRepository).save(any(UploadedDocument.class));
  }

  @Test
  void uploadDocument_withEmptyFile_shouldReturnError() {
    // Given
    MockMultipartFile emptyFile =
        new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

    // When
    DocumentUploadResponse response =
        documentService.uploadDocument(emptyFile, "PITCH", 1L, 1L, "testuser");

    // Then
    assertThat(response).isNotNull();
    assertThat(response.isTextExtracted()).isFalse();
    assertThat(response.getErrorMessage()).contains("File is empty");
    verify(documentRepository, never()).save(any());
  }

  @Test
  void uploadDocument_withUnsupportedFileType_shouldReturnError() {
    // Given
    MockMultipartFile file =
        new MockMultipartFile("file", "test.exe", "application/x-executable", "content".getBytes());

    // When
    DocumentUploadResponse response =
        documentService.uploadDocument(file, "PITCH", 1L, 1L, "testuser");

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
    DocumentUploadResponse response =
        documentService.uploadDocument(file, "PITCH", 1L, 1L, "testuser");

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
    when(documentRepository.findByEntityTypeAndEntityId("PITCH", 1L))
        .thenReturn(List.of(doc1, doc2));

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
    assertThatThrownBy(() -> documentService.getDocumentById(999L))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Document not found");
  }

  @Test
  void deleteDocument_shouldDeleteFileAndRecord() throws IOException {
    // Given
    Path testFile = tempDir.resolve("test-file.txt");
    Files.write(testFile, "content".getBytes());

    UploadedDocument doc =
        UploadedDocument.builder().id(1L).storagePath(testFile.toString()).build();

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

    UploadedDocument doc =
        UploadedDocument.builder()
            .id(1L)
            .originalFileName("document.pdf")
            .fileType("pdf")
            .storagePath(testFile.toString())
            .build();

    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

    // When
    ResponseEntity<Resource> response = documentService.downloadDocument(1L);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
    assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .contains("attachment")
        .contains("document.pdf");
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().exists()).isTrue();
  }

  @Test
  void downloadDocument_withDocxFile_shouldReturnCorrectContentType() throws IOException {
    // Given
    Path testFile = tempDir.resolve("document.docx");
    Files.write(testFile, "DOCX content".getBytes());

    UploadedDocument doc =
        UploadedDocument.builder()
            .id(1L)
            .originalFileName("report.docx")
            .fileType("docx")
            .storagePath(testFile.toString())
            .build();

    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

    // When
    ResponseEntity<Resource> response = documentService.downloadDocument(1L);

    // Then
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
  }

  @Test
  void downloadDocument_withTextFile_shouldReturnTextPlainContentType() throws IOException {
    // Given
    Path testFile = tempDir.resolve("notes.txt");
    Files.write(testFile, "Text content".getBytes());

    UploadedDocument doc =
        UploadedDocument.builder()
            .id(1L)
            .originalFileName("notes.txt")
            .fileType("txt")
            .storagePath(testFile.toString())
            .build();

    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

    // When
    ResponseEntity<Resource> response = documentService.downloadDocument(1L);

    // Then
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
  }

  @Test
  void downloadDocument_whenFileNotFound_shouldThrowException() {
    // Given
    UploadedDocument doc =
        UploadedDocument.builder()
            .id(1L)
            .originalFileName("missing.pdf")
            .fileType("pdf")
            .storagePath(tempDir.resolve("non-existent.pdf").toString())
            .build();

    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));

    // When/Then
    assertThatThrownBy(() -> documentService.downloadDocument(1L))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("File not found or not readable");
  }

  @Test
  void extractTextFromFile_withTextFile_shouldExtractContent() throws IOException {
    // Given
    String textContent = "This is a test document";
    MockMultipartFile file =
        new MockMultipartFile("file", "test.txt", "text/plain", textContent.getBytes());

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
    UploadedDocument doc =
        UploadedDocument.builder()
            .id(1L)
            .fileName("test.pdf")
            .originalFileName("test.pdf")
            .entityType("PITCH")
            .entityId(0L)
            .build();

    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
    when(documentRepository.save(any(UploadedDocument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    documentService.linkDocumentToEntity(1L, "PITCH", 123L);

    // Then
    verify(documentRepository)
        .save(
            argThat(
                document ->
                    document.getEntityType().equals("PITCH")
                        && document.getEntityId().equals(123L)));
  }

  @Test
  void linkDocumentToEntity_whenDocumentNotFound_shouldThrowException() {
    // Given
    when(documentRepository.findById(999L)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> documentService.linkDocumentToEntity(999L, "PITCH", 123L))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Document not found");
  }
}
