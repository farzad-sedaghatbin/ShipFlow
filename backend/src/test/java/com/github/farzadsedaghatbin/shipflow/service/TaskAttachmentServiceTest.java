package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.TaskAttachmentDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TaskAttachment;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.TaskAttachmentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link TaskAttachmentService}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskAttachmentService Tests")
class TaskAttachmentServiceTest {

  @Mock
  private TaskAttachmentRepository attachmentRepository;
  @Mock
  private TaskRepository taskRepository;
  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private TaskAttachmentService service;

  @TempDir
  Path tempDir;

  private Task task;
  private User uploader;
  private User admin;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());

    task = Task.builder().build();
    ReflectionTestUtils.setField(task, "id", 1L);

    uploader = new User();
    uploader.setId(10L);
    uploader.setUsername("sara");
    uploader.setRole(UserRole.MEMBER);

    admin = new User();
    admin.setId(99L);
    admin.setUsername("admin");
    admin.setRole(UserRole.ADMIN);

    authenticateAs(uploader.getUsername());

    when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
    when(taskRepository.existsById(1L)).thenReturn(true);
    when(userRepository.findByUsername("sara")).thenReturn(Optional.of(uploader));
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
    when(attachmentRepository.save(any())).thenAnswer(inv -> {
      TaskAttachment a = inv.getArgument(0);
      ReflectionTestUtils.setField(a, "id", 100L);
      ReflectionTestUtils.setField(a, "createdAt", LocalDateTime.now());
      return a;
    });
  }

  private void authenticateAs(String username) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(username, null, List.of()));
  }

  // ── Upload ─────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("uploadAttachment")
  class Upload {

    @Test
    @DisplayName("valid PNG file — returns DTO with correct metadata")
    void upload_validPng_returnsDTO() {
      MockMultipartFile file = new MockMultipartFile(
          "file", "screenshot.png", "image/png", new byte[512]);

      TaskAttachmentDTO dto = service.uploadAttachment(1L, file);

      assertThat(dto.getFileName()).isEqualTo("screenshot.png");
      assertThat(dto.getContentType()).isEqualTo("image/png");
      assertThat(dto.getFileSize()).isEqualTo(512L);
      assertThat(dto.getUploadedByUsername()).isEqualTo("sara");
      verify(attachmentRepository).save(any());
    }

    @Test
    @DisplayName("file exceeding 10 MB — throws IllegalArgumentException")
    void upload_tooLarge_throws() {
      byte[] big = new byte[11 * 1024 * 1024];
      MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);

      assertThatThrownBy(() -> service.uploadAttachment(1L, file))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("10 MB");
    }

    @Test
    @DisplayName("unsupported content type — throws IllegalArgumentException")
    void upload_disallowedType_throws() {
      MockMultipartFile file = new MockMultipartFile(
          "file", "video.mp4", "video/mp4", new byte[1024]);

      assertThatThrownBy(() -> service.uploadAttachment(1L, file))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unsupported");
    }

    @Test
    @DisplayName("task not found — throws ResourceNotFoundException")
    void upload_taskNotFound_throws() {
      when(taskRepository.findById(999L)).thenReturn(Optional.empty());
      MockMultipartFile file = new MockMultipartFile("file", "f.png", "image/png", new byte[1]);

      assertThatThrownBy(() -> service.uploadAttachment(999L, file))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // ── List ───────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getAttachments")
  class ListAttachments {

    @Test
    @DisplayName("returns DTOs in reverse chronological order")
    void list_returnsMappedDTOs() {
      TaskAttachment a1 = buildAttachment(1L, "a.png", uploader);
      TaskAttachment a2 = buildAttachment(2L, "b.pdf", uploader);
      when(attachmentRepository.findByTaskIdOrderByCreatedAtDesc(1L))
          .thenReturn(List.of(a1, a2));

      List<TaskAttachmentDTO> dtos = service.getAttachments(1L);

      assertThat(dtos).hasSize(2);
      assertThat(dtos.get(0).getFileName()).isEqualTo("a.png");
    }

    @Test
    @DisplayName("task not found — throws ResourceNotFoundException")
    void list_taskNotFound_throws() {
      when(taskRepository.existsById(999L)).thenReturn(false);
      assertThatThrownBy(() -> service.getAttachments(999L))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // ── Delete ─────────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("deleteAttachment")
  class Delete {

    @Test
    @DisplayName("uploader can delete their own attachment")
    void delete_byUploader_succeeds() throws Exception {
      TaskAttachment a = buildAttachment(100L, "shot.png", uploader);
      // Create the file so deleteIfExists finds it
      Files.createFile(tempDir.resolve(a.getFilePath()));
      when(attachmentRepository.findById(100L)).thenReturn(Optional.of(a));

      service.deleteAttachment(1L, 100L);

      verify(attachmentRepository).delete(a);
    }

    @Test
    @DisplayName("ADMIN can delete any attachment")
    void delete_byAdmin_succeeds() throws Exception {
      User otherUser = new User();
      otherUser.setId(20L);
      otherUser.setUsername("ali");
      otherUser.setRole(UserRole.MEMBER);

      TaskAttachment a = buildAttachment(101L, "spec.pdf", otherUser);
      Files.createFile(tempDir.resolve(a.getFilePath()));
      when(attachmentRepository.findById(101L)).thenReturn(Optional.of(a));

      authenticateAs("admin");
      service.deleteAttachment(1L, 101L);

      verify(attachmentRepository).delete(a);
    }

    @Test
    @DisplayName("non-uploader non-admin — throws AccessDeniedException")
    void delete_byOtherUser_throws() {
      User other = new User();
      other.setId(20L);
      other.setUsername("ali");
      other.setRole(UserRole.MEMBER);
      when(userRepository.findByUsername("ali")).thenReturn(Optional.of(other));

      TaskAttachment a = buildAttachment(102L, "doc.txt", uploader);
      when(attachmentRepository.findById(102L)).thenReturn(Optional.of(a));

      authenticateAs("ali");
      assertThatThrownBy(() -> service.deleteAttachment(1L, 102L))
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("attachment belongs to different task — throws ResourceNotFoundException")
    void delete_wrongTask_throws() {
      Task otherTask = Task.builder().build();
      ReflectionTestUtils.setField(otherTask, "id", 99L);
      TaskAttachment a = buildAttachment(200L, "img.jpg", uploader);
      ReflectionTestUtils.setField(a, "task", otherTask);
      when(attachmentRepository.findById(200L)).thenReturn(Optional.of(a));

      assertThatThrownBy(() -> service.deleteAttachment(1L, 200L))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // ── Helper ─────────────────────────────────────────────────────────────────

  private TaskAttachment buildAttachment(Long id, String fileName, User owner) {
    String storedName = "uuid_" + fileName;
    TaskAttachment a = TaskAttachment.builder()
        .task(task).fileName(fileName).filePath(storedName)
        .fileSize(1024L).contentType("image/png").uploadedBy(owner).build();
    ReflectionTestUtils.setField(a, "id", id);
    ReflectionTestUtils.setField(a, "createdAt", LocalDateTime.now());
    return a;
  }
}
