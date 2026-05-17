package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.HillChartPointRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskAttachmentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for {@link TaskService#exportTasksCsv}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskService.exportTasksCsv Tests")
class TaskCsvExportServiceTest {

  @Mock TaskRepository taskRepository;
  @Mock CycleRepository cycleRepository;
  @Mock PersonRepository personRepository;
  @Mock UserRepository userRepository;
  @Mock PitchRepository pitchRepository;
  @Mock HillChartPointRepository hillChartPointRepository;
  @Mock TaskAttachmentRepository attachmentRepository;
  @Mock DashboardNotificationService notificationService;
  @Mock MessageService messageService;
  @Mock ApplicationEventPublisher eventPublisher;
  @Mock PermissionService permissionService;
  @Mock ProjectService projectService;

  @InjectMocks TaskService taskService;

  private Project project;
  private Cycle cycle;

  @BeforeEach
  void setUp() {
    project = new Project();
    project.setId(1L);

    cycle = new Cycle();
    cycle.setId(10L);
    cycle.setName("Cycle 1");
    cycle.setProject(project);
  }

  private Task buildTask(long id, String title) {
    Task t = new Task();
    t.setId(id);
    t.setTitle(title);
    t.setStatus(TaskStatus.IN_PROGRESS);
    t.setPriority(TaskPriority.HIGH);
    t.setCycle(cycle);
    t.setCreatedAt(LocalDateTime.of(2026, 3, 1, 9, 0));
    t.setUpdatedAt(LocalDateTime.of(2026, 3, 15, 12, 0));
    return t;
  }

  @Test
  @DisplayName("CSV header row is always present")
  void exportTasksCsv_ShouldAlwaysIncludeHeader() {
    when(taskRepository.findByProjectIdWithFilters(any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    byte[] csv = taskService.exportTasksCsv(1L, null, null, null, null, null);
    String content = new String(csv, StandardCharsets.UTF_8);

    assertThat(content).startsWith("ID,Title,Status,Priority,Assignee,Pitch,Cycle,Estimate(h),Actual(h),Tags,Created,Updated");
  }

  @Test
  @DisplayName("Each task is emitted as a CSV row with correct field ordering")
  void exportTasksCsv_ShouldEmitOneRowPerTask() {
    Task task = buildTask(42L, "Fix login bug");
    when(taskRepository.findByProjectIdWithFilters(any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(task)));

    byte[] csv = taskService.exportTasksCsv(1L, null, null, null, null, null);
    String content = new String(csv, StandardCharsets.UTF_8);

    String[] lines = content.split("\n");
    // 2 lines: header + 1 task
    assertThat(lines).hasSize(2);
    assertThat(lines[1]).startsWith("42,Fix login bug,IN_PROGRESS,HIGH,");
  }

  @Test
  @DisplayName("Title containing commas and quotes is properly escaped")
  void exportTasksCsv_ShouldEscapeTitleWithCommasAndQuotes() {
    Task task = buildTask(1L, "Fix \"auth\", login");
    when(taskRepository.findByProjectIdWithFilters(any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(task)));

    byte[] csv = taskService.exportTasksCsv(1L, null, null, null, null, null);
    String content = new String(csv, StandardCharsets.UTF_8);

    assertThat(content).contains("\"Fix \"\"auth\"\", login\"");
  }

  @Test
  @DisplayName("Assignee and pitch names are included in the row")
  void exportTasksCsv_ShouldIncludeAssigneeAndPitchNames() {
    Task task = buildTask(1L, "My task");

    Person assignee = new Person();
    assignee.setId(5L);
    assignee.setName("Alice Smith");
    task.setAssignee(assignee);

    Pitch pitch = new Pitch();
    pitch.setId(7L);
    pitch.setTitle("Search Redesign");
    task.setPitch(pitch);

    when(taskRepository.findByProjectIdWithFilters(any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(task)));

    byte[] csv = taskService.exportTasksCsv(1L, null, null, null, null, null);
    String content = new String(csv, StandardCharsets.UTF_8);

    assertThat(content).contains("Alice Smith");
    assertThat(content).contains("Search Redesign");
  }

  @Test
  @DisplayName("Tags string is preserved as-is in the CSV row")
  void exportTasksCsv_ShouldIncludeTagsValue() {
    Task task = buildTask(1L, "Tagged task");
    task.setTags("bug,ui,critical");

    when(taskRepository.findByProjectIdWithFilters(any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(task)));

    byte[] csv = taskService.exportTasksCsv(1L, null, null, null, null, null);
    String content = new String(csv, StandardCharsets.UTF_8);

    // tags field is comma-separated; the csvEscape wraps it in quotes
    assertThat(content).contains("bug,ui,critical");
  }

  @Test
  @DisplayName("When cycleId is provided, the cycle-scoped repository method is used")
  void exportTasksCsv_WhenCycleIdProvided_ShouldUseCycleScopedQuery() {
    Task task = buildTask(1L, "Cycle task");
    when(taskRepository.findByCycleIdWithFilters(eq(10L), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(task)));

    byte[] csv = taskService.exportTasksCsv(1L, 10L, null, null, null, null);
    String content = new String(csv, StandardCharsets.UTF_8);

    assertThat(content).contains("Cycle task");
    verify(taskRepository, never()).findByProjectIdWithFilters(any(), any(), any(), any(), any(), any(Pageable.class));
    verify(taskRepository).findByCycleIdWithFilters(eq(10L), any(), any(), any(), any(), any(Pageable.class));
  }

  @Test
  @DisplayName("Empty project returns only the header row")
  void exportTasksCsv_WhenNoTasks_ShouldReturnHeaderOnly() {
    when(taskRepository.findByProjectIdWithFilters(any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    byte[] csv = taskService.exportTasksCsv(1L, null, null, null, null, null);
    String content = new String(csv, StandardCharsets.UTF_8).trim();

    // Only the header, no trailing empty line when split
    assertThat(content.split("\n")).hasSize(1);
  }

  @Test
  @DisplayName("Null optional task fields produce empty CSV cells (no NPE)")
  void exportTasksCsv_WhenOptionalFieldsAreNull_ShouldNotThrow() {
    Task task = buildTask(1L, "Minimal task");
    // assignee, pitch, estimate, tags are all null / empty by default

    when(taskRepository.findByProjectIdWithFilters(any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(task)));

    assertThatNoException().isThrownBy(
        () -> taskService.exportTasksCsv(1L, null, null, null, null, null));
  }

  @Test
  @DisplayName("Soft-deleted tasks are excluded from cycle-scoped exports")
  void exportTasksCsv_WhenCyclePath_ShouldExcludeSoftDeletedTasks() {
    Task active = buildTask(1L, "Active task");
    Task deleted = buildTask(2L, "Deleted task");
    deleted.setDeletedAt(LocalDateTime.of(2026, 3, 10, 8, 0));

    when(taskRepository.findByCycleIdWithFilters(eq(10L), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(active, deleted)));

    byte[] csv = taskService.exportTasksCsv(null, 10L, null, null, null, null);
    String content = new String(csv, StandardCharsets.UTF_8);

    assertThat(content).contains("Active task");
    assertThat(content).doesNotContain("Deleted task");
  }

  @Test
  @DisplayName("Title starting with '=' is prefixed to prevent formula injection")
  void exportTasksCsv_WhenTitleStartsWithEquals_ShouldPrefixApostrophe() {
    Task task = buildTask(1L, "=SUM(A1:A10)");

    when(taskRepository.findByProjectIdWithFilters(any(), any(), any(), any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(task)));

    byte[] csv = taskService.exportTasksCsv(1L, null, null, null, null, null);
    String content = new String(csv, StandardCharsets.UTF_8);

    // The escaped value should be prefixed with ' and then wrapped in quotes
    assertThat(content).doesNotContain(",=SUM(");
    assertThat(content).contains("'=SUM(");
  }
}
