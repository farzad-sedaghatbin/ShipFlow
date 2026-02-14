package com.github.farzadsedaghatbin.shipflow.event;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a task's status changes.
 * Used to trigger scope progress synchronization.
 */
@Getter
public class TaskStatusChangedEvent extends ApplicationEvent {

  private final Long taskId;
  private final Long pitchId;
  private final Long scopeId;
  private final Long parentTaskId;
  private final TaskStatus oldStatus;
  private final TaskStatus newStatus;

  public TaskStatusChangedEvent(Object source, Long taskId, Long pitchId, Long scopeId,
      Long parentTaskId, TaskStatus oldStatus, TaskStatus newStatus) {
    super(source);
    this.taskId = taskId;
    this.pitchId = pitchId;
    this.scopeId = scopeId;
    this.parentTaskId = parentTaskId;
    this.oldStatus = oldStatus;
    this.newStatus = newStatus;
  }

  /**
   * Check if the status changed to a completion state (DONE or CANCELLED).
   */
  public boolean isCompletionChange() {
    return (newStatus == TaskStatus.DONE || newStatus == TaskStatus.CANCELLED)
        && oldStatus != TaskStatus.DONE && oldStatus != TaskStatus.CANCELLED;
  }

  /**
   * Check if any status change occurred that might affect progress.
   */
  public boolean affectsProgress() {
    return oldStatus != newStatus;
  }
}
