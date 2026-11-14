package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.TaskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.HillChartPoint;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.repository.HillChartPointRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Jira-style "convert to subtask" / "add existing task as subtask" rules in {@link TaskService}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskService — convert to / from subtask")
class TaskConvertSubtaskTest {

  @Mock private TaskRepository taskRepository;
  @Mock private HillChartPointRepository hillChartPointRepository;
  @Mock private ScopeProgressService scopeProgressService;
  @Mock private MessageService messageService;
  @Mock private ProjectService projectService;

  @InjectMocks private TaskService taskService;

  private Project project;
  private Cycle cycleA;
  private Cycle cycleB;
  private Task parent;
  private Task task;

  @BeforeEach
  void setUp() {
    lenient().when(messageService.getMessage(anyString())).thenAnswer(i -> i.getArgument(0));
    project = Project.builder().id(1L).name("P").projectKey("P").build();
    cycleA = Cycle.builder().id(10L).name("A").project(project).build();
    cycleB = Cycle.builder().id(11L).name("B").project(project).build();
    parent = task(1L, "Parent", cycleA, TaskStatus.IN_PROGRESS);
    task = task(2L, "Loose task", cycleB, TaskStatus.BACKLOG);

    lenient().when(taskRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(parent));
    lenient().when(taskRepository.findByIdNotDeleted(2L)).thenReturn(Optional.of(task));
    lenient().when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));
    lenient().when(hillChartPointRepository.findByLinkedTaskId(any())).thenReturn(Optional.empty());
  }

  private Task task(Long id, String title, Cycle cycle, TaskStatus status) {
    return Task.builder().id(id).title(title).cycle(cycle).project(project).status(status)
        .priority(TaskPriority.MEDIUM).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
  }

  @Test
  @DisplayName("converts: adopts the parent's cycle and leaves Backlog")
  void convertsAndInheritsParentPlacement() {
    TaskDTO dto = taskService.setTaskParent(2L, 1L);

    assertThat(task.getParentTask()).isSameAs(parent);
    assertThat(task.getCycle()).isSameAs(cycleA);
    assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
    assertThat(dto.getParentTaskId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("converts: keeps a non-backlog status untouched")
  void keepsActiveStatus() {
    task.setStatus(TaskStatus.IN_REVIEW);
    taskService.setTaskParent(2L, 1L);
    assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_REVIEW);
  }

  @Test
  @DisplayName("converts: takes the parent's pitch when it has one")
  void inheritsPitch() {
    Pitch pitch = new Pitch();
    parent.setPitch(pitch);
    taskService.setTaskParent(2L, 1L);
    assertThat(task.getPitch()).isSameAs(pitch);
  }

  @Test
  @DisplayName("refuses a task that already has subtasks")
  void refusesTaskWithSubtasks() {
    when(taskRepository.existsByParentTaskIdAndDeletedAtIsNull(2L)).thenReturn(true);

    assertThatThrownBy(() -> taskService.setTaskParent(2L, 1L))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("error.task.convert.has.subtasks");
    verify(taskRepository, never()).save(any());
    assertThat(task.getParentTask()).isNull();
  }

  @Test
  @DisplayName("refuses a parent that is itself a subtask (two levels only)")
  void refusesSubtaskAsParent() {
    Task grandparent = task(3L, "Top", cycleA, TaskStatus.TODO);
    parent.setParentTask(grandparent);

    assertThatThrownBy(() -> taskService.setTaskParent(2L, 1L))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("error.task.parent.is.subtask");
  }

  @Test
  @DisplayName("refuses itself as parent")
  void refusesSelf() {
    assertThatThrownBy(() -> taskService.setTaskParent(2L, 2L))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("error.task.circular.reference");
  }

  @Test
  @DisplayName("refuses a parent from another project")
  void refusesOtherProject() {
    Project other = Project.builder().id(99L).name("O").projectKey("O").build();
    parent.setCycle(Cycle.builder().id(50L).project(other).build());
    parent.setProject(other);

    assertThatThrownBy(() -> taskService.setTaskParent(2L, 1L))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("error.task.parent.different.project");
  }

  @Test
  @DisplayName("refuses a task that owns a hill chart scope")
  void refusesScopeOwner() {
    when(hillChartPointRepository.findByLinkedTaskId(2L)).thenReturn(Optional.of(new HillChartPoint()));

    assertThatThrownBy(() -> taskService.setTaskParent(2L, 1L))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("error.task.convert.owns.scope");
  }

  @Test
  @DisplayName("moving a subtask between parents is allowed and resyncs both scopes")
  void movesBetweenParents() {
    Task other = task(3L, "Other parent", cycleB, TaskStatus.TODO);
    when(taskRepository.findByIdNotDeleted(3L)).thenReturn(Optional.of(other));
    task.setParentTask(parent);
    HillChartPoint oldScope = new HillChartPoint();
    oldScope.setId(77L);
    when(hillChartPointRepository.findByLinkedTaskId(1L)).thenReturn(Optional.of(oldScope));

    taskService.setTaskParent(2L, 3L);

    assertThat(task.getParentTask()).isSameAs(other);
    assertThat(task.getCycle()).isSameAs(cycleB);
    verify(scopeProgressService).syncProgressIfEnabled(77L);
  }

  @Test
  @DisplayName("null parent turns a subtask back into a top-level task")
  void detaches() {
    task.setParentTask(parent);

    TaskDTO dto = taskService.setTaskParent(2L, null);

    assertThat(task.getParentTask()).isNull();
    assertThat(dto.getParentTaskId()).isNull();
  }

  @Test
  @DisplayName("null parent on a top-level task is a no-op")
  void detachNoop() {
    taskService.setTaskParent(2L, null);
    verify(taskRepository, never()).save(any());
  }

  @Test
  @DisplayName("a subtask has no subtask candidates")
  void subtaskHasNoChildCandidates() {
    task.setParentTask(parent);
    assertThat(taskService.findSubtaskCandidates(2L, "x")).isEmpty();
  }
}
