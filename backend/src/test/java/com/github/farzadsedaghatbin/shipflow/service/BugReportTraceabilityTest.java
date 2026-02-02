package com.github.farzadsedaghatbin.shipflow.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.qa.BugReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.CreateBugReportRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.UpdateBugReportRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for BugReport traceability relationships - scope, task, and project linking. */
@ExtendWith(MockitoExtension.class)
@DisplayName("BugReport Traceability Tests")
class BugReportTraceabilityTest {

  @Mock private BugReportRepository bugReportRepository;

  @Mock private TestRunRepository testRunRepository;

  @Mock private PitchRepository pitchRepository;

  @Mock private CycleRepository cycleRepository;

  @Mock private TeamRepository teamRepository;

  @Mock private UserRepository userRepository;

  @Mock private HillChartPointRepository hillChartPointRepository;

  @Mock private TaskRepository taskRepository;

  @Mock private ProjectRepository projectRepository;

  @InjectMocks private BugReportService bugReportService;

  private User user;
  private Project project;
  private Project kanbanProject;
  private Cycle cycle;
  private Pitch pitch;
  private HillChartPoint scope;
  private Task task;

  @BeforeEach
  void setUp() {
    // Enable QA Test Management feature for tests
    ReflectionTestUtils.setField(bugReportService, "testManagementEnabled", true);

    user = User.builder().id(1L).username("testuser").build();

    project = Project.builder()
        .id(1L)
        .name("Shape Up Project")
        .projectKey("SUP")
        .projectType(ProjectType.SHAPE_UP)
        .build();

    kanbanProject = Project.builder()
        .id(2L)
        .name("Kanban Project")
        .projectKey("KBN")
        .projectType(ProjectType.KANBAN)
        .build();

    cycle = Cycle.builder().id(1L).name("Cycle 1").project(project).build();

    pitch = Pitch.builder().id(1L).title("User Authentication").cycle(cycle).build();

    scope =
        HillChartPoint.builder()
            .id(1L)
            .scope("Login Form")
            .description("Build login form UI")
            .pitch(pitch)
            .build();

    task =
        Task.builder()
            .id(1L)
            .title("Implement login validation")
            .cycle(cycle)
            .pitch(pitch)
            .scope(scope)
            .build();
  }

