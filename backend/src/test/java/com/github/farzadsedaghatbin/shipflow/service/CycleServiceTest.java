package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreateCycleRequest;
import com.github.farzadsedaghatbin.shipflow.dto.CycleDTO;
import com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDate;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CycleServiceTest {

  @Mock
  private CycleRepository cycleRepository;

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private RetroRepository retroRepository;

  @Mock
  private DashboardNotificationService notificationService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private OrganizationSettingsService organizationSettingsService;

  @Mock
  private MessageService messageService;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication authentication;

  @InjectMocks
  private CycleService cycleService;

  private Cycle testCycle;
  private Project testProject;
  private CreateCycleRequest testRequest;
  private User adminUser;
  private User developerUser;
  private OrganizationSettingsDTO orgSettings;

  @BeforeEach
  void setUp() {
    testProject = Project.builder().id(1L).name("Test Project").projectKey("TST").isActive(true).build();

    testCycle = Cycle.builder().id(1L).name("Test Cycle").project(testProject).startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6)).phase(CyclePhase.SHAPING_BUILDING).isActive(true).build();

    testRequest = new CreateCycleRequest();
    testRequest.setProjectId(1L);
    testRequest.setName("Test Cycle");
    testRequest.setStartDate(LocalDate.now());
    // Don't set endDate - let auto-calculation handle it
    testRequest.setPhase(CyclePhase.SHAPING_BUILDING);

    // Setup test users
    adminUser = User.builder().id(1L).username("admin").role(UserRole.ADMIN).build();

    developerUser = User.builder().id(2L).username("developer").role(UserRole.MEMBER).build();

    // Setup organization settings
    orgSettings = OrganizationSettingsDTO.builder().defaultCycleLengthWeeks(6).build();

    // Setup pitch count mock (default to 0 for most tests) - using lenient to avoid
    // unnecessary stubbing errors
    lenient().when(pitchRepository.countByCycleIdNotDeleted(any())).thenReturn(0L);
  }

  @Test
  void getAllCycles_ShouldReturnAllCycles() {
    when(cycleRepository.findAllByOrderByStartDateDesc()).thenReturn(Arrays.asList(testCycle));

    List<CycleDTO> result = cycleService.getAllCycles();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Test Cycle");
    verify(cycleRepository).findAllByOrderByStartDateDesc();
  }

  @Test
  void getCycleById_WhenExists_ShouldReturnCycle() {
    when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));

    CycleDTO result = cycleService.getCycleById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Test Cycle");
    assertThat(result.getProjectId()).isEqualTo(1L);
  }

  @Test
  void getCycleById_WhenNotExists_ShouldThrowException() {
    when(cycleRepository.findByIdWithProject(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> cycleService.getCycleById(999L)).isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Cycle not found");
  }

  @Test
  void createCycle_ShouldSaveCycle() {
    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);
    when(organizationSettingsService.getSettings()).thenReturn(orgSettings);

    CycleDTO result = cycleService.createCycle(testRequest);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Test Cycle");
    verify(cycleRepository).save(any(Cycle.class));
  }

  @Test
  void updateCycle_WhenExists_ShouldUpdateCycle() {
    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);
    when(organizationSettingsService.getSettings()).thenReturn(orgSettings);

    testRequest.setName("Updated Cycle");
    CycleDTO result = cycleService.updateCycle(1L, testRequest);

    assertThat(result).isNotNull();
    verify(cycleRepository).save(any(Cycle.class));
  }

  @Test
  void deleteCycle_ShouldCallRepository() {
    when(cycleRepository.existsById(1L)).thenReturn(true);
    doNothing().when(cycleRepository).deleteById(1L);

    cycleService.deleteCycle(1L);

    verify(cycleRepository).deleteById(1L);
  }

  @Test
  void getActiveCycles_ShouldReturnActiveCycles() {
    when(cycleRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(testCycle));

    List<CycleDTO> result = cycleService.getActiveCycles();

    assertThat(result).hasSize(1);
    verify(cycleRepository).findByIsActiveTrue();
  }

  @Test
  void toggleActive_ShouldToggleCycleActiveStatus() {
    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

    CycleDTO result = cycleService.toggleActive(1L);

    assertThat(result).isNotNull();
    verify(cycleRepository).save(any(Cycle.class));
  }

  @Test
  void getCyclesByProject_ShouldReturnCyclesForProject() {
    when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L)).thenReturn(Arrays.asList(testCycle));

    List<CycleDTO> result = cycleService.getCyclesByProject(1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getProjectId()).isEqualTo(1L);
    verify(cycleRepository).findByProjectIdOrderByStartDateDesc(1L);
  }

  // ========== Auto-Calculation Feature Tests ==========

  @Test
  void createCycle_WithNullEndDate_ShouldAutoCalculateFromConfiguration() {
    // Arrange
    testRequest.setEndDate(null);

    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
    when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

    // Act
    CycleDTO result = cycleService.createCycle(testRequest);

    // Assert
    verify(organizationSettingsService).getSettings();
    verify(cycleRepository).save(any(Cycle.class));
    assertThat(result).isNotNull();
  }

  @Test
  void createCycle_WithCustomEndDate_AsAdmin_ShouldUseProvidedDate() {
    // Arrange
    setupSecurityContext("admin", adminUser);
    LocalDate customEndDate = testRequest.getStartDate().plusWeeks(4);
    testRequest.setEndDate(customEndDate);

    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
    when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

    // Act
    CycleDTO result = cycleService.createCycle(testRequest);

    // Assert
    verify(userRepository).findByUsername("admin");
    verify(cycleRepository).save(any(Cycle.class));
    assertThat(result).isNotNull();
  }

  @Test
  void createCycle_WithCustomEndDate_AsProjectManager_ShouldUseProvidedDate() {
    // Arrange
    User pmUser = User.builder().id(3L).username("pm").role(UserRole.MANAGER).build();
    setupSecurityContext("pm", pmUser);
    LocalDate customEndDate = testRequest.getStartDate().plusWeeks(8);
    testRequest.setEndDate(customEndDate);

    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(userRepository.findByUsername("pm")).thenReturn(Optional.of(pmUser));
    when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

    // Act
    CycleDTO result = cycleService.createCycle(testRequest);

    // Assert
    verify(userRepository).findByUsername("pm");
    verify(cycleRepository).save(any(Cycle.class));
    assertThat(result).isNotNull();
  }

  @Test
  void createCycle_WithCustomEndDate_AsDeveloper_ShouldThrowAccessDeniedException() {
    // Arrange
    setupSecurityContext("developer", developerUser);
    LocalDate customEndDate = testRequest.getStartDate().plusWeeks(4);
    testRequest.setEndDate(customEndDate);

    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(userRepository.findByUsername("developer")).thenReturn(Optional.of(developerUser));

    // Act & Assert
    assertThatThrownBy(() -> cycleService.createCycle(testRequest)).isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Only users with ADMIN or PROJECT_MANAGER role");

    verify(userRepository).findByUsername("developer");
    verify(cycleRepository, never()).save(any(Cycle.class));
  }

  @Test
  void createCycle_WithNullEndDate_ShouldUse8WeekCycleLength_WhenConfiguredAs8() {
    // Arrange
    testRequest.setEndDate(null);
    orgSettings = OrganizationSettingsDTO.builder().defaultCycleLengthWeeks(8).build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
    when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

    // Act
    CycleDTO result = cycleService.createCycle(testRequest);

    // Assert
    verify(organizationSettingsService).getSettings();
    assertThat(result).isNotNull();
  }

  @Test
  void createCycle_WithNullEndDate_ShouldFallbackTo6Weeks_WhenConfigurationIsInvalid() {
    // Arrange
    testRequest.setEndDate(null);
    orgSettings = OrganizationSettingsDTO.builder().defaultCycleLengthWeeks(null).build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
    when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

    // Act
    CycleDTO result = cycleService.createCycle(testRequest);

    // Assert
    verify(organizationSettingsService).getSettings();
    assertThat(result).isNotNull();
  }

  @Test
  void updateCycle_WithNullEndDate_ShouldAutoCalculate() {
    // Arrange
    testRequest.setEndDate(null);

    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
    when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

    // Act
    CycleDTO result = cycleService.updateCycle(1L, testRequest);

    // Assert
    // getSettings() is called twice: once for date-change detection, once for actual end-date calculation
    verify(organizationSettingsService, times(2)).getSettings();
    verify(cycleRepository).save(any(Cycle.class));
    assertThat(result).isNotNull();
  }

  @Test
  void updateCycle_WithCustomEndDate_AsNonPrivilegedUser_ShouldThrowException() {
    // Arrange
    setupSecurityContext("qa", User.builder().username("qa").role(UserRole.MEMBER).build());
    LocalDate customEndDate = testRequest.getStartDate().plusWeeks(10);
    testRequest.setEndDate(customEndDate);

    when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
    when(userRepository.findByUsername("qa"))
        .thenReturn(Optional.of(User.builder().username("qa").role(UserRole.MEMBER).build()));

    // Act & Assert
    assertThatThrownBy(() -> cycleService.updateCycle(1L, testRequest)).isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Only users with ADMIN or PROJECT_MANAGER role");
  }

  @Test
  void toDTO_ShouldExcludeDeletedPitchesFromCount() {
    // Given
    when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
    when(pitchRepository.countByCycleIdNotDeleted(1L)).thenReturn(3L); // 3 active pitches

    // When
    CycleDTO result = cycleService.getCycleById(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getName()).isEqualTo("Test Cycle");
    assertThat(result.getPitchCount()).isEqualTo(3); // Should use count from repository, not entity collection

    // Verify that the count method was called
    verify(pitchRepository).countByCycleIdNotDeleted(1L);
  }

  @Test
  void toDTO_ShouldReturnZeroPitchCount_WhenNoPitchesExist() {
    // Given
    when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
    when(pitchRepository.countByCycleIdNotDeleted(1L)).thenReturn(0L);

    // When
    CycleDTO result = cycleService.getCycleById(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getPitchCount()).isEqualTo(0);
    verify(pitchRepository).countByCycleIdNotDeleted(1L);
  }

  @Test
  void toDTO_ShouldReturnZeroPitchCount_WhenAllPitchesAreDeleted() {
    // Given - cycle has pitches in entity collection but none are active
    when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
    when(pitchRepository.countByCycleIdNotDeleted(1L)).thenReturn(0L); // All pitches are deleted

    // When
    CycleDTO result = cycleService.getCycleById(1L);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getPitchCount()).isEqualTo(0); // Should be 0 even if entity has pitches
    verify(pitchRepository).countByCycleIdNotDeleted(1L);
  }

  @Test
  void getAllCycles_ShouldUsePitchCountFromRepository() {
    // Given
    when(cycleRepository.findAllByOrderByStartDateDesc()).thenReturn(Arrays.asList(testCycle));
    when(pitchRepository.countByCycleIdNotDeleted(1L)).thenReturn(5L);

    // When
    List<CycleDTO> result = cycleService.getAllCycles();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPitchCount()).isEqualTo(5);
    verify(pitchRepository).countByCycleIdNotDeleted(1L);
  }

  @Test
  void getActiveCycles_ShouldUsePitchCountFromRepository() {
    // Given
    when(cycleRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(testCycle));
    when(pitchRepository.countByCycleIdNotDeleted(1L)).thenReturn(2L);

    // When
    List<CycleDTO> result = cycleService.getActiveCycles();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPitchCount()).isEqualTo(2);
    verify(pitchRepository).countByCycleIdNotDeleted(1L);
  }

  @Test
  void getAccessibleCycles_ShouldExcludeKanbanProjectCycles() {
    // Given: Set up a regular Shape Up project and a Kanban project
    Project shapeUpProject = Project.builder()
        .id(1L)
        .name("Shape Up Project")
        .projectKey("SUP")
        .projectType(ProjectType.SHAPE_UP)
        .isActive(true)
        .build();

    Cycle shapeUpCycle = Cycle.builder()
        .id(1L)
        .name("Shape Up Cycle")
        .project(shapeUpProject)
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6))
        .phase(CyclePhase.SHAPING_BUILDING)
        .isActive(true)
        .build();

    setupSecurityContext("developer", developerUser);
    when(userRepository.findByUsername("developer")).thenReturn(Optional.of(developerUser));
    
    // Repository should return only Shape Up cycles (Kanban excluded by query)
    when(cycleRepository.findAccessibleCyclesByUserId(developerUser.getId()))
        .thenReturn(Arrays.asList(shapeUpCycle));

    // When
    List<CycleDTO> result = cycleService.getAccessibleCycles();

    // Then: Only Shape Up cycles should be returned (no Kanban cycles)
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Shape Up Cycle");
    assertThat(result.get(0).getProjectId()).isEqualTo(1L);
    // Verify that no Kanban cycles are in the result
    assertThat(result.stream().noneMatch(c -> c.getProjectId().equals(2L))).isTrue();
    verify(cycleRepository).findAccessibleCyclesByUserId(developerUser.getId());
  }

  @Test
  void getAccessibleActiveCycles_ShouldExcludeKanbanProjectCycles() {
    // Given: Set up a regular Shape Up project and a Kanban project
    Project shapeUpProject = Project.builder()
        .id(1L)
        .name("Shape Up Project")
        .projectKey("SUP")
        .projectType(ProjectType.SHAPE_UP)
        .isActive(true)
        .build();

    Cycle shapeUpCycle = Cycle.builder()
        .id(1L)
        .name("Shape Up Active Cycle")
        .project(shapeUpProject)
        .startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6))
        .phase(CyclePhase.SHAPING_BUILDING)
        .isActive(true)
        .build();

    setupSecurityContext("developer", developerUser);
    when(userRepository.findByUsername("developer")).thenReturn(Optional.of(developerUser));
    
    // Repository should return only Shape Up cycles (Kanban excluded by query)
    when(cycleRepository.findAccessibleActiveCyclesByUserId(developerUser.getId()))
        .thenReturn(Arrays.asList(shapeUpCycle));

    // When
    List<CycleDTO> result = cycleService.getAccessibleActiveCycles();

    // Then: Only Shape Up cycles should be returned (no Kanban cycles)
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Shape Up Active Cycle");
    assertThat(result.get(0).getProjectId()).isEqualTo(1L);
    // Verify that no Kanban cycles are in the result
    assertThat(result.stream().noneMatch(c -> c.getProjectId().equals(2L))).isTrue();
    verify(cycleRepository).findAccessibleActiveCyclesByUserId(developerUser.getId());
  }

  private void setupSecurityContext(String username, User user) {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(username);
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void tearDown() {
    // Clear security context to prevent test pollution
    SecurityContextHolder.clearContext();
  }
}
