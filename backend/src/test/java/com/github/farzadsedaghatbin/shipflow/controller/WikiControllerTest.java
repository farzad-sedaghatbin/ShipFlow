package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.github.farzadsedaghatbin.shipflow.service.storage.DownloadResource;
import com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageService;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import com.github.farzadsedaghatbin.shipflow.service.storage.StoredObjectRef;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for WikiController.
 *
 * <p>Uses @SpringBootTest with real database (H2) for space/page CRUD. ObjectStorageService is
 * mocked to avoid real file I/O.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "wikiadmin", roles = {"ADMIN"})
class WikiControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private WikiSpaceRepository wikiSpaceRepository;
  @Autowired private WikiPageRepository wikiPageRepository;
  @Autowired private WikiAttachmentRepository wikiAttachmentRepository;
  @Autowired private WikiSpacePermissionRepository wikiSpacePermissionRepository;

  @MockBean private ObjectStorageService objectStorageService;

  private User adminUser;

  @BeforeEach
  void setUp() {
    wikiAttachmentRepository.deleteAll();
    wikiSpacePermissionRepository.deleteAll();
    wikiPageRepository.deleteAll();
    wikiSpaceRepository.deleteAll();
    userRepository.deleteAll();

    adminUser =
        User.builder()
            .username("wikiadmin")
            .email("wikiadmin@test.com")
            .password("password")
            .role(UserRole.ADMIN)
            .isActive(true)
            .build();
    userRepository.save(adminUser);

    // Default storage mock: active provider is LOCAL_FS
    when(objectStorageService.activeProvider()).thenReturn(StorageProviderType.LOCAL_FS);
  }

  // ── Space CRUD ────────────────────────────────────────────────────────────

  @Test
  void createSpace_happyPath_returns201() throws Exception {
    CreateWikiSpaceRequest req = new CreateWikiSpaceRequest("Engineering", "ENG", "Engineering docs");

    mockMvc
        .perform(
            post("/api/wiki/spaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("Engineering")))
        .andExpect(jsonPath("$.spaceKey", is("ENG")));
  }

  @Test
  void listSpaces_returnsCreatedSpaces() throws Exception {
    CreateWikiSpaceRequest req = new CreateWikiSpaceRequest("Docs", "DOCS", null);
    mockMvc
        .perform(
            post("/api/wiki/spaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/wiki/spaces"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
  }

  @Test
  void getSpace_returns200() throws Exception {
    WikiSpace space =
        WikiSpace.builder()
            .name("Test Space")
            .spaceKey("TS1")
            .createdBy(adminUser.getId())
            .build();
    space = wikiSpaceRepository.save(space);

    mockMvc
        .perform(get("/api/wiki/spaces/{id}", space.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(space.getId().intValue())))
        .andExpect(jsonPath("$.name", is("Test Space")));
  }

  // ── Page CRUD ─────────────────────────────────────────────────────────────

  @Test
  void pageCrudHappyPath() throws Exception {
    WikiSpace space =
        WikiSpace.builder()
            .name("Page Test Space")
            .spaceKey("PTS1")
            .createdBy(adminUser.getId())
            .build();
    space = wikiSpaceRepository.save(space);

    // Create page
    CreateWikiPageRequest createReq =
        new CreateWikiPageRequest(space.getId(), null, "My Page", "{\"type\":\"doc\"}");
    String createBody =
        mockMvc
            .perform(
                post("/api/wiki/pages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title", is("My Page")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    Long pageId = objectMapper.readTree(createBody).get("id").asLong();

    // Get page
    mockMvc
        .perform(get("/api/wiki/pages/{id}", pageId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(pageId.intValue())))
        .andExpect(jsonPath("$.title", is("My Page")));

    // Update page
    UpdateWikiPageRequest updateReq = new UpdateWikiPageRequest("Updated Title", null);
    mockMvc
        .perform(
            put("/api/wiki/pages/{id}", pageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title", is("Updated Title")));

    // Delete page
    mockMvc.perform(delete("/api/wiki/pages/{id}", pageId)).andExpect(status().isNoContent());

    // Verify gone
    mockMvc.perform(get("/api/wiki/pages/{id}", pageId)).andExpect(status().isNotFound());
  }

  // ── Access control ────────────────────────────────────────────────────────

  @Test
  void unauthenticated_getPage_returns403Or401() throws Exception {
    WikiSpace space =
        WikiSpace.builder()
            .name("Any Space")
            .spaceKey("ANY1")
            .createdBy(adminUser.getId())
            .build();
    space = wikiSpaceRepository.save(space);

    WikiPage page =
        WikiPage.builder()
            .spaceId(space.getId())
            .title("Any Page")
            .slug("any-page")
            .content("{}")
            .contentText("Any")
            .position(0)
            .createdBy(adminUser.getId())
            .build();
    page = wikiPageRepository.save(page);

    // Spring Security redirects unauthenticated requests to the login page (302)
    // or returns 401/403 depending on security config — all indicate access is denied.
    mockMvc
        .perform(
            get("/api/wiki/pages/{id}", page.getId())
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.anonymous()))
        .andExpect(
            status()
                .is(
                    org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is(302),
                        org.hamcrest.Matchers.is(401),
                        org.hamcrest.Matchers.is(403))));
  }

  // ── Attachments ───────────────────────────────────────────────────────────

  @Test
  void uploadAttachment_routesThroughObjectStorageService_andRecordsProviderAndKey()
      throws Exception {
    WikiSpace space =
        WikiSpace.builder()
            .name("Attach Space")
            .spaceKey("ATT1")
            .createdBy(adminUser.getId())
            .build();
    space = wikiSpaceRepository.save(space);

    WikiPage page =
        WikiPage.builder()
            .spaceId(space.getId())
            .title("Attach Page")
            .slug("attach-page")
            .content("{}")
            .contentText("Attach")
            .position(0)
            .createdBy(adminUser.getId())
            .build();
    page = wikiPageRepository.save(page);

    Long pageId = page.getId();

    StoredObjectRef mockRef =
        StoredObjectRef.builder()
            .bucket("test-bucket")
            .key("wiki/" + pageId + "/abc123_test.png")
            .contentType("image/png")
            .sizeBytes(100L)
            .build();
    when(objectStorageService.store(eq("wiki/" + pageId), any(), any(), anyLong(), any()))
        .thenReturn(mockRef);

    MockMultipartFile file =
        new MockMultipartFile("file", "test.png", "image/png", "fake-image-bytes".getBytes());

    mockMvc
        .perform(multipart("/api/wiki/pages/{id}/attachments", pageId).file(file))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.pageId", is(pageId.intValue())))
        .andExpect(jsonPath("$.fileName", is("test.png")))
        .andExpect(jsonPath("$.contentType", is("image/png")));

    verify(objectStorageService).store(eq("wiki/" + pageId), any(), any(), anyLong(), any());

    List<WikiAttachment> attachments =
        wikiAttachmentRepository.findByPageIdAndDeletedAtIsNullOrderByCreatedAtDesc(pageId);
    org.junit.jupiter.api.Assertions.assertEquals(1, attachments.size());
    org.junit.jupiter.api.Assertions.assertEquals(
        StorageProviderType.LOCAL_FS, attachments.get(0).getStorageProvider());
    org.junit.jupiter.api.Assertions.assertEquals(
        "wiki/" + pageId + "/abc123_test.png", attachments.get(0).getStorageKey());
  }

  @Test
  void downloadAttachment_returnsEntityContentType() throws Exception {
    WikiSpace space =
        WikiSpace.builder()
            .name("Dl Space")
            .spaceKey("DL1")
            .createdBy(adminUser.getId())
            .build();
    space = wikiSpaceRepository.save(space);

    WikiPage page =
        WikiPage.builder()
            .spaceId(space.getId())
            .title("DL Page")
            .slug("dl-page")
            .content("{}")
            .contentText("DL")
            .position(0)
            .createdBy(adminUser.getId())
            .build();
    page = wikiPageRepository.save(page);

    WikiAttachment att =
        WikiAttachment.builder()
            .pageId(page.getId())
            .storageProvider(StorageProviderType.LOCAL_FS)
            .storageKey("wiki/" + page.getId() + "/uuid_report.pdf")
            .fileName("report.pdf")
            .contentType("application/pdf")
            .fileSize(512L)
            .uploadedBy(adminUser.getId())
            .build();
    att = wikiAttachmentRepository.save(att);

    when(objectStorageService.retrieve(StorageProviderType.LOCAL_FS, att.getStorageKey()))
        .thenReturn(
            DownloadResource.builder()
                .stream(new ByteArrayInputStream("pdf-bytes".getBytes()))
                // provider returns generic — entity's content type is used by the controller
                .contentType("application/octet-stream")
                .sizeBytes(512L)
                .filename("report.pdf")
                .build());

    mockMvc
        .perform(get("/api/wiki/attachments/{attId}/download", att.getId()))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("report.pdf")))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
  }

  @Test
  void downloadAttachment_missingObject_returns404() throws Exception {
    WikiSpace space =
        WikiSpace.builder()
            .name("Missing Space")
            .spaceKey("MS1")
            .createdBy(adminUser.getId())
            .build();
    space = wikiSpaceRepository.save(space);

    WikiPage page =
        WikiPage.builder()
            .spaceId(space.getId())
            .title("Missing Page")
            .slug("missing-page")
            .content("{}")
            .contentText("Missing")
            .position(0)
            .createdBy(adminUser.getId())
            .build();
    page = wikiPageRepository.save(page);

    WikiAttachment att =
        WikiAttachment.builder()
            .pageId(page.getId())
            .storageProvider(StorageProviderType.LOCAL_FS)
            .storageKey("wiki/" + page.getId() + "/gone_file.txt")
            .fileName("gone.txt")
            .contentType("text/plain")
            .fileSize(10L)
            .uploadedBy(adminUser.getId())
            .build();
    att = wikiAttachmentRepository.save(att);

    when(objectStorageService.retrieve(any(), any()))
        .thenThrow(new ResourceNotFoundException("Object not found in storage"));

    mockMvc
        .perform(get("/api/wiki/attachments/{attId}/download", att.getId()))
        .andExpect(status().isNotFound());
  }
}
