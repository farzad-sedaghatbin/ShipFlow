package com.github.farzadsedaghatbin.shipflow.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a pitch's status changes significantly.
 * Used to trigger narrative regeneration for relevant sections.
 */
@Getter
public class PitchStatusChangedEvent extends ApplicationEvent {
    
    private final Long pitchId;
    private final Long cycleId;
    private final String oldStatus;
    private final String newStatus;
    private final String pitchTitle;

    public PitchStatusChangedEvent(Object source, Long pitchId, Long cycleId, String pitchTitle, 
                                   String oldStatus, String newStatus) {
        super(source);
        this.pitchId = pitchId;
        this.cycleId = cycleId;
        this.pitchTitle = pitchTitle;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
    
    /**
     * Check if this is a significant status change that warrants narrative regeneration.
     * Significant changes include: SHIPPED, DROPPED, DONE
     */
    public boolean isSignificantChange() {
        return "SHIPPED".equals(newStatus) || 
               "DROPPED".equals(newStatus) || 
               "DONE".equals(newStatus) ||
               "CUT".equals(newStatus);
    }
}
