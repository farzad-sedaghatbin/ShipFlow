package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.HillChartPoint;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.HillChartPointRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ScopeProgressService}. Regression coverage for the bug where a scope's
 * hill-chart position only reflected the linked task's direct subtasks (the legacy Scope-Task
 * Bridge), silently ignoring tasks linked to the scope via {@code Task.scope} — e.g. a task
 * edited to point at an existing scope, or completed after the fact.
 */
@ExtendWith(MockitoExtension.class)
class ScopeProgressServiceTest {

  @Mock private HillChartPointRepository hillChartPointRepository;
  @Mock private TaskRepository taskRepository;

  @InjectMocks private ScopeProgressService service;

  private Task taskWithStatus(Long id, TaskStatus status) {
    return Task.builder().id(id).status(status).build();
  }

  @Test
  void calculateSuggestedPosition_scopeNotFound_returnsNull() {
    when(hillChartPointRepository.findById(1L)).thenReturn(Optional.empty());

    assertThat(service.calculateSuggestedPosition(1L)).isNull();
  }

  @Test
  void calculateSuggestedPosition_noLinkedTaskAndNoScopedTasks_returnsNull() {
    HillChartPoint scope = HillChartPoint.builder().id(1L).build();
    when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));
    when(taskRepository.findByScopeIdNotDeleted(1L)).thenReturn(List.of());

    assertThat(service.calculateSuggestedPosition(1L)).isNull();
  }

  @Test
  void calculateSuggestedPosition_onlySubtasksOfLinkedTask_averagesThem() {
    Task linkedTask = taskWithStatus(10L, TaskStatus.IN_PROGRESS);
    HillChartPoint scope = HillChartPoint.builder().id(1L).linkedTask(linkedTask).build();
    when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));
    when(taskRepository.findByParentTaskIdNotDeleted(10L))
        .thenReturn(List.of(taskWithStatus(11L, TaskStatus.DONE), taskWithStatus(12L, TaskStatus.TODO)));
    when(taskRepository.findByScopeIdNotDeleted(1L)).thenReturn(List.of());

    // average(DONE=100, TODO=10) = 55
    assertThat(service.calculateSuggestedPosition(1L)).isEqualTo(55);
  }

  @Test
  void calculateSuggestedPosition_taskLinkedDirectlyViaScope_isIncludedEvenWithoutBeingASubtask() {
    // Regression test: a task the user links to an existing scope (not a subtask of the scope's
    // linkedTask) must move the hill chart when its status changes.
    Task linkedTask = taskWithStatus(10L, TaskStatus.TODO);
    HillChartPoint scope = HillChartPoint.builder().id(1L).linkedTask(linkedTask).build();
    when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));
    when(taskRepository.findByParentTaskIdNotDeleted(10L)).thenReturn(List.of());
    when(taskRepository.findByScopeIdNotDeleted(1L))
        .thenReturn(List.of(taskWithStatus(20L, TaskStatus.DONE)));

    assertThat(service.calculateSuggestedPosition(1L)).isEqualTo(100);
  }

  @Test
  void calculateSuggestedPosition_unionOfSubtasksAndScopeLinkedTasks_dedupesByTaskId() {
    Task linkedTask = taskWithStatus(10L, TaskStatus.TODO);
    HillChartPoint scope = HillChartPoint.builder().id(1L).linkedTask(linkedTask).build();
    Task subtask = taskWithStatus(11L, TaskStatus.DONE);
    when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));
    when(taskRepository.findByParentTaskIdNotDeleted(10L)).thenReturn(List.of(subtask));
    // Same task (id 11) is also directly scope-linked — must count once, not twice.
    when(taskRepository.findByScopeIdNotDeleted(1L))
        .thenReturn(List.of(subtask, taskWithStatus(21L, TaskStatus.TODO)));

    // average(DONE=100, TODO=10) = 55, not the id-11-counted-twice average of 70
    assertThat(service.calculateSuggestedPosition(1L)).isEqualTo(55);
  }

  @Test
  void calculateSuggestedPosition_noSubtasksButLinkedTaskItself_fallsBackToLinkedTaskStatus() {
    Task linkedTask = taskWithStatus(10L, TaskStatus.IN_REVIEW);
    HillChartPoint scope = HillChartPoint.builder().id(1L).linkedTask(linkedTask).build();
    when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(scope));
    when(taskRepository.findByParentTaskIdNotDeleted(10L)).thenReturn(List.of());
    when(taskRepository.findByScopeIdNotDeleted(1L)).thenReturn(List.of());

    assertThat(service.calculateSuggestedPosition(1L)).isEqualTo(75);
  }

  @Test
  void calculatePositionFromTask_noSubtasks_usesTaskOwnStatus() {
    Task task = taskWithStatus(1L, TaskStatus.BLOCKED);
    when(taskRepository.findByParentTaskIdNotDeleted(1L)).thenReturn(List.of());

    assertThat(service.calculatePositionFromTask(task)).isEqualTo(40);
  }

  @Test
  void calculatePositionFromTask_withSubtasks_averagesSubtaskStatuses() {
    Task task = taskWithStatus(1L, TaskStatus.TODO);
    when(taskRepository.findByParentTaskIdNotDeleted(1L))
        .thenReturn(List.of(taskWithStatus(2L, TaskStatus.DONE), taskWithStatus(3L, TaskStatus.DONE)));

    assertThat(service.calculatePositionFromTask(task)).isEqualTo(100);
  }
}
