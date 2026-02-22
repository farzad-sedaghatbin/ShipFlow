package com.github.farzadsedaghatbin.shipflow.service.retro;

import static com.github.farzadsedaghatbin.shipflow.service.retro.RetroTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreateRetroRequest;
import com.github.farzadsedaghatbin.shipflow.dto.RetroDTO;
import com.github.farzadsedaghatbin.shipflow.dto.UpdateRetroRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Retrospective;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.github.farzadsedaghatbin.shipflow.service.LocalizationService;
import com.github.farzadsedaghatbin.shipflow.service.MessageService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetroCrudService")
class RetroCrudServiceTest {

  @Mock private RetroRepository retroRepository;
  @Mock private RetroItemRepository retroItemRepository;
  @Mock private CycleRepository cycleRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private UserRepository userRepository;
  @Mock private LocalizationService localizationService;
  @Mock private MessageService messageService;
  @Mock private RetroMapper retroMapper;

  @InjectMocks private RetroCrudService service;

  private Project testProject;
  private Cycle testCycle;
  private User testUser;

  @BeforeEach
  void setUp() {
    testProject = aProject().build();
    testCycle = aCycle().withProject(testProject).build();
    testUser = aUser().build();
    
    setupSecurityContext("testuser");
    setupMessageMocks();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setupSecurityContext(String username) {
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = mock(Authentication.class);
    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    lenient().when(authentication.getName()).thenReturn(username);
    SecurityContextHolder.setContext(securityContext);
  }

  private void setupMessageMocks() {
    lenient().when(messageService.getMessage(any(String.class)))
        .thenAnswer(i -> i.getArgument(0));
    lenient().when(localizationService.getMessage(any(String.class)))
        .thenAnswer(i -> i.getArgument(0));
  }

  // ==================== TEAM SETTINGS TESTS ====================

  @Nested
  @DisplayName("Project Settings")
  class ProjectSettingsTests {

    @Test
    @DisplayName("isRetrospectivesEnabled returns true when enabled")
    void isRetrospectivesEnabled_WhenEnabled_ReturnsTrue() {
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

      assertThat(service.isRetrospectivesEnabled(1L)).isTrue();
    }

    @Test
    @DisplayName("isRetrospectivesEnabled returns false when disabled")
    void isRetrospectivesEnabled_WhenDisabled_ReturnsFalse() {
      testProject.setEnableRetrospectives(false);
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

      assertThat(service.isRetrospectivesEnabled(1L)).isFalse();
    }

    @Test
    @DisplayName("setRetrospectivesEnabled updates project setting")
    void setRetrospectivesEnabled_UpdatesProjectSetting() {
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(projectRepository.save(any())).thenReturn(testProject);

      service.setRetrospectivesEnabled(1L, false);

      verify(projectRepository).save(testProject);
      assertThat(testProject.getEnableRetrospectives()).isFalse();
    }

    @Test
    @DisplayName("getAllRetrosByProject returns empty list when retrospectives disabled")
    void getAllRetros_WhenDisabled_ReturnsEmptyList() {
      testProject.setEnableRetrospectives(false);
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

      List<RetroDTO> result = service.getAllRetrosByProject(1L);
      
      assertThat(result).isEmpty();
      verify(retroRepository, never()).findByProjectIdOrderByCreatedAtDesc(anyLong());
    }
  }

  // ==================== CREATE TESTS ====================

  @Nested
  @DisplayName("Create Retro")
  class CreateTests {

    @Test
    @DisplayName("createRetro succeeds with valid request")
    void createRetro_WithValidRequest_Succeeds() {
      Retrospective savedRetro = aRetro()
          .withCycle(testCycle)
          .withProject(testProject)
          .createdBy(testUser)
          .build();

      RetroDTO expectedDto = RetroDTO.builder()
          .id(1L)
          .title("Test Retro")
          .status(RetroStatus.DRAFT)
          .build();

      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
      when(retroRepository.save(any(Retrospective.class))).thenReturn(savedRetro);
      when(retroMapper.toDTO(any(Retrospective.class), eq(0L))).thenReturn(expectedDto);

      CreateRetroRequest request = CreateRetroRequest.builder()
          .title("Test Retro")
          .notes("Notes")
          .cycleId(1L)
          .projectId(1L)
          .build();

      RetroDTO result = service.createRetro(request);

      assertThat(result).isNotNull();
      assertThat(result.getTitle()).isEqualTo("Test Retro");
      verify(retroRepository).save(any(Retrospective.class));
    }

    @Test
    @DisplayName("createRetro throws when cycle not found")
    void createRetro_WhenCycleNotFound_Throws() {
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(cycleRepository.findById(999L)).thenReturn(Optional.empty());

      CreateRetroRequest request = CreateRetroRequest.builder()
          .title("Test")
          .cycleId(999L)
          .projectId(1L)
          .build();

      assertThatThrownBy(() -> service.createRetro(request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Cycle not found");
    }
  }

  // ==================== READ TESTS ====================

  @Nested
  @DisplayName("Read Operations")
  class ReadTests {

    @Test
    @DisplayName("getAllRetrosByProject returns DTOs with batched item counts")
    void getAllRetrosByProject_ReturnsDTOsWithBatchedCounts() {
      Retrospective retro1 = aRetro().withId(1L).withProject(testProject).build();
      Retrospective retro2 = aRetro().withId(2L).withProject(testProject).build();
      List<Retrospective> retros = Arrays.asList(retro1, retro2);

      RetroDTO dto1 = RetroDTO.builder().id(1L).itemCount(3).build();
      RetroDTO dto2 = RetroDTO.builder().id(2L).itemCount(5).build();

      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(retroRepository.findByProjectIdOrderByCreatedAtDesc(1L)).thenReturn(retros);
      when(retroMapper.toDTOBatch(retros)).thenReturn(Arrays.asList(dto1, dto2));

      List<RetroDTO> result = service.getAllRetrosByProject(1L);

      assertThat(result).hasSize(2);
      verify(retroMapper).toDTOBatch(retros); // Uses batch method, not N+1
    }

    @Test
    @DisplayName("getRetroById returns DTO")
    void getRetroById_ReturnsDTO() {
      Retrospective retro = aRetro().withProject(testProject).build();
      RetroDTO expectedDto = RetroDTO.builder().id(1L).title("Test Retro").build();

      when(retroRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(retro));
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(retroItemRepository.countByRetrospectiveId(1L)).thenReturn(5L);
      when(retroMapper.toDTO(retro, 5L)).thenReturn(expectedDto);

      RetroDTO result = service.getRetroById(1L);

      assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getRetroById throws when not found")
    void getRetroById_WhenNotFound_Throws() {
      when(retroRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getRetroById(999L))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Retrospective not found");
    }
  }

  // ==================== UPDATE TESTS ====================

  @Nested
  @DisplayName("Update Operations")
  class UpdateTests {

    @Test
    @DisplayName("updateRetro updates title and notes")
    void updateRetro_UpdatesTitleAndNotes() {
      Retrospective retro = aRetro().draft().withProject(testProject).build();
      RetroDTO expectedDto = RetroDTO.builder()
          .id(1L)
          .title("Updated Title")
          .notes("Updated Notes")
          .build();

      when(retroRepository.findById(1L)).thenReturn(Optional.of(retro));
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(retroRepository.save(any())).thenReturn(retro);
      when(retroItemRepository.countByRetrospectiveId(1L)).thenReturn(0L);
      when(retroMapper.toDTO(any(), eq(0L))).thenReturn(expectedDto);

      UpdateRetroRequest request = UpdateRetroRequest.builder()
          .title("Updated Title")
          .notes("Updated Notes")
          .build();

      RetroDTO result = service.updateRetro(1L, request);

      assertThat(result.getTitle()).isEqualTo("Updated Title");
      verify(retroRepository).save(retro);
    }

    @ParameterizedTest
    @EnumSource(value = RetroStatus.class, names = {"DRAFT", "OPEN"})
    @DisplayName("updateRetro succeeds for non-closed statuses")
    void updateRetro_SucceedsForNonClosedStatus(RetroStatus status) {
      Retrospective retro = aRetro().withStatus(status).withProject(testProject).build();
      RetroDTO dto = RetroDTO.builder().id(1L).build();

      when(retroRepository.findById(1L)).thenReturn(Optional.of(retro));
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(retroRepository.save(any())).thenReturn(retro);
      when(retroItemRepository.countByRetrospectiveId(1L)).thenReturn(0L);
      when(retroMapper.toDTO(any(), eq(0L))).thenReturn(dto);

      UpdateRetroRequest request = UpdateRetroRequest.builder().title("New").build();

      service.updateRetro(1L, request); // Should not throw

      verify(retroRepository).save(any());
    }

    @Test
    @DisplayName("updateRetro throws for closed retro")
    void updateRetro_WhenClosed_Throws() {
      Retrospective retro = aRetro().closed().withProject(testProject).build();
      
      when(retroRepository.findById(1L)).thenReturn(Optional.of(retro));
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

      UpdateRetroRequest request = UpdateRetroRequest.builder().title("New").build();

      assertThatThrownBy(() -> service.updateRetro(1L, request))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  // ==================== LIFECYCLE TESTS ====================

  @Nested
  @DisplayName("Lifecycle Operations")
  class LifecycleTests {

    @Test
    @DisplayName("openRetro changes status to OPEN")
    void openRetro_ChangesStatusToOpen() {
      Retrospective retro = aRetro().draft().withProject(testProject).build();
      RetroDTO dto = RetroDTO.builder().id(1L).status(RetroStatus.OPEN).build();

      when(retroRepository.findById(1L)).thenReturn(Optional.of(retro));
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(retroRepository.save(any())).thenAnswer(i -> i.getArgument(0));
      when(retroItemRepository.countByRetrospectiveId(1L)).thenReturn(0L);
      when(retroMapper.toDTO(any(), eq(0L))).thenReturn(dto);

      service.openRetro(1L);

      assertThat(retro.getStatus()).isEqualTo(RetroStatus.OPEN);
    }

    @Test
    @DisplayName("openRetro throws for closed retro")
    void openRetro_WhenClosed_Throws() {
      Retrospective retro = aRetro().closed().withProject(testProject).build();
      
      when(retroRepository.findById(1L)).thenReturn(Optional.of(retro));
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

      assertThatThrownBy(() -> service.openRetro(1L))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("closeRetro changes status to CLOSED and sets closedAt")
    void closeRetro_ChangesStatusAndSetsClosedAt() {
      Retrospective retro = aRetro().open().withProject(testProject).build();
      RetroDTO dto = RetroDTO.builder().id(1L).status(RetroStatus.CLOSED).build();

      when(retroRepository.findById(1L)).thenReturn(Optional.of(retro));
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(retroRepository.save(any())).thenAnswer(i -> i.getArgument(0));
      when(retroItemRepository.countByRetrospectiveId(1L)).thenReturn(0L);
      when(retroMapper.toDTO(any(), eq(0L))).thenReturn(dto);

      service.closeRetro(1L);

      assertThat(retro.getStatus()).isEqualTo(RetroStatus.CLOSED);
      assertThat(retro.getClosedAt()).isNotNull();
    }
  }

  // ==================== DELETE TESTS ====================

  @Nested
  @DisplayName("Delete Operations")
  class DeleteTests {

    @Test
    @DisplayName("deleteRetro removes retrospective")
    void deleteRetro_RemovesRetrospective() {
      when(retroRepository.existsById(1L)).thenReturn(true);
      doNothing().when(retroRepository).deleteById(1L);

      service.deleteRetro(1L);

      verify(retroRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteRetro throws when not found")
    void deleteRetro_WhenNotFound_Throws() {
      when(retroRepository.existsById(999L)).thenReturn(false);

      assertThatThrownBy(() -> service.deleteRetro(999L))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }
}
