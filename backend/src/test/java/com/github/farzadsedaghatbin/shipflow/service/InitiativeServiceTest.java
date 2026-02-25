package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreateInitiativeRequest;
import com.github.farzadsedaghatbin.shipflow.dto.InitiativeDTO;
import com.github.farzadsedaghatbin.shipflow.dto.ReorderRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BusinessValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.InitiativeStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class InitiativeServiceTest {

  @Mock
  private InitiativeRepository initiativeRepository;

  @Mock
  private EpicRepository epicRepository;

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication authentication;

  @InjectMocks
  private InitiativeService initiativeService;

  private Initiative testInitiative;
  private Project testProject;
  private User testOwner;
  private CreateInitiativeRequest testRequest;

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
        .description("Strategic initiative for mobile platform")
        .status(InitiativeStatus.PLANNED)
        .color("#3B82F6")
        .targetStartDate(LocalDate.of(2026, 1, 1))
        .targetEndDate(LocalDate.of(2026, 6, 30))
        .project(testProject)
        .owner(testOwner)
        .sortOrder(1)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    testRequest = new CreateInitiativeRequest();
    testRequest.setName("New Initiative");
    testRequest.setDescription("Test initiative description");
    testRequest.setStatus(InitiativeStatus.DRAFT);
    testRequest.setColor("#10B981");
    testRequest.setProjectId(1L);
    testRequest.setOwnerId(1L);
    testRequest.setTargetStartDate(LocalDate.of(2026, 3, 1));
    testRequest.setTargetEndDate(LocalDate.of(2026, 9, 30));
    testRequest.setSortOrder(2);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getAllInitiatives_ShouldReturnAllNotDeletedInitiatives() {
    when(initiativeRepository.findAllNotDeleted()).thenReturn(Arrays.asList(testInitiative));
    when(epicRepository.countByInitiativeIdNotDeleted(any())).thenReturn(3L);
    when(epicRepository.countByInitiativeIdAndStatusNotDeleted(any(), eq(EpicStatus.COMPLETED))).thenReturn(1L);

    List<InitiativeDTO> result = initiativeService.getAllInitiatives();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Mobile Experience 2026");
    assertThat(result.get(0).getEpicCount()).isEqualTo(3);
    assertThat(result.get(0).getCompletedEpicCount()).isEqualTo(1);
    verify(initiativeRepository).findAllNotDeleted();
  }

  @Test
  void getInitiativesByProjectId_ShouldReturnProjectInitiatives() {
    when(initiativeRepository.findByProjectIdNotDeleted(1L)).thenReturn(Arrays.asList(testInitiative));
    when(epicRepository.countByInitiativeIdNotDeleted(any())).thenReturn(2L);
    when(epicRepository.countByInitiativeIdAndStatusNotDeleted(any(), eq(EpicStatus.COMPLETED))).thenReturn(0L);

    List<InitiativeDTO> result = initiativeService.getInitiativesByProjectId(1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getProjectId()).isEqualTo(1L);
    verify(initiativeRepository).findByProjectIdNotDeleted(1L);
  }

  @Test
  void getInitiativeById_WhenExists_ShouldReturnInitiative() {
    when(initiativeRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testInitiative));
    when(epicRepository.countByInitiativeIdNotDeleted(any())).thenReturn(0L);
    when(epicRepository.countByInitiativeIdAndStatusNotDeleted(any(), any())).thenReturn(0L);

    InitiativeDTO result = initiativeService.getInitiativeById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Mobile Experience 2026");
    assertThat(result.getStatus()).isEqualTo(InitiativeStatus.PLANNED);
  }

  @Test
  void getInitiativeById_WhenNotExists_ShouldThrowException() {
    when(initiativeRepository.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> initiativeService.getInitiativeById(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Initiative not found");
  }

  @Test
  void createInitiative_ShouldSaveAndReturnInitiative() {
    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));
    when(initiativeRepository.save(any(Initiative.class))).thenReturn(testInitiative);
    when(epicRepository.countByInitiativeIdNotDeleted(any())).thenReturn(0L);
    when(epicRepository.countByInitiativeIdAndStatusNotDeleted(any(), any())).thenReturn(0L);

    InitiativeDTO result = initiativeService.createInitiative(testRequest);

    assertThat(result).isNotNull();
    verify(initiativeRepository).save(any(Initiative.class));
  }

  @Test
  void createInitiative_WhenProjectNotFound_ShouldThrowException() {
    when(projectRepository.findById(999L)).thenReturn(Optional.empty());
    testRequest.setProjectId(999L);

    assertThatThrownBy(() -> initiativeService.createInitiative(testRequest))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Project not found");
  }

  @Test
  void updateInitiative_WhenExists_ShouldUpdateAndReturn() {
    when(initiativeRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testInitiative));
    when(userRepository.findById(1L)).thenReturn(Optional.of(testOwner));
    when(initiativeRepository.save(any(Initiative.class))).thenReturn(testInitiative);
    when(epicRepository.countByInitiativeIdNotDeleted(any())).thenReturn(0L);
    when(epicRepository.countByInitiativeIdAndStatusNotDeleted(any(), any())).thenReturn(0L);

    testRequest.setName("Updated Initiative");
    InitiativeDTO result = initiativeService.updateInitiative(1L, testRequest);

    assertThat(result).isNotNull();
    verify(initiativeRepository).save(any(Initiative.class));
  }

  @Test
  void deleteInitiative_ShouldSoftDelete() {
    // Setup security context
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("testowner");
    SecurityContextHolder.setContext(securityContext);

    when(initiativeRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testInitiative));
    when(userRepository.findByUsername("testowner")).thenReturn(Optional.of(testOwner));
    when(initiativeRepository.save(any(Initiative.class))).thenReturn(testInitiative);

    initiativeService.deleteInitiative(1L);

    verify(initiativeRepository).save(any(Initiative.class));
  }

  @Test
  void updateStatus_ShouldUpdateAndReturn() {
    when(initiativeRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testInitiative));
    when(initiativeRepository.save(any(Initiative.class))).thenReturn(testInitiative);
    when(epicRepository.countByInitiativeIdNotDeleted(any())).thenReturn(0L);
    when(epicRepository.countByInitiativeIdAndStatusNotDeleted(any(), any())).thenReturn(0L);

    InitiativeDTO result = initiativeService.updateStatus(1L, InitiativeStatus.IN_PROGRESS);

    assertThat(result).isNotNull();
    verify(initiativeRepository).save(any(Initiative.class));
  }

  @Test
  void calculateProgress_WithEpics_ShouldReturnCorrectPercentage() {
    when(epicRepository.countByInitiativeIdNotDeleted(1L)).thenReturn(4L);
    when(epicRepository.countByInitiativeIdAndStatusNotDeleted(1L, EpicStatus.COMPLETED)).thenReturn(2L);

    double progress = initiativeService.calculateProgress(1L);

    assertThat(progress).isEqualTo(50.0);
  }

  @Test
  void calculateProgress_WithNoEpics_ShouldReturnZero() {
    when(epicRepository.countByInitiativeIdNotDeleted(1L)).thenReturn(0L);

    double progress = initiativeService.calculateProgress(1L);

    assertThat(progress).isEqualTo(0.0);
  }

  @Test
  void getActiveInitiatives_ShouldReturnPlannedAndInProgress() {
    when(initiativeRepository.findByProjectIdAndStatusInNotDeleted(eq(1L), any()))
        .thenReturn(Arrays.asList(testInitiative));
    when(epicRepository.countByInitiativeIdNotDeleted(any())).thenReturn(0L);
    when(epicRepository.countByInitiativeIdAndStatusNotDeleted(any(), any())).thenReturn(0L);

    List<InitiativeDTO> result = initiativeService.getActiveInitiatives(1L);

    assertThat(result).hasSize(1);
    verify(initiativeRepository).findByProjectIdAndStatusInNotDeleted(eq(1L), any());
  }

  @Test
  void createInitiative_WithPriority_ShouldPersistPriority() {
    CreateInitiativeRequest req = new CreateInitiativeRequest();
    req.setName("New Initiative"); req.setStatus(InitiativeStatus.PLANNED);
    req.setProjectId(1L); req.setSortOrder(1); req.setPriority(BusinessValue.HIGH);

    Initiative saved = Initiative.builder()
        .id(2L).name("New Initiative").status(InitiativeStatus.PLANNED)
        .project(testProject).sortOrder(1).priority(BusinessValue.HIGH)
        .build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(initiativeRepository.save(any(Initiative.class))).thenReturn(saved);
    when(epicRepository.countByInitiativeIdNotDeleted(any())).thenReturn(0L);
    when(epicRepository.countByInitiativeIdAndStatusNotDeleted(any(), any())).thenReturn(0L);

    InitiativeDTO result = initiativeService.createInitiative(req);

    assertThat(result.getPriority()).isEqualTo(BusinessValue.HIGH);
    verify(initiativeRepository).save(argThat(i -> i.getPriority() == BusinessValue.HIGH));
  }

  @Test
  void reorder_ShouldUpdateSortOrderForEachInitiative() {
    Initiative init1 = Initiative.builder().id(1L).name("Init 1")
        .status(InitiativeStatus.PLANNED).project(testProject).sortOrder(0).build();
    Initiative init2 = Initiative.builder().id(2L).name("Init 2")
        .status(InitiativeStatus.PLANNED).project(testProject).sortOrder(1).build();

    when(initiativeRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(init1));
    when(initiativeRepository.findByIdNotDeleted(2L)).thenReturn(Optional.of(init2));
    when(initiativeRepository.save(any(Initiative.class))).thenAnswer(inv -> inv.getArgument(0));

    ReorderRequest request = ReorderRequest.builder()
        .items(Arrays.asList(
            ReorderRequest.ReorderItem.builder().id(1L).sortOrder(1).build(),
            ReorderRequest.ReorderItem.builder().id(2L).sortOrder(0).build()
        ))
        .build();

    initiativeService.reorder(request);

    verify(initiativeRepository).save(argThat(i -> i.getId().equals(1L) && i.getSortOrder() == 1));
    verify(initiativeRepository).save(argThat(i -> i.getId().equals(2L) && i.getSortOrder() == 0));
  }

  @Test
  void reorder_WhenInitiativeNotFound_ShouldThrowException() {
    when(initiativeRepository.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

    ReorderRequest request = ReorderRequest.builder()
        .items(List.of(ReorderRequest.ReorderItem.builder().id(999L).sortOrder(0).build()))
        .build();

    assertThatThrownBy(() -> initiativeService.reorder(request))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Initiative not found");
  }
}
