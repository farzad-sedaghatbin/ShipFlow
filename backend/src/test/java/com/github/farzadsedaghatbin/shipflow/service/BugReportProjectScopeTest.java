package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.qa.BugReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.CreateBugReportRequest;
import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TestRun;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestRunStatus;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TestRunRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Regression tests for {@link BugReportService#createBugReport}'s project-scope resolution and
 * the new "a bug must resolve to a project" guard. 29 orphan (project-less) bugs existed in
 * production and were invisible in every project-scoped bug list/board, since {@code
 * BugReportSpecification} matches on {@code project_id} OR {@code cycle.project_id}. This class
 * covers every derivation path (explicit projectId, pitch's cycle, cycle, task's own project,
 * task's cycle, test run's cycle) plus the rejection case when none resolve.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BugReport Project Scope Tests")
class BugReportProjectScopeTest {

  @Mock
  private BugReportRepository bugReportRepository;

  @Mock
  private TestRunRepository testRunRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private MessageService messageService;

  @Mock
  private CycleRepository cycleRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private ProjectRepository projectRepository;

  @InjectMocks
  private BugReportService bugReportService;

  private User user;
  private Project project;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(bugReportService, "testManagementEnabled", true);

    user = User.builder().id(1L).username("qa.tester").build();
    project = Project.builder().id(1L).name("Shape Up Project").projectKey("SUP").build();

    lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    lenient().when(messageService.getMessage(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn("A project is required.");
    lenient().when(bugReportRepository.save(any(BugReport.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  @DisplayName("Explicit projectId keeps that project")
  void createBugReport_WithExplicitProjectId_KeepsThatProject() {
    when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

    CreateBugReportRequest request = CreateBugReportRequest.builder().title("Explicit project bug")
        .description("...").severity(BugSeverity.MAJOR).projectId(1L).build();

    BugReportDTO result = bugReportService.createBugReport(request, 1L);

    assertThat(result.getProjectId()).isEqualTo(1L);
    assertThat(result.getProjectName()).isEqualTo("Shape Up Project");
  }

  @Test
  @DisplayName("Project derived from the pitch's cycle when only pitchId is given")
  void createBugReport_WithPitchOnly_DerivesProjectFromPitchCycle() {
    Cycle cycle = Cycle.builder().id(10L).name("Cycle 1").project(project).build();
    Pitch pitch = Pitch.builder().id(20L).title("A pitch").cycle(cycle).build();
    when(pitchRepository.findById(20L)).thenReturn(Optional.of(pitch));

    CreateBugReportRequest request = CreateBugReportRequest.builder().title("Pitch-derived bug")
        .description("...").severity(BugSeverity.MAJOR).pitchId(20L).build();

    BugReportDTO result = bugReportService.createBugReport(request, 1L);

    assertThat(result.getProjectId()).isEqualTo(1L);
    assertThat(result.getPitchId()).isEqualTo(20L);
  }

  @Test
  @DisplayName("Project derived from the cycle when only cycleId is given")
  void createBugReport_WithCycleOnly_DerivesProjectFromCycle() {
    Cycle cycle = Cycle.builder().id(11L).name("Cycle 2").project(project).build();
    when(cycleRepository.findById(11L)).thenReturn(Optional.of(cycle));

    CreateBugReportRequest request = CreateBugReportRequest.builder().title("Cycle-derived bug")
        .description("...").severity(BugSeverity.MAJOR).cycleId(11L).build();

    BugReportDTO result = bugReportService.createBugReport(request, 1L);

    assertThat(result.getProjectId()).isEqualTo(1L);
    assertThat(result.getCycleId()).isEqualTo(11L);
  }

  @Test
  @DisplayName("Project derived from the task's own project when only taskId is given")
  void createBugReport_WithTaskOnly_DerivesProjectFromTasksOwnProject() {
    Task task = Task.builder().id(30L).title("Backlog task").project(project).build();
    when(taskRepository.findById(30L)).thenReturn(Optional.of(task));

    CreateBugReportRequest request = CreateBugReportRequest.builder().title("Task-project-derived bug")
        .description("...").severity(BugSeverity.MAJOR).taskId(30L).build();

    BugReportDTO result = bugReportService.createBugReport(request, 1L);

    assertThat(result.getProjectId()).isEqualTo(1L);
    assertThat(result.getTaskId()).isEqualTo(30L);
  }

  @Test
  @DisplayName("Project derived from the task's cycle when the task has no direct project")
  void createBugReport_WithTaskOnly_DerivesProjectFromTasksCycle_WhenNoDirectProject() {
    Cycle cycle = Cycle.builder().id(12L).name("Cycle 3").project(project).build();
    Task task = Task.builder().id(31L).title("Pitch-scope task").cycle(cycle).build();
    when(taskRepository.findById(31L)).thenReturn(Optional.of(task));

    CreateBugReportRequest request = CreateBugReportRequest.builder().title("Task-cycle-derived bug")
        .description("...").severity(BugSeverity.MAJOR).taskId(31L).build();

    BugReportDTO result = bugReportService.createBugReport(request, 1L);

    assertThat(result.getProjectId()).isEqualTo(1L);
    assertThat(result.getTaskId()).isEqualTo(31L);
  }

  @Test
  @DisplayName("Project derived from the test run's cycle when only testRunId is given")
  void createBugReport_WithTestRunOnly_DerivesProjectFromTestRunsCycle() {
    Cycle cycle = Cycle.builder().id(13L).name("Cycle 4").project(project).build();
    TestRun testRun = TestRun.builder().id(40L).status(TestRunStatus.FAILED).cycle(cycle).build();
    when(testRunRepository.findById(40L)).thenReturn(Optional.of(testRun));

    CreateBugReportRequest request = CreateBugReportRequest.builder().title("Test-run-derived bug")
        .description("...").severity(BugSeverity.MAJOR).testRunId(40L).build();

    BugReportDTO result = bugReportService.createBugReport(request, 1L);

    assertThat(result.getProjectId()).isEqualTo(1L);
    assertThat(result.getTestRunId()).isEqualTo(40L);
  }

  @Test
  @DisplayName("No project-resolving scope at all throws BadRequestException")
  void createBugReport_WithNoProjectResolvingScope_ThrowsBadRequestException() {
    // This is the regression being fixed: 29 such orphan bugs existed in production and were
    // invisible in every project-scoped list, because every bug list/board view is project-scoped.
    CreateBugReportRequest request = CreateBugReportRequest.builder().title("Orphan bug")
        .description("...").severity(BugSeverity.MAJOR).build();

    assertThatThrownBy(() -> bugReportService.createBugReport(request, 1L))
        .isInstanceOf(BadRequestException.class);
  }
}
