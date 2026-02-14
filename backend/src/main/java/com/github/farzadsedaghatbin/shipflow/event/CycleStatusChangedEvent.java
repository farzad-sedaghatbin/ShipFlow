package com.github.farzadsedaghatbin.shipflow.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a cycle's status changes.
 * Used to trigger automatic narrative regeneration.
 */
@Getter
public class CycleStatusChangedEvent extends ApplicationEvent {
    
    private final Long cycleId;
    private final String oldStatus;
    private final String newStatus;
    private final String cycleName;

    public CycleStatusChangedEvent(Object source, Long cycleId, String cycleName, String oldStatus, String newStatus) {
        super(source);
        this.cycleId = cycleId;
        this.cycleName = cycleName;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}
