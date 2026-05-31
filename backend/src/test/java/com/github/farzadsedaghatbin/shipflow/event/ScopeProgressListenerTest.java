package com.github.farzadsedaghatbin.shipflow.event;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.HillChartPoint;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.HillChartPointRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.service.ScopeProgressService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ScopeProgressListener} — verifies a task status change routes to the right
 * scope-sync strategy. Regression coverage for the auto-progress bug where a DONE linked task left
 * its hill-chart scope stuck at its old position.
 */
@ExtendWith(MockitoExtension.class)
class ScopeProgressListenerTest {

  @Mock private ScopeProgressService scopeProgressService;
  @Mock private TaskRepository taskRepository;
  @Mock private HillChartPointRepository hillChartPointRepository;

  @InjectMocks private ScopeProgressListener listener;

  private TaskStatusChangedEvent event(Long taskId, Long pitchId, Long scopeId, Long parentTaskId,
      TaskStatus oldStatus, TaskStatus newStatus) {
    return new TaskStatusChangedEvent(this, taskId, pitchId, scopeId, parentTaskId, oldStatus, newStatus);
  }

  @Test
  void noStatusChange_doesNothing() {
    listener.onTaskStatusChanged(
        event(1L, 10L, null, null, TaskStatus.IN_PROGRESS, TaskStatus.IN_PROGRESS));

    verifyNoInteractions(scopeProgressService, taskRepository, hillChartPointRepository);
  }

  @Test
  void directScope_syncsThatScope() {
    listener.onTaskStatusChanged(
        event(1L, 10L, 99L, null, TaskStatus.IN_PROGRESS, TaskStatus.DONE));

    verify(scopeProgressService).syncProgressIfEnabled(99L);
  }

  @Test
  void rootTaskWithPitch_syncsAutoCreatedScope() {
    // The common auto-progress case: a root scope-task (pitch set, no direct scope, no parent) that
    // moves to DONE must sync its auto-created linked scope.
    HillChartPoint scope = HillChartPoint.builder().id(77L).position(35).autoProgressEnabled(true).build();
    when(hillChartPointRepository.findByLinkedTaskId(1L)).thenReturn(Optional.of(scope));

    listener.onTaskStatusChanged(
        event(1L, 10L, null, null, TaskStatus.IN_PROGRESS, TaskStatus.DONE));

    verify(hillChartPointRepository).findByLinkedTaskId(1L);
    verify(scopeProgressService).syncProgressIfEnabled(77L);
  }

  @Test
  void subtask_syncsRootTaskScope() {
    Task root = Task.builder().id(5L).build();
    Task parent = Task.builder().id(2L).parentTask(root).build();
    when(taskRepository.findById(2L)).thenReturn(Optional.of(parent));
    HillChartPoint scope = HillChartPoint.builder().id(88L).build();
    when(hillChartPointRepository.findByLinkedTaskId(5L)).thenReturn(Optional.of(scope));

    listener.onTaskStatusChanged(
        event(3L, null, null, 2L, TaskStatus.TODO, TaskStatus.DONE));

    verify(scopeProgressService).syncProgressIfEnabled(88L);
  }

  @Test
  void noLinkedScope_doesNotSync() {
    when(hillChartPointRepository.findByLinkedTaskId(1L)).thenReturn(Optional.empty());

    listener.onTaskStatusChanged(
        event(1L, 10L, null, null, TaskStatus.IN_PROGRESS, TaskStatus.DONE));

    verify(scopeProgressService, never()).syncProgressIfEnabled(org.mockito.ArgumentMatchers.anyLong());
  }
}
