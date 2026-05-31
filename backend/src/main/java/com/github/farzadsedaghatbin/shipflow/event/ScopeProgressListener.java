package com.github.farzadsedaghatbin.shipflow.event;

import com.github.farzadsedaghatbin.shipflow.entity.HillChartPoint;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.repository.HillChartPointRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.service.ScopeProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for task status changes and synchronizes scope progress on the hill chart.
 * This enables auto-progress: when subtasks are completed, the parent scope
 * automatically moves on the hill chart.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScopeProgressListener {

  private final ScopeProgressService scopeProgressService;
  private final TaskRepository taskRepository;
  private final HillChartPointRepository hillChartPointRepository;

  /**
   * Handle task status changes and sync scope progress.
   *
   * <p>Runs <em>after</em> the publishing transaction commits ({@link TransactionPhase#AFTER_COMMIT})
   * so the sync reads the task's committed status — not the pre-commit value. The previous
   * {@code @Async @EventListener} combination fired on a separate thread before the status change
   * was committed, so the scope position was recomputed from the stale (e.g. IN_PROGRESS) status and
   * the move to 100% was lost. Because this runs after the publishing transaction has already
   * committed, there is no transaction to join — the listener must open its own. Hence
   * {@code Propagation.REQUIRES_NEW}, which Spring also requires for an {@code AFTER_COMMIT}
   * {@code @TransactionalEventListener} (a plain {@code @Transactional} here fails at startup).
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onTaskStatusChanged(TaskStatusChangedEvent event) {
    if (!event.affectsProgress()) {
      return;
    }

    log.debug("Task {} status changed from {} to {}, checking scope progress sync",
        event.getTaskId(), event.getOldStatus(), event.getNewStatus());

    // Strategy 1: If task has a direct scope, sync that scope
    if (event.getScopeId() != null) {
      syncScopeProgress(event.getScopeId());
      return;
    }

    // Strategy 2: If task has a parent task, find the root task's scope
    if (event.getParentTaskId() != null) {
      syncParentScopeProgress(event.getParentTaskId());
      return;
    }

    // Strategy 3: If this is a root task with a pitch, find its auto-created scope
    if (event.getPitchId() != null) {
      syncTaskAutoCreatedScope(event.getTaskId());
    }
  }

  /**
   * Sync progress for a specific scope.
   */
  private void syncScopeProgress(Long scopeId) {
    try {
      boolean updated = scopeProgressService.syncProgressIfEnabled(scopeId);
      if (updated) {
        log.info("Scope {} progress auto-synced from subtask completion", scopeId);
      }
    } catch (Exception e) {
      log.error("Failed to sync scope {} progress", scopeId, e);
    }
  }

  /**
   * Find and sync the scope linked to the root ancestor of a parent task.
   */
  private void syncParentScopeProgress(Long parentTaskId) {
    try {
      Task parentTask = taskRepository.findById(parentTaskId).orElse(null);
      if (parentTask == null) {
        return;
      }

      // Traverse up to find the root task
      Task rootTask = findRootTask(parentTask);

      // Check if root task has an auto-created scope
      HillChartPoint scope = hillChartPointRepository.findByLinkedTaskId(rootTask.getId()).orElse(null);
      if (scope != null) {
        syncScopeProgress(scope.getId());
      }
    } catch (Exception e) {
      log.error("Failed to sync parent scope progress for parent task {}", parentTaskId, e);
    }
  }

  /**
   * Sync the auto-created scope for a root task.
   */
  private void syncTaskAutoCreatedScope(Long taskId) {
    try {
      HillChartPoint scope = hillChartPointRepository.findByLinkedTaskId(taskId).orElse(null);
      if (scope != null) {
        syncScopeProgress(scope.getId());
      }
    } catch (Exception e) {
      log.error("Failed to sync auto-created scope for task {}", taskId, e);
    }
  }

  /**
   * Find the root task (task with no parent) by traversing up the hierarchy.
   */
  private Task findRootTask(Task task) {
    Task current = task;
    int maxDepth = 100; // Prevent infinite loops
    int depth = 0;

    while (current.getParentTask() != null && depth < maxDepth) {
      current = current.getParentTask();
      depth++;
    }

    return current;
  }
}
