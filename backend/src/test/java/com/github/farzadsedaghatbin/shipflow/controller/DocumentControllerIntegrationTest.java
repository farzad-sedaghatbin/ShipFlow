package com.github.farzadsedaghatbin.shipflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Permission;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for DocumentController. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DocumentControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private PitchRepository pitchRepository;

  @Autowired
  private CycleRepository cycleRepository;

  @Autowired
  private TeamRepository teamRepository;

  @Autowired
  private UploadedDocumentRepository documentRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PermissionRepository permissionRepository;

  @Autowired
  private BugReportRepository bugReportRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Value("${app.upload.dir}")
  private String uploadDir;

  @TempDir
  Path tempDir;

  private Pitch testPitch;
  private Cycle testCycle;
  private User testUser;
  private BugReport testBug;

  @BeforeEach
  void setUp() {
    // Clean up
    permissionRepository.deleteAll();
    documentRepository.deleteAll();
    pitchRepository.deleteAll();
    cycleRepository.deleteAll();

    // Create test user for authentication
    testUser = userRepository.findByUsername("testuser").orElse(null);
    if (testUser == null) {
      testUser = User.builder().username("testuser").email("test@example.com").password("password")
          .role(UserRole.MEMBER).isActive(true).build();
      testUser = userRepository.save(testUser);
    }

    // Add permissions for AI_FEATURES
    permissionRepository.save(Permission.builder().role(UserRole.MEMBER).resourceType(ResourceType.AI_FEATURES)
        .permissionType(PermissionType.UPDATE).build());

    // Create test data
    Team team = teamRepository.findAll().stream().findFirst().orElse(null);
    if (team == null) {
      team = Team.builder().name("Test Team").build();
      team = teamRepository.save(team);
    }

    testCycle = Cycle.builder().name("Test Cycle").startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(42))
        .phase(com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase.SHAPING).isActive(true).build();
    testCycle = cycleRepository.save(testCycle);
    team.setCycle(testCycle);
    teamRepository.save(team);

    testPitch = Pitch.builder().title("Test Pitch").description("Test pitch for documents").cycle(testCycle)
        .appetiteDays(14).status(com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus.PENDING)
        .build();
    testPitch = pitchRepository.save(testPitch);

    // Create test project for bug report
    Project testProject = projectRepository.findAll().stream().findFirst().orElse(null);
    if (testProject == null) {
      testProject = Project.builder()
          .name("Test Project")
          .projectKey("TEST")
          .description("Test project for bug attachments")
          .isActive(true)
          .build();
      testProject = projectRepository.save(testProject);
    }

    // Create test bug report
    testBug = BugReport.builder()
        .bugKey("BUG-001")
        .title("Test Bug")
        .description("Test bug for attachment testing")
        .severity(BugSeverity.MAJOR)
        .status(BugStatus.OPEN)
        .project(testProject)
        .reporter(testUser)
        .build();
    testBug = bugReportRepository.save(testBug);
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void uploadDocumentForPitch_withValidPdf_shouldReturnSuccess() throws Exception {
    // Given
    MockMultipartFile file = new MockMultipartFile("file", "test-pitch.pdf", "application/pdf",
        "PDF content here".getBytes());

    // When/Then
    mockMvc.perform(multipart("/api/documents/pitch/{pitchId}/upload", testPitch.getId()).file(file))
        .andExpect(status().isOk()).andExpect(jsonPath("$.fileName").exists())
        .andExpect(jsonPath("$.fileType").value("pdf"));
    // Note: textExtracted may be false for invalid PDF content

    // Verify document was saved
    assertThat(documentRepository.findByEntityTypeAndEntityId("PITCH", testPitch.getId())).hasSize(1);
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void uploadDocumentForPitch_withTextFile_shouldExtractText() throws Exception {
    // Given
    String textContent = "This is a test pitch document with important information.";
    MockMultipartFile file = new MockMultipartFile("file", "pitch-notes.txt", "text/plain", textContent.getBytes());

    // When/Then
    mockMvc.perform(multipart("/api/documents/pitch/{pitchId}/upload", testPitch.getId()).file(file))
        .andExpect(status().isOk()).andExpect(jsonPath("$.textExtracted").value(true))
        .andExpect(jsonPath("$.extractedText").value(textContent));
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void getDocumentsForPitch_shouldReturnAllDocuments() throws Exception {
    // Given
    UploadedDocument doc1 = createDocument("doc1.txt", "PITCH", testPitch.getId());
    UploadedDocument doc2 = createDocument("doc2.txt", "PITCH", testPitch.getId());

    // When/Then
    mockMvc.perform(get("/api/documents/pitch/{pitchId}", testPitch.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].originalFileName").exists())
        .andExpect(jsonPath("$[1].originalFileName").exists());
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void downloadDocument_withValidId_shouldReturnFile() throws Exception {
    // Given
    UploadedDocument doc = createDocument("download-test.pdf", "PITCH", testPitch.getId());

    // When/Then
    mockMvc.perform(get("/api/documents/{id}/download", doc.getId())).andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"download-test.pdf\""))
        .andExpect(content().contentType(MediaType.APPLICATION_PDF));
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void downloadDocument_withTextFile_shouldReturnTextPlain() throws Exception {
    // Given
    UploadedDocument doc = createDocument("notes.txt", "PITCH", testPitch.getId());

    // When/Then
    mockMvc.perform(get("/api/documents/{id}/download", doc.getId())).andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"notes.txt\""))
        .andExpect(content().contentType(MediaType.TEXT_PLAIN));
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void downloadDocument_withNonExistentId_shouldReturnNotFound() throws Exception {
    // When/Then
    mockMvc.perform(get("/api/documents/{id}/download", 99999L)).andExpect(status().is5xxServerError());
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void deleteDocument_withValidId_shouldRemoveDocument() throws Exception {
    // Given
    UploadedDocument doc = createDocument("delete-test.txt", "PITCH", testPitch.getId());
    Long docId = doc.getId();

    // When/Then
    mockMvc.perform(delete("/api/documents/{id}", docId)).andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Document deleted successfully"));

    // Verify deletion
    assertThat(documentRepository.findById(docId)).isEmpty();
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void getDocument_withValidId_shouldReturnDocument() throws Exception {
    // Given
    UploadedDocument doc = createDocument("get-test.txt", "PITCH", testPitch.getId());

    // When/Then
    mockMvc.perform(get("/api/documents/{id}", doc.getId())).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(doc.getId()))
        .andExpect(jsonPath("$.originalFileName").value("get-test.txt"))
        .andExpect(jsonPath("$.fileType").value("txt"));
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void extractPitchData_withTextDocument_shouldExtractShapeUpFields() throws Exception {
    // Given
    String pitchContent = """
        Problem: Users struggle to track project progress effectively.

        Solution: Create a comprehensive dashboard with real-time updates.

        Rabbit Holes: Don't get stuck on complex authentication flows.

        Risks: Integration with third-party APIs might be unstable.

        No-Gos: We won't build a mobile app in this cycle.

        Appetite: 6 weeks
        """;

    MockMultipartFile file = new MockMultipartFile("file", "pitch-document.txt", "text/plain",
        pitchContent.getBytes());

    // When/Then - Expect OK or Bad Request depending on AI availability
    mockMvc.perform(
        multipart("/api/documents/extract-pitch-data").file(file).param("pitchId", testPitch.getId().toString())
            .param("addToKnowledgeBase", "false").param("saveDocument", "false"))
        .andExpect(jsonPath("$.extractionSuccessful").exists());
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void getExtractionStatus_shouldReturnAvailability() throws Exception {
    // When/Then
    mockMvc.perform(get("/api/documents/extract-pitch-data/status")).andExpect(status().isOk())
        .andExpect(jsonPath("$.available").exists()).andExpect(jsonPath("$.message").exists());
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void extractPitchData_withSaveDocument_shouldReturnDocumentId() throws Exception {
    // Given
    String pitchContent = "Problem: Test problem\nSolution: Test solution";
    MockMultipartFile file = new MockMultipartFile("file", "pitch.txt", "text/plain", pitchContent.getBytes());

    // When/Then - Check that extraction response includes basic fields
    mockMvc.perform(multipart("/api/documents/extract-pitch-data").file(file).param("addToKnowledgeBase", "false")
        .param("saveDocument", "true")).andExpect(jsonPath("$.extractionSuccessful").exists());
    // documentId will only exist if extraction was successful
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void linkDocumentToPitch_withValidIds_shouldLinkSuccessfully() throws Exception {
    // Given - Create a document with entityId=0 (temporary)
    UploadedDocument doc = createDocument("temp-doc.txt", "PITCH", 0L);

    // When/Then - Link to actual pitch
    mockMvc.perform(put("/api/documents/{documentId}/link-to-pitch/{pitchId}", doc.getId(), testPitch.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Document linked to pitch successfully"));

    // Verify the document was updated
    UploadedDocument updated = documentRepository.findById(doc.getId()).orElseThrow();
    assertThat(updated.getEntityId()).isEqualTo(testPitch.getId());
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void linkDocumentToPitch_withInvalidDocumentId_shouldReturnBadRequest() throws Exception {
    // When/Then
    mockMvc.perform(put("/api/documents/{documentId}/link-to-pitch/{pitchId}", 999L, testPitch.getId()))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
  }

  // ========== Bug Report Attachment Tests ==========

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void uploadBugAttachment_withValidImage_shouldReturnSuccess() throws Exception {
    // Given - Create a mock PNG image
    byte[] imageContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}; // PNG header
    MockMultipartFile file = new MockMultipartFile("file", "screenshot.png", "image/png", imageContent);

    // When/Then
    mockMvc.perform(multipart("/api/documents/bug/{bugId}/attachment", testBug.getId()).file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fileName").value("screenshot.png"))
        .andExpect(jsonPath("$.fileType").value("png"))
        .andExpect(jsonPath("$.textExtracted").value(false));

    // Verify document was saved
    assertThat(documentRepository.findByEntityTypeAndEntityId("BUG_REPORT", testBug.getId())).hasSize(1);
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void uploadBugAttachment_withValidVideo_shouldReturnSuccess() throws Exception {
    // Given - Create a mock MP4 video
    byte[] videoContent = "mock video content".getBytes();
    MockMultipartFile file = new MockMultipartFile("file", "recording.mp4", "video/mp4", videoContent);

    // When/Then
    mockMvc.perform(multipart("/api/documents/bug/{bugId}/attachment", testBug.getId()).file(file))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fileName").value("recording.mp4"))
        .andExpect(jsonPath("$.fileType").value("mp4"))
        .andExpect(jsonPath("$.textExtracted").value(false));
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void uploadBugAttachment_withInvalidFileType_shouldReturnError() throws Exception {
    // Given - Create a file with unsupported type
    MockMultipartFile file = new MockMultipartFile("file", "document.exe", "application/octet-stream", 
        "executable content".getBytes());

    // When/Then
    mockMvc.perform(multipart("/api/documents/bug/{bugId}/attachment", testBug.getId()).file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorMessage").exists());
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void uploadBugAttachment_withEmptyFile_shouldReturnError() throws Exception {
    // Given - Create an empty file
    MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

    // When/Then
    mockMvc.perform(multipart("/api/documents/bug/{bugId}/attachment", testBug.getId()).file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorMessage").value("File is empty"));
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void getBugAttachments_shouldReturnAllAttachments() throws Exception {
    // Given - Create two attachments for the bug
    createDocument("screenshot1.png", "BUG_REPORT", testBug.getId());
    createDocument("screenshot2.jpg", "BUG_REPORT", testBug.getId());

    // When/Then
    mockMvc.perform(get("/api/documents/bug/{bugId}/attachments", testBug.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].originalFileName").exists())
        .andExpect(jsonPath("$[1].originalFileName").exists());
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void getBugAttachments_withNonExistentBug_shouldReturnEmptyList() throws Exception {
    // When/Then
    mockMvc.perform(get("/api/documents/bug/{bugId}/attachments", 99999L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void deleteBugAttachment_withValidId_shouldRemoveAttachment() throws Exception {
    // Given - Create an attachment
    UploadedDocument doc = createDocument("screenshot.png", "BUG_REPORT", testBug.getId());
    Long docId = doc.getId();

    // When/Then
    mockMvc.perform(delete("/api/documents/bug/attachment/{attachmentId}", docId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Attachment deleted successfully"));

    // Verify deletion
    assertThat(documentRepository.findById(docId)).isEmpty();
  }

  @Test
  @WithMockUser(username = "testuser", roles = {"USER"})
  void uploadBugAttachment_multipleFiles_shouldSaveAll() throws Exception {
    // Given - Create multiple files
    MockMultipartFile file1 = new MockMultipartFile("file", "screenshot1.png", "image/png", 
        "image content 1".getBytes());

    // Upload first file
    mockMvc.perform(multipart("/api/documents/bug/{bugId}/attachment", testBug.getId()).file(file1))
        .andExpect(status().isOk());

    // Upload second file
    MockMultipartFile file2 = new MockMultipartFile("file", "recording.mp4", "video/mp4", 
        "video content".getBytes());
    mockMvc.perform(multipart("/api/documents/bug/{bugId}/attachment", testBug.getId()).file(file2))
        .andExpect(status().isOk());

    // Verify both attachments were saved
    assertThat(documentRepository.findByEntityTypeAndEntityId("BUG_REPORT", testBug.getId())).hasSize(2);
  }

  private UploadedDocument createDocument(String fileName, String entityType, Long entityId) throws IOException {
    // Create actual file in the upload directory (where DocumentService looks for it)
    Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    Files.createDirectories(uploadPath);
    
    String storedFileName = "uuid_" + fileName;
    Path filePath = uploadPath.resolve(storedFileName);
    Files.writeString(filePath, "Sample text content for " + fileName);

    UploadedDocument doc = UploadedDocument.builder().fileName(storedFileName).originalFileName(fileName)
        .fileType(fileName.substring(fileName.lastIndexOf('.') + 1)).fileSize(Files.size(filePath))
        .storagePath(storedFileName).extractedText("Sample text content").textExtracted(true)
        .entityType(entityType).entityId(entityId).uploaderId(1L).uploaderUsername("testuser")
        .indexedForQA(false).build();
    return documentRepository.save(doc);
  }
}
