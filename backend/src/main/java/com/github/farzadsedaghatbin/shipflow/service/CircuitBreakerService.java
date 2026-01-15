package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CircuitBreakerDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.WorkLog;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Circuit Breaker Service - Shape Up safety valve
 * Detects and flags pitches that overflow their time budget (appetite)
 * Key Shape Up principle: if work won't fit in the box, stop it rather than extend the timeline
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CircuitBreakerService {

    private final PitchRepository pitchRepository;
    private final WorkLogRepository workLogRepository;
    private final DashboardNotificationService notificationService;

    /**
     * Detect pitches that are overflowing their appetite (time budget).
     * Returns pitches where actual time spent exceeds appetite by a threshold.
     */
    public List<CircuitBreakerDTO> detectOverflowPitches(Long cycleId, double thresholdPercentage) {
        List<Pitch> activePitches = pitchRepository.findByCycleId(cycleId).stream()
                .filter(p -> p.getStatus() == PitchStatus.IN_PROGRESS || 
                            p.getStatus() == PitchStatus.STARTED ||
                            p.getStatus() == PitchStatus.TESTING)
                .toList();

        return activePitches.stream()
                .map(pitch -> {
                    double hoursSpent = calculateTotalHoursSpent(pitch.getId());
                    double daysSpent = hoursSpent / 8.0; // 8 hours per day
                    double appetiteDays = pitch.getAppetiteDays();
                    double utilizationPercentage = (daysSpent / appetiteDays) * 100.0;
                    double overflowPercentage = utilizationPercentage - 100.0;

                    return CircuitBreakerDTO.builder()
                            .pitchId(pitch.getId())
                            .pitchTitle(pitch.getTitle())
                            .appetiteDays(appetiteDays)
                            .daysSpent(daysSpent)
                            .hoursSpent(hoursSpent)
                            .utilizationPercentage(utilizationPercentage)
                            .overflowPercentage(overflowPercentage)
                            .isOverflowing(utilizationPercentage >= thresholdPercentage)
                            .isCircuitBreakerTriggered(pitch.getIsCircuitBreakerTriggered())
                            .circuitBreakerReason(pitch.getCircuitBreakerReason())
                            .circuitBreakerDate(pitch.getCircuitBreakerDate())
                            .status(pitch.getStatus().name())
                            .build();
                })
                .filter(dto -> dto.getUtilizationPercentage() >= thresholdPercentage)
                .collect(Collectors.toList());
    }

    /**
     * Get all pitches with active circuit breakers
     */
    public List<CircuitBreakerDTO> getTriggeredCircuitBreakers(Long cycleId) {
        List<Pitch> pitches = pitchRepository.findByCycleId(cycleId).stream()
                .filter(Pitch::getIsCircuitBreakerTriggered)
                .toList();

        return pitches.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Trigger circuit breaker for a pitch (flag as overflowing)
     */
    public CircuitBreakerDTO triggerCircuitBreaker(Long pitchId, String reason) {
        Pitch pitch = pitchRepository.findById(pitchId)
                .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + pitchId));

        pitch.setIsCircuitBreakerTriggered(true);
        pitch.setCircuitBreakerReason(reason);
        pitch.setCircuitBreakerDate(LocalDateTime.now());
        pitch.setStatus(PitchStatus.CIRCUIT_BREAKER);

        Pitch saved = pitchRepository.save(pitch);
        log.info("Circuit breaker triggered for pitch {}: {}", pitchId, reason);
        
        // Notify team members
        notificationService.notifyCircuitBreakerTriggered(saved);

        return toDTO(saved);
    }

    /**
     * Resolve/clear circuit breaker (e.g., after killing the work or re-scoping)
     */
    public CircuitBreakerDTO resolveCircuitBreaker(Long pitchId, PitchStatus newStatus) {
        Pitch pitch = pitchRepository.findById(pitchId)
                .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + pitchId));

        pitch.setIsCircuitBreakerTriggered(false);
        pitch.setCircuitBreakerReason(null);
        pitch.setCircuitBreakerDate(null);
        pitch.setStatus(newStatus);

        Pitch saved = pitchRepository.save(pitch);
        log.info("Circuit breaker resolved for pitch {}, new status: {}", pitchId, newStatus);

        return toDTO(saved);
    }

    /**
     * Kill the pitch (cancel it due to overflow)
     */
    public CircuitBreakerDTO killPitch(Long pitchId, String reason) {
        Pitch pitch = pitchRepository.findById(pitchId)
                .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + pitchId));

        pitch.setIsCircuitBreakerTriggered(true);
        pitch.setCircuitBreakerReason("KILLED: " + reason);
        pitch.setCircuitBreakerDate(LocalDateTime.now());
        pitch.setStatus(PitchStatus.CANCELLED);

        Pitch saved = pitchRepository.save(pitch);
        log.info("Pitch {} killed due to overflow: {}", pitchId, reason);
        // Notify team members
        notificationService.notifyPitchKilled(saved, reason);
        return toDTO(saved);
    }

    /**
     * Calculate total hours worked on a pitch
     */
    private double calculateTotalHoursSpent(Long pitchId) {
        List<WorkLog> workLogs = workLogRepository.findByPitchId(pitchId);
        return workLogs.stream()
                .mapToDouble(wl -> wl.getHoursSpent().doubleValue())
                .sum();
    }

    private CircuitBreakerDTO toDTO(Pitch pitch) {
        double hoursSpent = calculateTotalHoursSpent(pitch.getId());
        double daysSpent = hoursSpent / 8.0;
        double appetiteDays = pitch.getAppetiteDays();
        double utilizationPercentage = (daysSpent / appetiteDays) * 100.0;
        double overflowPercentage = utilizationPercentage - 100.0;

        return CircuitBreakerDTO.builder()
                .pitchId(pitch.getId())
                .pitchTitle(pitch.getTitle())
                .appetiteDays(appetiteDays)
                .daysSpent(daysSpent)
                .hoursSpent(hoursSpent)
                .utilizationPercentage(utilizationPercentage)
                .overflowPercentage(overflowPercentage)
                .isOverflowing(utilizationPercentage > 100.0)
                .isCircuitBreakerTriggered(pitch.getIsCircuitBreakerTriggered())
                .circuitBreakerReason(pitch.getCircuitBreakerReason())
                .circuitBreakerDate(pitch.getCircuitBreakerDate())
                .status(pitch.getStatus().name())
                .build();
    }
}
