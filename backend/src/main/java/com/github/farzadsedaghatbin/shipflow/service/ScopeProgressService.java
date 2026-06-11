package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.HillChartPoint;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.repository.HillChartPointRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for calculating and synchronizing hill chart scope progress
 * based on linked task and subtask completion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScopeProgressService {

  private final HillChartPointRepository hillChartPointRepository;
  private final TaskRepository taskRepository;

  /**
   * Calculate the suggested hill chart position (0-100) based on subtask completion.
   * 
   * Position mapping:
   * - 0-50: "Figuring Things Out" (uphill) - maps to 0-50% completion
   * - 50-100: "Making It Happen" (downhill) - maps to 50-100% completion
   * 
   * @param scopeId the hill chart point (scope) ID
   * @return calculated position 0-100, or null if no linked task
   */
  public Integer calculateSuggestedPosition(Long scopeId) {
    HillChartPoint scope = hillChartPointRepository.findById(scopeId).orElse(null);
    if (scope == null || scope.getLinkedTask() == null) {
      return null;
    }

    Task linkedTask = scope.getLinkedTask();
    return calculatePositionFromTask(linkedTask);
  }

  /**
   * Calculate hill chart position from a task and its subtasks.
   * 
   * @param task the root task
   * @return position 0-100
   */
  public Integer calculatePositionFromTask(Task task) {
    // Get all subtasks (children) of this task
    List<Task> subtasks = taskRepository.findByParentTaskIdNotDeleted(task.getId());

    if (subtasks.isEmpty()) {
      // No subtasks: base position on task's own status
      return getPositionFromTaskStatus(task.getStatus());
    }

    // Weighted average: each subtask contributes its own status-based position.
    // Using binary DONE/CANCELLED counting caused IN_REVIEW subtasks (position=75)
    // to contribute zero, making a scope with all-IN_REVIEW subtasks show 0%.
    double averagePosition = subtasks.stream()
        .mapToInt(t -> getPositionFromTaskStatus(t.getStatus()))
        .average()
        .orElse(0);

    return (int) Math.round(averagePosition);
  }

  /**
   * Map task status to a default hill chart position.
   */
  private int getPositionFromTaskStatus(TaskStatus status) {
    return switch (status) {
      case BACKLOG -> 0;
      case TODO -> 10;
      case IN_PROGRESS -> 35;
      case BLOCKED -> 40; // Stuck in figuring out phase
      case IN_REVIEW -> 75;
      case DONE -> 100;
      case CANCELLED -> 100;
    };
  }

  /**
   * Sync scope position from subtask completion if auto-progress is enabled.
   * 
   * @param scopeId the scope ID to sync
   * @return true if position was updated, false otherwise
   */
  public boolean syncProgressIfEnabled(Long scopeId) {
    HillChartPoint scope = hillChartPointRepository.findById(scopeId).orElse(null);
    if (scope == null) {
      return false;
    }

    // Skip if auto-progress is disabled (user manually controls position)
    if (!Boolean.TRUE.equals(scope.getAutoProgressEnabled())) {
      log.debug("Auto-progress disabled for scope {}, skipping sync", scopeId);
      return false;
    }

    Integer suggestedPosition = calculateSuggestedPosition(scopeId);
    if (suggestedPosition == null) {
      return false;
    }

    // Only update if position actually changed
    if (!suggestedPosition.equals(scope.getPosition())) {
      log.info("Auto-updating scope {} position from {} to {} based on subtask completion",
          scopeId, scope.getPosition(), suggestedPosition);
      scope.setPosition(suggestedPosition);
      hillChartPointRepository.save(scope);
      return true;
    }

    return false;
  }

  /**
   * Sync progress for all scopes linked to tasks under a specific pitch.
   * 
   * @param pitchId the pitch ID
   * @return number of scopes updated
   */
  public int syncAllScopesForPitch(Long pitchId) {
    List<HillChartPoint> scopes = hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(pitchId);
    int updated = 0;

    for (HillChartPoint scope : scopes) {
      if (syncProgressIfEnabled(scope.getId())) {
        updated++;
      }
    }

    return updated;
  }

  /**
   * Get suggested position for a scope without applying it.
   * Useful for showing difference between current and suggested position in UI.
   * 
   * @param scopeId the scope ID
   * @return suggested position or current position if no linked task
   */
  public Integer getSuggestedPositionForUI(Long scopeId) {
    Integer suggested = calculateSuggestedPosition(scopeId);
    if (suggested != null) {
      return suggested;
    }

    // Fall back to current position
    return hillChartPointRepository.findById(scopeId)
        .map(HillChartPoint::getPosition)
        .orElse(0);
  }

  /**
   * Disable auto-progress for a scope (called when user manually drags position).
   * 
   * @param scopeId the scope ID
   */
  public void disableAutoProgress(Long scopeId) {
    hillChartPointRepository.findById(scopeId).ifPresent(scope -> {
      if (Boolean.TRUE.equals(scope.getAutoProgressEnabled())) {
        log.info("Disabling auto-progress for scope {} due to manual position update", scopeId);
        scope.setAutoProgressEnabled(false);
        hillChartPointRepository.save(scope);
      }
    });
  }

  /**
   * Re-enable auto-progress for a scope.
   * 
   * @param scopeId the scope ID
   * @param syncImmediately if true, sync position immediately after enabling
   */
  public void enableAutoProgress(Long scopeId, boolean syncImmediately) {
    hillChartPointRepository.findById(scopeId).ifPresent(scope -> {
      log.info("Enabling auto-progress for scope {}", scopeId);
      scope.setAutoProgressEnabled(true);
      hillChartPointRepository.save(scope);

      if (syncImmediately) {
        syncProgressIfEnabled(scopeId);
      }
    });
  }
}
