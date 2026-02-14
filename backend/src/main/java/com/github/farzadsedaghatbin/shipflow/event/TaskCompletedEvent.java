package com.github.farzadsedaghatbin.shipflow.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a significant number of tasks are completed in a pitch.
 * Used to trigger WHAT_SHIPPED narrative regeneration.
 */
@Getter
public class TaskCompletedEvent extends ApplicationEvent {
    
    private final Long taskId;
    private final Long pitchId;
    private final Long cycleId;
    private final String taskTitle;

    public TaskCompletedEvent(Object source, Long taskId, Long pitchId, Long cycleId, String taskTitle) {
        super(source);
        this.taskId = taskId;
        this.pitchId = pitchId;
        this.cycleId = cycleId;
        this.taskTitle = taskTitle;
    }
}
