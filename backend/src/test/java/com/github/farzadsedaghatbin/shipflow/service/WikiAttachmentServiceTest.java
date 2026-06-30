package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.WikiAttachment;
import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiAttachmentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiPageRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpaceRepository;
import com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageService;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link WikiAttachmentService}, focused on the deletion-authorization matrix:
 * only the original uploader or an ADMIN may delete a wiki attachment. Read/write space access is
 * stubbed via the mocked {@link WikiPermissionService} so the test isolates the uploader-or-admin
 * rule.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WikiAttachmentService Tests")
class WikiAttachmentServiceTest {

  @Mock private WikiAttachmentRepository attachmentRepository;
  @Mock private WikiPageRepository pageRepository;
  @Mock private WikiSpaceRepository spaceRepository;
  @Mock private WikiPermissionService wikiPermissionService;
  @Mock private ObjectStorageService objectStorageService;
  @Mock private UserRepository userRepository;

  @InjectMocks private WikiAttachmentService service;

  private static final Long PAGE_ID = 5L;
  private static final Long SPACE_ID = 7L;
  private static final Long UPLOADER_ID = 10L;
  private static final Long ADMIN_ID = 99L;
  private static final Long OTHER_ID = 20L;

  @BeforeEach
  void setUp() {
    WikiPage page = WikiPage.builder().spaceId(SPACE_ID).build();
    WikiSpace space = WikiSpace.builder().id(SPACE_ID).build();
    when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));
    when(spaceRepository.findById(SPACE_ID)).thenReturn(Optional.of(space));

    User admin = new User();
    admin.setId(ADMIN_ID);
    admin.setRole(UserRole.ADMIN);
    User other = new User();
    other.setId(OTHER_ID);
    other.setRole(UserRole.MEMBER);
    when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
    when(userRepository.findById(OTHER_ID)).thenReturn(Optional.of(other));
  }

  private WikiAttachment attachmentUploadedBy(Long uploaderId) {
    WikiAttachment att =
        WikiAttachment.builder()
            .id(100L)
            .pageId(PAGE_ID)
            .storageProvider(StorageProviderType.LOCAL_FS)
            .storageKey("attachments/wiki/5/uuid_file.png")
            .fileName("file.png")
            .contentType("image/png")
            .fileSize(123L)
            .uploadedBy(uploaderId)
            .build();
    when(attachmentRepository.findById(100L)).thenReturn(Optional.of(att));
    return att;
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    @DisplayName("uploader can delete their own attachment (soft delete)")
    void delete_byUploader_succeeds() {
      WikiAttachment att = attachmentUploadedBy(UPLOADER_ID);

      service.delete(100L, UPLOADER_ID);

      assertThat(att.getDeletedAt()).isNotNull();
      verify(attachmentRepository).save(att);
      verify(objectStorageService).delete(StorageProviderType.LOCAL_FS, att.getStorageKey());
    }

    @Test
    @DisplayName("ADMIN can delete any attachment")
    void delete_byAdmin_succeeds() {
      WikiAttachment att = attachmentUploadedBy(UPLOADER_ID);

      service.delete(100L, ADMIN_ID);

      assertThat(att.getDeletedAt()).isNotNull();
      verify(attachmentRepository).save(att);
    }

    @Test
    @DisplayName("non-uploader non-admin — throws AccessDeniedException, no soft delete")
    void delete_byOtherUser_throws() {
      WikiAttachment att = attachmentUploadedBy(UPLOADER_ID);

      assertThatThrownBy(() -> service.delete(100L, OTHER_ID))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("uploader or an ADMIN");

      assertThat(att.getDeletedAt()).isNull();
      verify(attachmentRepository, never()).save(any());
      verify(objectStorageService, never()).delete(any(), any());
    }
  }
}
