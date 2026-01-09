package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.health.CycleHealthSummaryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.health.PitchHealthDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Meeting;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.enums.MeetingType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.MeetingRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating pitch health summaries for non-technical stakeholders.
 * Supports both fast (rule-based) and AI-enhanced analysis modes.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PitchHealthService {

    private static final double HOURS_PER_DAY = 8.0;

    private final PitchRepository pitchRepository;
    private final CycleRepository cycleRepository;
    private final WorkLogRepository workLogRepository;
    private final MeetingRepository meetingRepository;
    
    @Autowired(required = false)
    private RiskAnalysisService riskAnalysisService;

    /**
     * Get health summary for a single pitch (fast mode - no AI).
     */
    public PitchHealthDTO getPitchHealth(Long pitchId) {
        return getPitchHealth(pitchId, false);
    }

    /**
     * Get health summary for a single pitch.
     * @param pitchId The pitch ID
     * @param includeAI If true, includes AI-based risk analysis (slower)
     */
    public PitchHealthDTO getPitchHealth(Long pitchId, boolean includeAI) {
        Pitch pitch = pitchRepository.findById(pitchId)
                .orElseThrow(() -> new RuntimeException("Pitch not found with id: " + pitchId));
        return buildPitchHealth(pitch, includeAI, null);
    }

    /**
     * Get health summary for all pitches in a cycle (fast mode - no AI).
     */
    public CycleHealthSummaryDTO getCycleHealthSummary(Long cycleId) {
        return getCycleHealthSummary(cycleId, false);
    }

    /**
     * Get health summary for all pitches in a cycle.
     * @param cycleId The cycle ID
     * @param includeAI If true, includes AI-based risk analysis (slower)
     */
    public CycleHealthSummaryDTO getCycleHealthSummary(Long cycleId, boolean includeAI) {
        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new RuntimeException("Cycle not found with id: " + cycleId));

        List<Pitch> pitches = pitchRepository.findByCycleId(cycleId);
        
        // Batch load all work hours for the cycle to avoid N+1 queries
        Map<Long, Double> hoursMap = batchLoadWorkHoursForCycle(cycleId);
        
        List<PitchHealthDTO> pitchHealthList = pitches.stream()
                .map(p -> buildPitchHealth(p, includeAI, hoursMap))
                .collect(Collectors.toList());

        // Calculate statistics
        LocalDate today = LocalDate.now();
        int daysLeft = (int) Math.max(0, ChronoUnit.DAYS.between(today, cycle.getEndDate()));
        long totalDays = ChronoUnit.DAYS.between(cycle.getStartDate(), cycle.getEndDate());
        long elapsedDays = ChronoUnit.DAYS.between(cycle.getStartDate(), today);
        double cycleProgress = totalDays > 0 ? Math.min(100, (double) elapsedDays / totalDays * 100) : 0;

        // Risk breakdown
        int healthyCount = (int) pitchHealthList.stream()
                .filter(p -> p.getRiskLevel() == PitchRiskDTO.RiskLevel.LOW)
                .count();
        int atRiskCount = (int) pitchHealthList.stream()
                .filter(p -> p.getRiskLevel() == PitchRiskDTO.RiskLevel.MEDIUM)
                .count();
        int criticalCount = (int) pitchHealthList.stream()
                .filter(p -> p.getRiskLevel() == PitchRiskDTO.RiskLevel.HIGH || 
                           p.getRiskLevel() == PitchRiskDTO.RiskLevel.CRITICAL)
                .count();

        // Budget calculations
        double totalAppetiteHours = pitchHealthList.stream()
                .mapToDouble(p -> p.getAppetiteHours() != null ? p.getAppetiteHours() : 0)
                .sum();
        double totalActualHours = pitchHealthList.stream()
                .mapToDouble(p -> p.getActualHours() != null ? p.getActualHours() : 0)
                .sum();
        double budgetUsed = totalAppetiteHours > 0 ? (totalActualHours / totalAppetiteHours) * 100 : 0;

        // Status breakdown
        int pendingCount = (int) pitchHealthList.stream()
                .filter(p -> "PENDING".equals(p.getStatus()) || "STARTED".equals(p.getStatus()))
                .count();
        int inProgressCount = (int) pitchHealthList.stream()
                .filter(p -> "IN_PROGRESS".equals(p.getStatus()))
                .count();
        int testingCount = (int) pitchHealthList.stream()
                .filter(p -> "TESTING".equals(p.getStatus()))
                .count();
        int doneCount = (int) pitchHealthList.stream()
                .filter(p -> "DONE".equals(p.getStatus()))
                .count();

        // Determine overall health
        PitchRiskDTO.RiskLevel overallHealth;
        if (criticalCount > 0) {
            overallHealth = PitchRiskDTO.RiskLevel.HIGH;
        } else if (atRiskCount > healthyCount) {
            overallHealth = PitchRiskDTO.RiskLevel.MEDIUM;
        } else {
            overallHealth = PitchRiskDTO.RiskLevel.LOW;
        }

        return CycleHealthSummaryDTO.builder()
                .cycleId(cycle.getId())
                .cycleName(cycle.getName())
                .projectName(cycle.getProject() != null ? cycle.getProject().getName() : null)
                .projectKey(cycle.getProject() != null ? cycle.getProject().getProjectKey() : null)
                .overallHealth(overallHealth)
                .healthColor(getRiskColor(overallHealth))
                .startDate(cycle.getStartDate())
                .endDate(cycle.getEndDate())
                .daysLeft(daysLeft)
                .cycleProgressPercent(cycleProgress)
                .totalPitches(pitches.size())
                .healthyPitches(healthyCount)
                .atRiskPitches(atRiskCount)
                .criticalPitches(criticalCount)
                .totalAppetiteHours(totalAppetiteHours)
                .totalActualHours(totalActualHours)
                .budgetUsedPercent(budgetUsed)
                .pendingCount(pendingCount)
                .inProgressCount(inProgressCount)
                .testingCount(testingCount)
                .doneCount(doneCount)
                .pitchHealthList(pitchHealthList)
                .build();
    }

    /**
     * Get health summary for all active cycles (fast mode - no AI).
     */
    public List<CycleHealthSummaryDTO> getAllActiveCycleHealth() {
        return getAllActiveCycleHealth(false);
    }

    /**
     * Get health summary for all active cycles.
     * @param includeAI If true, includes AI-based risk analysis (slower)
     */
    public List<CycleHealthSummaryDTO> getAllActiveCycleHealth(boolean includeAI) {
        List<Cycle> activeCycles = cycleRepository.findByIsActiveTrue();
        return activeCycles.stream()
                .map(cycle -> getCycleHealthSummary(cycle.getId(), includeAI))
                .collect(Collectors.toList());
    }

    /**
     * Batch load work hours for a cycle using a single query.
     */
    private Map<Long, Double> batchLoadWorkHoursForCycle(Long cycleId) {
        Map<Long, Double> hoursMap = new HashMap<>();
        List<Object[]> results = workLogRepository.getTotalHoursByCycleId(cycleId);
        for (Object[] row : results) {
            Long pitchId = (Long) row[0];
            Double hours = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            hoursMap.put(pitchId, hours);
        }
        return hoursMap;
    }

    /**
     * Batch load work hours for multiple pitches to avoid N+1 queries.
     */
    private Map<Long, Double> batchLoadWorkHours(List<Pitch> pitches) {
        if (pitches.isEmpty()) {
            return new HashMap<>();
        }
        List<Long> pitchIds = pitches.stream().map(Pitch::getId).collect(Collectors.toList());
        Map<Long, Double> hoursMap = new HashMap<>();
        List<Object[]> results = workLogRepository.getTotalHoursByPitchIds(pitchIds);
        for (Object[] row : results) {
            Long pitchId = (Long) row[0];
            Double hours = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            hoursMap.put(pitchId, hours);
        }
        // Ensure all pitch IDs are in the map (even with 0 hours)
        for (Pitch pitch : pitches) {
            hoursMap.putIfAbsent(pitch.getId(), 0.0);
        }
        return hoursMap;
    }

    private PitchHealthDTO buildPitchHealth(Pitch pitch, boolean includeAI, Map<Long, Double> hoursMap) {
        // Get risk level - use rule-based calculation for fast mode
        PitchRiskDTO.RiskLevel riskLevel;
        
        if (includeAI && riskAnalysisService != null) {
            // AI mode - slower but more accurate
            try {
                PitchRiskDTO riskDTO = riskAnalysisService.analyzePitchRisk(pitch);
                riskLevel = riskDTO.getRiskLevel();
            } catch (Exception e) {
                log.warn("Failed to get AI risk analysis for pitch {}: {}", pitch.getId(), e.getMessage());
                riskLevel = calculateRuleBasedRiskLevel(pitch, hoursMap);
            }
        } else {
            // Fast mode - rule-based only
            riskLevel = calculateRuleBasedRiskLevel(pitch, hoursMap);
        }

        // Calculate hours - use cached value if available
        Double totalHours;
        if (hoursMap != null && hoursMap.containsKey(pitch.getId())) {
            totalHours = hoursMap.get(pitch.getId());
        } else {
            totalHours = workLogRepository.getTotalHoursByPitchId(pitch.getId());
            if (totalHours == null) totalHours = 0.0;
        }
        
        double appetiteHours = pitch.getAppetiteDays() * HOURS_PER_DAY;
        double appetiteUsed = appetiteHours > 0 ? (totalHours / appetiteHours) * 100 : 0;

        // Calculate days left
        Cycle cycle = pitch.getCycle();
        LocalDate today = LocalDate.now();
        int daysLeft = (int) Math.max(0, ChronoUnit.DAYS.between(today, cycle.getEndDate()));

        // Determine QA status
        String qaStatus = determineQAStatus(pitch);

        // Generate status summary
        String statusSummary = generateStatusSummary(pitch, appetiteUsed, daysLeft, qaStatus);

        return PitchHealthDTO.builder()
                .pitchId(pitch.getId())
                .pitchName(pitch.getTitle())
                .projectName(cycle.getProject() != null ? cycle.getProject().getName() : null)
                .projectKey(cycle.getProject() != null ? cycle.getProject().getProjectKey() : null)
                .cycleName(cycle.getName())
                .riskLevel(riskLevel)
                .riskColor(getRiskColor(riskLevel))
                .appetiteUsedPercent(Math.round(appetiteUsed * 10.0) / 10.0)
                .daysLeft(daysLeft)
                .statusSummary(statusSummary)
                .status(pitch.getStatus().name())
                .teamName(pitch.getTeam() != null ? pitch.getTeam().getName() : "Unassigned")
                .appetiteHours(appetiteHours)
                .actualHours(totalHours)
                .qaStatus(qaStatus)
                .cycleEndDate(cycle.getEndDate())
                .build();
    }

    /**
     * Fast rule-based risk level calculation without AI.
     */
    private PitchRiskDTO.RiskLevel calculateRuleBasedRiskLevel(Pitch pitch, Map<Long, Double> hoursMap) {
        // Get hours
        Double totalHours;
        if (hoursMap != null && hoursMap.containsKey(pitch.getId())) {
            totalHours = hoursMap.get(pitch.getId());
        } else {
            totalHours = workLogRepository.getTotalHoursByPitchId(pitch.getId());
            if (totalHours == null) totalHours = 0.0;
        }
        
        double appetiteHours = pitch.getAppetiteDays() * HOURS_PER_DAY;
        double appetiteUsed = appetiteHours > 0 ? (totalHours / appetiteHours) * 100 : 0;
        
        // Calculate cycle progress
        Cycle cycle = pitch.getCycle();
        LocalDate today = LocalDate.now();
        long daysElapsed = ChronoUnit.DAYS.between(cycle.getStartDate(), today);
        long totalCycleDays = ChronoUnit.DAYS.between(cycle.getStartDate(), cycle.getEndDate());
        double cycleProgress = totalCycleDays > 0 ? (double) daysElapsed / totalCycleDays * 100 : 0;
        
        // Simple rule-based risk calculation
        int riskScore = 0;
        
        // Check if behind schedule
        if (cycleProgress > appetiteUsed + 30) {
            riskScore += 30;
        } else if (cycleProgress > appetiteUsed + 15) {
            riskScore += 15;
        }
        
        // Check if over budget
        if (appetiteUsed > 120) {
            riskScore += 40;
        } else if (appetiteUsed > 100) {
            riskScore += 25;
        } else if (appetiteUsed > 80) {
            riskScore += 10;
        }
        
        // Check status vs time remaining
        int daysLeft = (int) Math.max(0, totalCycleDays - daysElapsed);
        PitchStatus status = pitch.getStatus();
        
        if (daysLeft <= 3 && status != PitchStatus.DONE && status != PitchStatus.TESTING) {
            riskScore += 30;
        } else if (daysLeft <= 7 && status == PitchStatus.PENDING) {
            riskScore += 20;
        }
        
        // Determine risk level from score
        if (riskScore >= 70) {
            return PitchRiskDTO.RiskLevel.CRITICAL;
        } else if (riskScore >= 50) {
            return PitchRiskDTO.RiskLevel.HIGH;
        } else if (riskScore >= 25) {
            return PitchRiskDTO.RiskLevel.MEDIUM;
        }
        return PitchRiskDTO.RiskLevel.LOW;
    }

    private String determineQAStatus(Pitch pitch) {
        if (pitch.getStatus() == PitchStatus.TESTING) {
            return "IN_PROGRESS";
        } else if (pitch.getStatus() == PitchStatus.DONE) {
            return "COMPLETED";
        }
        
        // Check if there's been any QA-related meeting
        List<Meeting> meetings = meetingRepository.findByPitchId(pitch.getId());
        boolean hasQAActivity = meetings.stream()
                .anyMatch(m -> m.getType() == MeetingType.DEMO || 
                              (m.getNotes() != null && m.getNotes().toLowerCase().contains("qa")));
        
        if (hasQAActivity) {
            return "IN_PROGRESS";
        }
        return "NOT_STARTED";
    }

    private String generateStatusSummary(Pitch pitch, double appetiteUsed, int daysLeft, String qaStatus) {
        StringBuilder summary = new StringBuilder();
        
        PitchStatus status = pitch.getStatus();
        switch (status) {
            case PENDING:
                summary.append("Not started");
                break;
            case STARTED:
                summary.append("Just kicked off");
                break;
            case SHAPED:
                summary.append("Shaped");
                break;
            case IN_PROGRESS:
                if (appetiteUsed > 100) {
                    summary.append("Over budget");
                } else if (appetiteUsed > 80 && daysLeft > 7) {
                    summary.append("On track");
                } else if (appetiteUsed < 30 && daysLeft < 14) {
                    summary.append("Needs attention");
                } else {
                    summary.append("In progress");
                }
                break;
            case TESTING:
                summary.append("In QA");
                break;
            case DONE:
                summary.append("Completed");
                break;
            case COOLDOWN:
                summary.append("In cooldown");
                break;
            case CANCELLED:
                summary.append("Cancelled");
                break;
        }

        // Add QA note if relevant
        if ("NOT_STARTED".equals(qaStatus) && status == PitchStatus.IN_PROGRESS && appetiteUsed > 60) {
            summary.append(" • QA not started");
        }

        // Add time pressure note
        if (daysLeft <= 5 && status != PitchStatus.DONE && status != PitchStatus.CANCELLED) {
            summary.append(" • ").append(daysLeft).append(" days left");
        }

        return summary.toString();
    }

    private String getRiskColor(PitchRiskDTO.RiskLevel level) {
        if (level == null) return "#9e9e9e"; // grey
        switch (level) {
            case LOW:
                return "#4caf50"; // green
            case MEDIUM:
                return "#ff9800"; // orange
            case HIGH:
                return "#f44336"; // red
            case CRITICAL:
                return "#d32f2f"; // dark red
            default:
                return "#9e9e9e"; // grey
        }
    }
}
