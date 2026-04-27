package com.github.farzadsedaghatbin.shipflow.event;

import com.github.farzadsedaghatbin.shipflow.entity.CycleNarrative;
import com.github.farzadsedaghatbin.shipflow.entity.enums.NarrativeType;
import com.github.farzadsedaghatbin.shipflow.repository.CycleNarrativeRepository;
import com.github.farzadsedaghatbin.shipflow.service.CycleNarrativeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener that triggers automatic narrative regeneration
 * when cycles or pitches undergo significant status changes.
 * 
 * This listener implements debouncing to prevent excessive regeneration
 * when multiple status changes occur in quick succession.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CycleNarrativeEventListener {
    
    private final CycleNarrativeService narrativeService;
    private final CycleNarrativeRepository narrativeRepository;
    
    // Debounce interval in minutes - don't regenerate more frequently than this
    private static final int DEBOUNCE_MINUTES = 60;
    
    // Track cycles currently being processed to prevent concurrent regeneration
    private final Set<Long> processingCycles = ConcurrentHashMap.newKeySet();

    /**
     * Handle cycle status changes - regenerate all narratives when cycle moves to 
     * significant states like SHIPPED, COMPLETED, or COOLDOWN.
     */
    @Async
    @EventListener
    public void handleCycleStatusChanged(CycleStatusChangedEvent event) {
        Long cycleId = event.getCycleId();
        String newStatus = event.getNewStatus();
        
        log.info("Received CycleStatusChangedEvent for cycle {} ({}): {} -> {}", 
                cycleId, event.getCycleName(), event.getOldStatus(), newStatus);
        
        // Only regenerate for significant status changes
        if (!isSignificantCycleStatus(newStatus)) {
            log.debug("Status {} is not significant for narrative regeneration", newStatus);
            return;
        }
        
        // Check debounce
        if (!shouldRegenerate(cycleId)) {
            log.info("Skipping regeneration for cycle {} due to debounce", cycleId);
            return;
        }
        
        // Prevent concurrent regeneration
        if (!processingCycles.add(cycleId)) {
            log.info("Cycle {} is already being processed, skipping", cycleId);
            return;
        }
        
        try {
            log.info("Auto-regenerating all narratives for cycle {} due to status change to {}", 
                    cycleId, newStatus);
            narrativeService.regenerateAllNarratives(cycleId);
            log.info("Successfully regenerated narratives for cycle {}", cycleId);
        } catch (Exception e) {
            log.error("Failed to auto-regenerate narratives for cycle {}: {}", cycleId, e.getMessage(), e);
        } finally {
            processingCycles.remove(cycleId);
        }
    }

    /**
     * Handle pitch status changes - regenerate specific narrative sections
     * based on the type of status change.
     */
    @Async
    @EventListener
    public void handlePitchStatusChanged(PitchStatusChangedEvent event) {
        if (!event.isSignificantChange()) {
            return;
        }
        
        Long cycleId = event.getCycleId();
        if (cycleId == null) {
            log.debug("Pitch {} has no cycle, skipping narrative regeneration", event.getPitchId());
            return;
        }
        
        log.info("Received PitchStatusChangedEvent for pitch {} in cycle {}: {} -> {}", 
                event.getPitchId(), cycleId, event.getOldStatus(), event.getNewStatus());
        
        // Check debounce
        if (!shouldRegenerate(cycleId)) {
            log.info("Skipping regeneration for cycle {} due to debounce", cycleId);
            return;
        }
        
        // Determine which narrative type to regenerate based on status change
        NarrativeType typeToRegenerate = determineNarrativeType(event.getNewStatus());
        if (typeToRegenerate == null) {
            return;
        }
        
        // Prevent concurrent regeneration
        if (!processingCycles.add(cycleId)) {
            log.info("Cycle {} is already being processed, skipping", cycleId);
            return;
        }
        
        try {
            log.info("Auto-regenerating {} narrative for cycle {} due to pitch {} status change", 
                    typeToRegenerate, cycleId, event.getPitchId());
            narrativeService.getOrGenerateNarrative(cycleId, typeToRegenerate, true);
            log.info("Successfully regenerated {} narrative for cycle {}", typeToRegenerate, cycleId);
        } catch (Exception e) {
            log.error("Failed to auto-regenerate {} narrative for cycle {}: {}", 
                    typeToRegenerate, cycleId, e.getMessage(), e);
        } finally {
            processingCycles.remove(cycleId);
        }
    }

    /**
     * Handle task completion events - regenerate WHAT_SHIPPED narrative
     * when tasks are completed.
     */
    @Async
    @EventListener
    public void handleTaskCompleted(TaskCompletedEvent event) {
        Long cycleId = event.getCycleId();
        if (cycleId == null) {
            return;
        }
        
        log.debug("Received TaskCompletedEvent for task {} in cycle {}", 
                event.getTaskId(), cycleId);
        
        // Task completions are frequent, apply stricter debouncing
        if (!shouldRegenerate(cycleId)) {
            return;
        }
        
        // Don't regenerate for every task - this could be enhanced with
        // batch processing or threshold-based regeneration
        log.debug("Task completion events are handled with debouncing - skipping immediate regeneration");
    }

    /**
     * Check if enough time has passed since the last regeneration to allow another.
     */
    private boolean shouldRegenerate(Long cycleId) {
        Optional<LocalDateTime> lastRegenTime = narrativeRepository
                .findByCycleId(cycleId)
                .stream()
                .map(CycleNarrative::getGeneratedAt)
                .max(LocalDateTime::compareTo);
        
        if (lastRegenTime.isEmpty()) {
            return true; // No previous narratives, allow regeneration
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(DEBOUNCE_MINUTES);
        return lastRegenTime.get().isBefore(cutoff);
    }

    /**
     * Determine if a cycle status is significant enough to trigger narrative regeneration.
     */
    private boolean isSignificantCycleStatus(String status) {
        return "SHIPPED".equalsIgnoreCase(status) || 
               "COMPLETED".equalsIgnoreCase(status) || 
               "COOLDOWN".equalsIgnoreCase(status) ||
               "DONE".equalsIgnoreCase(status);
    }

    /**
     * Determine which narrative type to regenerate based on pitch status change.
     */
    private NarrativeType determineNarrativeType(String newStatus) {
        return switch (newStatus.toUpperCase()) {
            case "SHIPPED", "DONE" -> NarrativeType.WHAT_SHIPPED;
            case "DROPPED", "CUT" -> NarrativeType.WHAT_WE_CUT;
            default -> null;
        };
    }
}