  @Test
  @DisplayName("Should create bug report with scope and task links")
  void shouldCreateBugReportWithScopeAndTask() {
    // Arrange
    CreateBugReportRequest request =
        CreateBugReportRequest.builder()
            .title("Login form validation fails for empty email")
            .description("When submitting login form with empty email, no error is shown")
            .severity(BugSeverity.MAJOR)
            .pitchId(1L)
            .scopeId(1L)
            .taskId(1L)
            .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
    when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));
    when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

    BugReport savedBug =
        BugReport.builder()
            .id(1L)
            .bugKey("BUG-001")
            .title(request.getTitle())
            .description(request.getDescription())
            .severity(request.getSeverity())
            .status(BugStatus.OPEN)
            .pitch(pitch)
            .cycle(cycle)
            .scope(scope)
            .task(task)
            .reporter(user)
            .build();

    when(bugReportRepository.save(any(BugReport.class))).thenReturn(savedBug);

    // Act
    BugReportDTO result = bugReportService.createBugReport(request, 1L);

    // Assert
    assertNotNull(result);
    assertEquals("Login form validation fails for empty email", result.getTitle());
    assertEquals(1L, result.getScopeId());
    assertEquals("Login Form", result.getScopeName());
    assertEquals(1L, result.getTaskId());
    assertEquals("Implement login validation", result.getTaskTitle());

    verify(hillChartPointRepository).findById(1L);
    verify(taskRepository).findById(1L);
    verify(bugReportRepository).save(any(BugReport.class));
  }

  @Test
  @DisplayName("Should create bug report without scope/task for general bugs")
  void shouldCreateBugReportWithoutScopeOrTask() {
    // Arrange
    CreateBugReportRequest request =
        CreateBugReportRequest.builder()
            .title("Application crashes on startup in IE11")
            .description("Legacy browser compatibility issue")
            .severity(BugSeverity.CRITICAL)
            .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    BugReport savedBug =
        BugReport.builder()
            .id(2L)
            .bugKey("BUG-002")
            .title(request.getTitle())
            .description(request.getDescription())
            .severity(request.getSeverity())
            .status(BugStatus.OPEN)
            .scope(null) // No specific scope
            .task(null) // No specific task
            .reporter(user)
            .build();

    when(bugReportRepository.save(any(BugReport.class))).thenReturn(savedBug);

    // Act
    BugReportDTO result = bugReportService.createBugReport(request, 1L);

    // Assert
    assertNotNull(result);
    assertEquals("Application crashes on startup in IE11", result.getTitle());
    assertNull(result.getScopeId());
    assertNull(result.getScopeName());
    assertNull(result.getTaskId());
    assertNull(result.getTaskTitle());

    verify(hillChartPointRepository, never()).findById(anyLong());
    verify(taskRepository, never()).findById(anyLong());
  }

  @Test
  @DisplayName("Should update bug report to link with scope and task")
  void shouldUpdateBugReportToLinkWithScopeAndTask() {
    // Arrange
    BugReport existingBug =
        BugReport.builder()
            .id(1L)
            .bugKey("BUG-001")
            .title("Generic bug")
            .description("Some issue")
            .severity(BugSeverity.MINOR)
            .status(BugStatus.OPEN)
            .reporter(user)
            .scope(null)
            .task(null)
            .build();

    UpdateBugReportRequest updateRequest =
        UpdateBugReportRequest.builder().scopeId(1L).taskId(1L).build();

    when(bugReportRepository.findById(1L)).thenReturn(Optional.of(existingBug));
    when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));
    when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
    when(bugReportRepository.save(any(BugReport.class))).thenReturn(existingBug);

    // Act
    BugReportDTO result = bugReportService.updateBugReport(1L, updateRequest, 1L);

    // Assert
    assertNotNull(result);
    assertEquals(1L, result.getScopeId());
    assertEquals(1L, result.getTaskId());

    verify(hillChartPointRepository).findById(1L);
    verify(taskRepository).findById(1L);
  }

  @Test
  @DisplayName("Should link bug to scope but not task")
  void shouldLinkBugToScopeOnly() {
    // Arrange
    CreateBugReportRequest request =
        CreateBugReportRequest.builder()
            .title("UI inconsistency in login form")
            .description("Font size varies across fields")
            .severity(BugSeverity.MINOR)
            .scopeId(1L)
            // No taskId - bug is related to scope but not specific task
            .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));

    BugReport savedBug =
        BugReport.builder()
            .id(3L)
            .bugKey("BUG-003")
            .title(request.getTitle())
            .description(request.getDescription())
            .severity(request.getSeverity())
            .status(BugStatus.OPEN)
            .scope(scope)
            .task(null)
            .reporter(user)
            .build();

    when(bugReportRepository.save(any(BugReport.class))).thenReturn(savedBug);

    // Act
    BugReportDTO result = bugReportService.createBugReport(request, 1L);

    // Assert
    assertNotNull(result);
    assertEquals(1L, result.getScopeId());
    assertEquals("Login Form", result.getScopeName());
    assertNull(result.getTaskId());

    verify(hillChartPointRepository).findById(1L);
    verify(taskRepository, never()).findById(anyLong());
  }

  @Test
  @DisplayName("Should throw exception when scope not found")
  void shouldThrowExceptionWhenScopeNotFound() {
    // Arrange
    CreateBugReportRequest request =
        CreateBugReportRequest.builder()
            .title("Bug with invalid scope")
            .description("Test bug")
            .severity(BugSeverity.MAJOR)
            .scopeId(999L) // Non-existent scope
            .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(hillChartPointRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class, () -> bugReportService.createBugReport(request, 1L));
    verify(bugReportRepository, never()).save(any(BugReport.class));
  }

  @Test
  @DisplayName("Should throw exception when task not found")
  void shouldThrowExceptionWhenTaskNotFound() {
    // Arrange
    CreateBugReportRequest request =
        CreateBugReportRequest.builder()
            .title("Bug with invalid task")
            .description("Test bug")
            .severity(BugSeverity.MAJOR)
            .taskId(999L) // Non-existent task
            .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(taskRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class, () -> bugReportService.createBugReport(request, 1L));
    verify(bugReportRepository, never()).save(any(BugReport.class));
  }

  // ========== Project-Level Bug Report Tests (Kanban Support) ==========

  @Nested
  @DisplayName("Direct Project Association Tests")
  class DirectProjectAssociationTests {

    @Test
    @DisplayName("Should create bug report with direct project ID for Kanban project")
    void shouldCreateBugReportWithDirectProjectId() {
      // Arrange - Kanban project scenario: bug created at project level without cycle/pitch
      CreateBugReportRequest request =
          CreateBugReportRequest.builder()
              .title("Smoke test failure - homepage not loading")
              .description("Found during general smoke testing")
              .severity(BugSeverity.CRITICAL)
              .projectId(2L) // Direct project association
              // No cycleId, pitchId, scopeId, taskId - general bug
              .build();

      when(userRepository.findById(1L)).thenReturn(Optional.of(user));
      when(projectRepository.findById(2L)).thenReturn(Optional.of(kanbanProject));

      BugReport savedBug =
          BugReport.builder()
              .id(10L)
              .bugKey("BUG-010")
              .title(request.getTitle())
              .description(request.getDescription())
              .severity(request.getSeverity())
              .status(BugStatus.OPEN)
              .project(kanbanProject)
              .cycle(null)
              .pitch(null)
              .scope(null)
              .task(null)
              .reporter(user)
              .build();

      when(bugReportRepository.save(any(BugReport.class))).thenReturn(savedBug);

      // Act
      BugReportDTO result = bugReportService.createBugReport(request, 1L);

      // Assert
      assertNotNull(result);
      assertEquals("Smoke test failure - homepage not loading", result.getTitle());
      assertEquals(2L, result.getProjectId());
      assertEquals("Kanban Project", result.getProjectName());
      assertEquals("KBN", result.getProjectKey());
      assertNull(result.getCycleId());
      assertNull(result.getPitchId());
      assertNull(result.getScopeId());
      assertNull(result.getTaskId());

      verify(projectRepository).findById(2L);
      verify(cycleRepository, never()).findById(anyLong());
      verify(pitchRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should auto-derive project from pitch's cycle when not explicit")
    void shouldDeriveProjectFromPitchCycle() {
      // Arrange - Shape Up scenario: project derived from pitch -> cycle -> project
      CreateBugReportRequest request =
          CreateBugReportRequest.builder()
              .title("Bug found in user authentication feature")
              .description("Issue during pitch testing")
              .severity(BugSeverity.MAJOR)
              .pitchId(1L) // Only pitch provided
              // No explicit projectId
              .build();

      when(userRepository.findById(1L)).thenReturn(Optional.of(user));
      when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));

      BugReport savedBug =
          BugReport.builder()
              .id(11L)
              .bugKey("BUG-011")
              .title(request.getTitle())
              .description(request.getDescription())
              .severity(request.getSeverity())
              .status(BugStatus.OPEN)
              .project(project) // Auto-derived from pitch.cycle.project
              .cycle(cycle)
              .pitch(pitch)
              .reporter(user)
              .build();

      when(bugReportRepository.save(any(BugReport.class))).thenReturn(savedBug);

      // Act
      BugReportDTO result = bugReportService.createBugReport(request, 1L);

      // Assert
      assertNotNull(result);
      assertEquals(1L, result.getProjectId());
      assertEquals("Shape Up Project", result.getProjectName());
      assertEquals(1L, result.getCycleId());
      assertEquals(1L, result.getPitchId());

      verify(pitchRepository).findById(1L);
      verify(projectRepository, never()).findById(anyLong()); // Not called - derived from pitch
    }

    @Test
    @DisplayName("Should auto-derive project from cycle when not explicit")
    void shouldDeriveProjectFromCycle() {
      // Arrange - Bug with cycle but no pitch/project explicitly set
      CreateBugReportRequest request =
          CreateBugReportRequest.builder()
              .title("General cycle bug")
              .description("Found during cycle testing")
              .severity(BugSeverity.MINOR)
              .cycleId(1L) // Only cycle provided
              // No explicit projectId or pitchId
              .build();

      when(userRepository.findById(1L)).thenReturn(Optional.of(user));
      when(cycleRepository.findById(1L)).thenReturn(Optional.of(cycle));

      BugReport savedBug =
          BugReport.builder()
              .id(12L)
              .bugKey("BUG-012")
              .title(request.getTitle())
              .description(request.getDescription())
              .severity(request.getSeverity())
              .status(BugStatus.OPEN)
              .project(project) // Auto-derived from cycle.project
              .cycle(cycle)
              .reporter(user)
              .build();

      when(bugReportRepository.save(any(BugReport.class))).thenReturn(savedBug);

      // Act
      BugReportDTO result = bugReportService.createBugReport(request, 1L);

      // Assert
      assertNotNull(result);
      assertEquals(1L, result.getProjectId());
      assertEquals("Shape Up Project", result.getProjectName());
      assertEquals(1L, result.getCycleId());
      assertNull(result.getPitchId());

      verify(cycleRepository).findById(1L);
      verify(projectRepository, never()).findById(anyLong()); // Not called - derived from cycle
    }

    @Test
    @DisplayName("Should update bug report to change project")
    void shouldUpdateBugReportToChangeProject() {
      // Arrange
      BugReport existingBug =
          BugReport.builder()
              .id(1L)
              .bugKey("BUG-001")
              .title("Bug to move")
              .description("Moving between projects")
              .severity(BugSeverity.MINOR)
              .status(BugStatus.OPEN)
              .project(project)
              .reporter(user)
              .build();

      UpdateBugReportRequest updateRequest =
          UpdateBugReportRequest.builder()
              .projectId(2L) // Move to Kanban project
              .build();

      when(bugReportRepository.findById(1L)).thenReturn(Optional.of(existingBug));
      when(projectRepository.findById(2L)).thenReturn(Optional.of(kanbanProject));
      when(bugReportRepository.save(any(BugReport.class))).thenReturn(existingBug);

      // Act
      BugReportDTO result = bugReportService.updateBugReport(1L, updateRequest, 1L);

      // Assert
      assertNotNull(result);
      verify(projectRepository).findById(2L);
      verify(bugReportRepository).save(any(BugReport.class));
    }

    @Test
    @DisplayName("Should throw exception when project not found")
    void shouldThrowExceptionWhenProjectNotFound() {
      // Arrange
      CreateBugReportRequest request =
          CreateBugReportRequest.builder()
              .title("Bug with invalid project")
              .description("Test bug")
              .severity(BugSeverity.MAJOR)
              .projectId(999L) // Non-existent project
              .build();

      when(userRepository.findById(1L)).thenReturn(Optional.of(user));
      when(projectRepository.findById(999L)).thenReturn(Optional.empty());

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class, () -> bugReportService.createBugReport(request, 1L));
      verify(bugReportRepository, never()).save(any(BugReport.class));
    }

    @Test
    @DisplayName("Should include project fields in DTO")
    void shouldIncludeProjectFieldsInDTO() {
      // Arrange
      BugReport bugWithProject =
          BugReport.builder()
              .id(1L)
              .bugKey("BUG-001")
              .title("Test bug")
              .description("Description")
              .severity(BugSeverity.MAJOR)
              .status(BugStatus.OPEN)
              .project(kanbanProject)
              .reporter(user)
              .build();

      // Act
      BugReportDTO dto = bugReportService.toDTO(bugWithProject);

      // Assert
      assertEquals(2L, dto.getProjectId());
      assertEquals("Kanban Project", dto.getProjectName());
      assertEquals("KBN", dto.getProjectKey());
    }
  }
}
