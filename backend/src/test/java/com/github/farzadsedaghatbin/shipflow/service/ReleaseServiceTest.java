package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreateReleaseRequest;
import com.github.farzadsedaghatbin.shipflow.dto.ReleaseDTO;
import com.github.farzadsedaghatbin.shipflow.dto.ReleaseProgressDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Release;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseRiskLevel;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ReleaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
class ReleaseServiceTest {

  @Mock
  private ReleaseRepository releaseRepository;

  @Mock
  private CycleRepository cycleRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private BugReportRepository bugReportRepository;

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
  private ReleaseService releaseService;

  private Release testRelease;
  private Project testProject;
  private Cycle testCycle;
  private User testUser;
  private CreateReleaseRequest testRequest;

  @BeforeEach
  void setUp() {
    testProject = Project.builder()
        .id(1L)
        .name("Test Project")
        .projectKey("TST")
        .isActive(true)
        .build();

    testUser = User.builder()
        .id(1L)
        .username("testuser")
        .role(UserRole.ADMIN)
        .build();

    testCycle = Cycle.builder()
        .id(1L)
        .name("Q1 2026 Sprint")
        .project(testProject)
        .startDate(LocalDate.of(2026, 1, 1))
        .endDate(LocalDate.of(2026, 2, 14))
        .phase(CyclePhase.BUILD)
        .isActive(true)
        .build();

    Set<Cycle> cycles = new HashSet<>();
    cycles.add(testCycle);

    testRelease = Release.builder()
        .id(1L)
        .name("February 2026 Release")
        .version("v2.4.0")
        .description("Major feature release with mobile checkout")
        .status(ReleaseStatus.PLANNING)
        .riskLevel(ReleaseRiskLevel.MEDIUM)
        .targetDate(LocalDate.of(2026, 2, 28))
        .project(testProject)
        .cycles(cycles)
        .sortOrder(1)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    testRequest = new CreateReleaseRequest();
    testRequest.setName("New Release");
    testRequest.setVersion("v2.5.0");
    testRequest.setDescription("Test release description");
    testRequest.setStatus(ReleaseStatus.PLANNING);
    testRequest.setRiskLevel(ReleaseRiskLevel.LOW);
    testRequest.setProjectId(1L);
    testRequest.setTargetDate(LocalDate.of(2026, 3, 31));
    testRequest.setCycleIds(Arrays.asList(1L));
    testRequest.setSortOrder(2);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getReleasesByProjectId_ShouldReturnProjectReleases() {
    when(releaseRepository.findByProjectIdNotDeleted(1L)).thenReturn(Arrays.asList(testRelease));

    List<ReleaseDTO> result = releaseService.getReleasesByProjectId(1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("February 2026 Release");
    assertThat(result.get(0).getVersion()).isEqualTo("v2.4.0");
    verify(releaseRepository).findByProjectIdNotDeleted(1L);
  }

  @Test
  void getReleaseById_WhenExists_ShouldReturnRelease() {
    when(releaseRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testRelease));

    ReleaseDTO result = releaseService.getReleaseById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("February 2026 Release");
    assertThat(result.getStatus()).isEqualTo(ReleaseStatus.PLANNING);
    assertThat(result.getRiskLevel()).isEqualTo(ReleaseRiskLevel.MEDIUM);
  }

  @Test
  void getReleaseById_WhenNotExists_ShouldThrowException() {
    when(releaseRepository.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> releaseService.getReleaseById(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Release not found");
  }

  @Test
  void createRelease_ShouldSaveAndReturnRelease() {
    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(releaseRepository.save(any(Release.class))).thenReturn(testRelease);

    ReleaseDTO result = releaseService.createRelease(testRequest);

    assertThat(result).isNotNull();
    verify(releaseRepository).save(any(Release.class));
  }

  @Test
  void createRelease_WhenProjectNotFound_ShouldThrowException() {
    when(projectRepository.findById(999L)).thenReturn(Optional.empty());
    testRequest.setProjectId(999L);

    assertThatThrownBy(() -> releaseService.createRelease(testRequest))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Project not found");
  }

  @Test
  void createRelease_WithNoCycles_ShouldSucceed() {
    testRequest.setCycleIds(null);
    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(releaseRepository.save(any(Release.class))).thenReturn(testRelease);

    ReleaseDTO result = releaseService.createRelease(testRequest);

    assertThat(result).isNotNull();
    verify(cycleRepository, never()).findById(any());
  }

  @Test
  void updateRelease_WhenExists_ShouldUpdateAndReturn() {
    when(releaseRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testRelease));
    when(releaseRepository.save(any(Release.class))).thenReturn(testRelease);

    testRequest.setName("Updated Release");
    ReleaseDTO result = releaseService.updateRelease(1L, testRequest);

    assertThat(result).isNotNull();
    verify(releaseRepository).save(any(Release.class));
  }

  @Test
  void deleteRelease_ShouldSoftDelete() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("testuser");
    SecurityContextHolder.setContext(securityContext);

    when(releaseRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testRelease));
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(releaseRepository.save(any(Release.class))).thenReturn(testRelease);

    releaseService.deleteRelease(1L);

    verify(releaseRepository).save(any(Release.class));
  }

  @Test
  void updateStatus_ShouldUpdateAndReturn() {
    when(releaseRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testRelease));
    when(releaseRepository.save(any(Release.class))).thenReturn(testRelease);

    ReleaseDTO result = releaseService.updateStatus(1L, ReleaseStatus.IN_PROGRESS);

    assertThat(result).isNotNull();
    verify(releaseRepository).save(any(Release.class));
  }

  @Test
  void calculateProgress_ShouldReturnProgressDTO() {
    when(releaseRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testRelease));
    when(pitchRepository.findByTargetReleaseIdNotDeleted(1L)).thenReturn(Arrays.asList());
    when(taskRepository.findByTargetReleaseIdNotDeleted(1L)).thenReturn(Arrays.asList());
    when(bugReportRepository.findByTargetReleaseId(1L)).thenReturn(Arrays.asList());

    ReleaseProgressDTO result = releaseService.calculateProgress(1L);

    assertThat(result).isNotNull();
  }

  @Test
  void getReleasesByProjectIdAndStatus_ShouldFilterByStatus() {
    when(releaseRepository.findByProjectIdAndStatusNotDeleted(1L, ReleaseStatus.PLANNING))
        .thenReturn(Arrays.asList(testRelease));

    List<ReleaseDTO> result = releaseService.getReleasesByProjectIdAndStatus(1L, ReleaseStatus.PLANNING);

    assertThat(result).hasSize(1);
    verify(releaseRepository).findByProjectIdAndStatusNotDeleted(1L, ReleaseStatus.PLANNING);
  }
}
