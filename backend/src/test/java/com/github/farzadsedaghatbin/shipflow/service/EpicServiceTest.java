package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreateEpicRequest;
import com.github.farzadsedaghatbin.shipflow.dto.EpicDTO;
import com.github.farzadsedaghatbin.shipflow.dto.ReorderRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BusinessValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.InitiativeStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class EpicServiceTest {

  @Mock
  private EpicRepository epicRepository;

  @Mock
  private InitiativeRepository initiativeRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private EpicService epicService;

  private Epic testEpic;
  private Initiative testInitiative;
  private Project testProject;
  private User testOwner;
  private CreateEpicRequest testRequest;

  @BeforeEach
  void setUp() {
    testProject = Project.builder()
        .id(1L)
        .name("Test Project")
        .projectKey("TST")
        .isActive(true)
        .build();

    testOwner = User.builder()
        .id(1L)
        .username("testowner")
        .role(UserRole.ADMIN)
        .build();

    testInitiative = Initiative.builder()
        .id(1L)
        .name("Mobile Experience 2026")
        .status(InitiativeStatus.PLANNED)
        .project(testProject)
        .build();

    testEpic = Epic.builder()
        .id(1L)
        .name("Mobile Checkout Redesign")
        .description("Complete redesign of mobile checkout flow")
        .status(EpicStatus.IN_PROGRESS)
        .color("#8B5CF6")
        .targetStartDate(LocalDate.of(2026, 2, 1))
        .targetEndDate(LocalDate.of(2026, 4, 30))
        .project(testProject)
        .initiative(testInitiative)
        .owner(testOwner)
        .sortOrder(1)
        .pitches(new ArrayList<>())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    testRequest = new CreateEpicRequest();
    testRequest.setName("New Epic");
    testRequest.setDescription("Test epic description");
    testRequest.setStatus(EpicStatus.DRAFT);
    testRequest.setColor("#10B981");
    testRequest.setProjectId(1L);
    testRequest.setInitiativeId(1L);
    testRequest.setOwnerId(1L);
    testRequest.setTargetStartDate(LocalDate.of(2026, 3, 1));
    testRequest.setTargetEndDate(LocalDate.of(2026, 5, 31));
    testRequest.setSortOrder(2);
  }

  @Test
  void getEpicsByProjectId_ShouldReturnProjectEpics() {
    Pitch donePitch = Pitch.builder().id(1L).status(PitchStatus.DONE).build();
    Pitch wipPitch = Pitch.builder().id(2L).status(PitchStatus.IN_PROGRESS).build();
    testEpic.setPitches(Arrays.asList(donePitch, wipPitch));
    
    when(epicRepository.findByProjectIdNotDeleted(1L)).thenReturn(Arrays.asList(testEpic));

    List<EpicDTO> result = epicService.getEpicsByProjectId(1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Mobile Checkout Redesign");
    assertThat(result.get(0).getPitchCount()).isEqualTo(2);
    assertThat(result.get(0).getCompletedPitchCount()).isEqualTo(1);
    verify(epicRepository).findByProjectIdNotDeleted(1L);
  }

  @Test
  void getEpicsByInitiativeId_ShouldReturnInitiativeEpics() {
    when(epicRepository.findByInitiativeIdNotDeleted(1L)).thenReturn(Arrays.asList(testEpic));

    List<EpicDTO> result = epicService.getEpicsByInitiativeId(1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getInitiativeId()).isEqualTo(1L);
    verify(epicRepository).findByInitiativeIdNotDeleted(1L);
  }

  @Test
  void getEpicById_WhenExists_ShouldReturnEpic() {
    when(epicRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testEpic));

    EpicDTO result = epicService.getEpicById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Mobile Checkout Redesign");
    assertThat(result.getStatus()).isEqualTo(EpicStatus.IN_PROGRESS);
  }

  @Test
  void getEpicById_WhenNotExists_ShouldThrowException() {
    when(epicRepository.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> epicService.getEpicById(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Epic not found");
  }

  @Test
  void createEpic_ShouldSaveAndReturnEpic() {
    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(initiativeRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testInitiative));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));
    when(epicRepository.save(any(Epic.class))).thenReturn(testEpic);

    EpicDTO result = epicService.createEpic(testRequest);

    assertThat(result).isNotNull();
    verify(epicRepository).save(any(Epic.class));
  }

  @Test
  void createEpic_WhenProjectNotFound_ShouldThrowException() {
    when(projectRepository.findById(999L)).thenReturn(Optional.empty());
    testRequest.setProjectId(999L);

    assertThatThrownBy(() -> epicService.createEpic(testRequest))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Project not found");
  }

  @Test
  void createEpic_WithoutInitiative_ShouldSucceed() {
    testRequest.setInitiativeId(null);
    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));
    when(epicRepository.save(any(Epic.class))).thenReturn(testEpic);

    EpicDTO result = epicService.createEpic(testRequest);

    assertThat(result).isNotNull();
    verify(initiativeRepository, never()).findByIdNotDeleted(any());
  }

  @Test
  void updateEpic_WhenExists_ShouldUpdateAndReturn() {
    when(epicRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testEpic));
    when(initiativeRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testInitiative));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));
    when(epicRepository.save(any(Epic.class))).thenReturn(testEpic);

    testEpic.setName("Updated Epic");
    EpicDTO result = epicService.updateEpic(1L, testRequest);

    assertThat(result).isNotNull();
    verify(epicRepository).save(any(Epic.class));
  }

  @Test
  void updateStatus_ShouldUpdateAndReturn() {
    when(epicRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testEpic));
    when(epicRepository.save(any(Epic.class))).thenReturn(testEpic);

    EpicDTO result = epicService.updateStatus(1L, EpicStatus.COMPLETED);

    assertThat(result).isNotNull();
    verify(epicRepository).save(any(Epic.class));
  }

  @Test
  void deleteEpic_WhenExists_ShouldSoftDelete() {
    when(epicRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testEpic));
    when(epicRepository.save(any(Epic.class))).thenReturn(testEpic);

    epicService.deleteEpic(1L);

    verify(epicRepository).save(argThat(epic -> epic.getDeletedAt() != null));
  }

  @Test
  void calculateProgress_WithPitches_ShouldReturnCorrectPercentage() {
    Pitch donePitch1 = Pitch.builder().id(1L).status(PitchStatus.DONE).build();
    Pitch donePitch2 = Pitch.builder().id(2L).status(PitchStatus.DONE).build();
    Pitch wipPitch = Pitch.builder().id(3L).status(PitchStatus.IN_PROGRESS).build();
    Pitch plannedPitch = Pitch.builder().id(4L).status(PitchStatus.SHAPED).build();
    testEpic.setPitches(Arrays.asList(donePitch1, donePitch2, wipPitch, plannedPitch));

    when(epicRepository.findByIdWithPitchesNotDeleted(1L)).thenReturn(Optional.of(testEpic));

    double progress = epicService.calculateProgress(1L);

    assertThat(progress).isEqualTo(50.0);
  }

  @Test
  void calculateProgress_WithNoPitches_ShouldReturnZero() {
    testEpic.setPitches(new ArrayList<>());
    when(epicRepository.findByIdWithPitchesNotDeleted(1L)).thenReturn(Optional.of(testEpic));

    double progress = epicService.calculateProgress(1L);

    assertThat(progress).isEqualTo(0.0);
  }

  @Test
  void getActiveEpics_ShouldReturnPlannedAndInProgress() {
    Epic plannedEpic = Epic.builder()
        .id(2L)
        .name("Planned Epic")
        .status(EpicStatus.PLANNED)
        .project(testProject)
        .pitches(new ArrayList<>())
        .build();
    
    when(epicRepository.findByProjectIdAndStatusInNotDeleted(any(), any()))
        .thenReturn(Arrays.asList(testEpic, plannedEpic));

    List<EpicDTO> result = epicService.getActiveEpics(1L);

    assertThat(result).hasSize(2);
    verify(epicRepository).findByProjectIdAndStatusInNotDeleted(any(), any());
  }

  @Test
  void createEpic_WithPriority_ShouldPersistPriority() {
    testRequest.setPriority(BusinessValue.HIGH);

    Epic savedEpic = Epic.builder()
        .id(1L).name("New Epic").description("Test epic description")
        .status(EpicStatus.DRAFT).color("#10B981")
        .project(testProject).initiative(testInitiative)
        .priority(BusinessValue.HIGH).sortOrder(2)
        .pitches(new ArrayList<>())
        .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(initiativeRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testInitiative));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));
    when(epicRepository.save(any(Epic.class))).thenReturn(savedEpic);

    EpicDTO result = epicService.createEpic(testRequest);

    assertThat(result.getPriority()).isEqualTo(BusinessValue.HIGH);
    verify(epicRepository).save(argThat(e -> e.getPriority() == BusinessValue.HIGH));
  }

  @Test
  void reorder_ShouldUpdateSortOrderForEachEpic() {
    Epic epic1 = Epic.builder().id(1L).name("Epic 1").status(EpicStatus.DRAFT)
        .project(testProject).pitches(new ArrayList<>()).build();
    Epic epic2 = Epic.builder().id(2L).name("Epic 2").status(EpicStatus.DRAFT)
        .project(testProject).pitches(new ArrayList<>()).build();

    when(epicRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(epic1));
    when(epicRepository.findByIdNotDeleted(2L)).thenReturn(Optional.of(epic2));
    when(epicRepository.save(any(Epic.class))).thenAnswer(inv -> inv.getArgument(0));

    ReorderRequest request = ReorderRequest.builder()
        .items(Arrays.asList(
            ReorderRequest.ReorderItem.builder().id(1L).sortOrder(0).build(),
            ReorderRequest.ReorderItem.builder().id(2L).sortOrder(1).build()
        ))
        .build();

    epicService.reorder(request);

    verify(epicRepository).save(argThat(e -> e.getId().equals(1L) && e.getSortOrder() == 0));
    verify(epicRepository).save(argThat(e -> e.getId().equals(2L) && e.getSortOrder() == 1));
  }

  @Test
  void reorder_WhenEpicNotFound_ShouldThrowException() {
    when(epicRepository.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

    ReorderRequest request = ReorderRequest.builder()
        .items(List.of(ReorderRequest.ReorderItem.builder().id(999L).sortOrder(0).build()))
        .build();

    assertThatThrownBy(() -> epicService.reorder(request))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Epic not found");
  }
}
