package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.DocumentUploadResponse;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanningAttachmentService")
class PlanningAttachmentServiceTest {

  @Mock private DocumentService documentService;
  @Mock private EpicRepository epicRepository;
  @Mock private InitiativeRepository initiativeRepository;
  @Mock private ProjectPermissionService projectPermissionService;

  private PlanningAttachmentService service;
  private final MockMultipartFile image =
      new MockMultipartFile("file", "mockup.png", "image/png", new byte[] {1, 2, 3});

  @BeforeEach
  void setUp() {
    service = new PlanningAttachmentService(documentService, epicRepository, initiativeRepository,
        projectPermissionService);
  }

  private static Project project(long id) {
    Project p = new Project();
    p.setId(id);
    return p;
  }

  private void epicInProject(long epicId, long projectId) {
    Epic e = new Epic();
    e.setProject(project(projectId));
    when(epicRepository.findByIdNotDeleted(epicId)).thenReturn(Optional.of(e));
  }

  @Test
  @DisplayName("Only epic and initiative are accepted, any case")
  void normalizeType() {
    assertThat(PlanningAttachmentService.normalizeType("epic")).isEqualTo("EPIC");
    assertThat(PlanningAttachmentService.normalizeType(" Initiative ")).isEqualTo("INITIATIVE");
    assertThatThrownBy(() -> PlanningAttachmentService.normalizeType("task"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PlanningAttachmentService.normalizeType(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Uploading to an epic needs contributor rights on ITS project, then stores via media path")
  void uploadToEpic() {
    epicInProject(3L, 5L);
    DocumentUploadResponse ok = DocumentUploadResponse.builder().id(77L).build();
    when(documentService.uploadMediaAttachment(eq(image), eq("EPIC"), eq(3L), eq(1L), eq("sara"),
        eq(PlanningAttachmentService.MAX_BYTES))).thenReturn(ok);

    assertThat(service.upload("epic", 3L, image, 1L, "sara")).isSameAs(ok);

    verify(projectPermissionService).requireContributor(5L);
  }

  @Test
  @DisplayName("Uploading to an initiative uses the initiative's project")
  void uploadToInitiative() {
    Initiative i = new Initiative();
    i.setProject(project(9L));
    when(initiativeRepository.findByIdNotDeleted(4L)).thenReturn(Optional.of(i));
    when(documentService.uploadMediaAttachment(any(), eq("INITIATIVE"), eq(4L), anyLong(), any(), anyLong()))
        .thenReturn(DocumentUploadResponse.builder().id(1L).build());

    service.upload("initiative", 4L, image, 1L, "sara");

    verify(projectPermissionService).requireContributor(9L);
  }

  @Test
  @DisplayName("No contributor rights: nothing is stored")
  void uploadDenied() {
    epicInProject(3L, 5L);
    doThrow(new AccessDeniedException("no")).when(projectPermissionService).requireContributor(5L);

    assertThatThrownBy(() -> service.upload("epic", 3L, image, 1L, "sara"))
        .isInstanceOf(AccessDeniedException.class);
    verify(documentService, never()).uploadMediaAttachment(any(), any(), anyLong(), anyLong(), any(), anyLong());
  }

  @Test
  @DisplayName("A missing or deleted epic is a 404, not a silent upload")
  void missingEpic() {
    when(epicRepository.findByIdNotDeleted(3L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.upload("epic", 3L, image, 1L, "sara"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("An epic with no project is refused (fails closed)")
  void epicWithoutProject() {
    when(epicRepository.findByIdNotDeleted(3L)).thenReturn(Optional.of(new Epic()));

    assertThatThrownBy(() -> service.list("epic", 3L)).isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Listing needs project access (viewers may look) and returns the stored documents")
  void listChecksAccess() {
    epicInProject(3L, 5L);
    List<UploadedDocument> docs = List.of(UploadedDocument.builder().id(1L).build());
    when(documentService.getDocumentsByEntity("EPIC", 3L)).thenReturn(docs);

    assertThat(service.list("epic", 3L)).isEqualTo(docs);

    verify(projectPermissionService).requireProjectAccess(5L);
    verify(projectPermissionService, never()).requireContributor(anyLong());
  }

  @Test
  @DisplayName("Delete removes an attachment that belongs to this epic")
  void deleteOwn() {
    epicInProject(3L, 5L);
    when(documentService.getDocumentById(11L))
        .thenReturn(UploadedDocument.builder().id(11L).entityType("EPIC").entityId(3L).build());

    service.delete("epic", 3L, 11L);

    verify(projectPermissionService).requireContributor(5L);
    verify(documentService).deleteDocument(11L);
  }

  @Test
  @DisplayName("Delete refuses a document id that belongs to something else (no cross-entity deletes)")
  void deleteForeignDocument() {
    epicInProject(3L, 5L);
    when(documentService.getDocumentById(11L))
        .thenReturn(UploadedDocument.builder().id(11L).entityType("PITCH").entityId(3L).build());

    assertThatThrownBy(() -> service.delete("epic", 3L, 11L)).isInstanceOf(ResourceNotFoundException.class);
    verify(documentService, never()).deleteDocument(anyLong());
  }

  @Test
  @DisplayName("Delete refuses an attachment of a DIFFERENT epic")
  void deleteOtherEpicsAttachment() {
    epicInProject(3L, 5L);
    when(documentService.getDocumentById(11L))
        .thenReturn(UploadedDocument.builder().id(11L).entityType("EPIC").entityId(99L).build());

    assertThatThrownBy(() -> service.delete("epic", 3L, 11L)).isInstanceOf(ResourceNotFoundException.class);
    verify(documentService, never()).deleteDocument(anyLong());
  }
}
