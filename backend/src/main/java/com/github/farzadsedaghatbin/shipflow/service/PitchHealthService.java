package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.health.CycleHealthSummaryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.health.PitchHealthDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.HillChartPoint;
import com.github.farzadsedaghatbin.shipflow.entity.Meeting;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.WorkLog;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.MeetingType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.HillChartPointRepository;
import com.github.farzadsedaghatbin.shipflow.repository.MeetingRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating pitch health summaries for non-technical stakeholders.
 * 
 * AUTOMATED RISK DETECTION:
 * This service implements sophisticated automated risk detection that analyzes multiple
 * data points to calculate health status WITHOUT requiring manual assignment:
 * 
 * 1. Budget Analysis (configurable weight, default 25%):
 *    - Compares actual hours spent vs. appetite (budget)
 *    - Flags when spending is ahead of schedule or over budget
 *    - Considers burn rate acceleration
 * 
 * 2. Bug Analysis (configurable weight, default 30%):
 *    - Tracks critical/blocker bugs (highest priority)
 *    - Monitors high-severity bug counts
 *    - Analyzes bug density and resolution rates
 *    - Detects recent bug influx (regression indicators)
 * 
 * 3. Scope Completion Analysis (configurable weight, default 25%):
 *    - Uses hill chart positions to track understanding and progress
 *    - Compares expected progress vs. actual scope completion
 *    - Identifies stagnant scopes (no recent movement)
 *    - Flags scopes stuck at decision points
 * 
 * 4. Time & Status Analysis (configurable weight, default 20%):
 *    - Evaluates status appropriateness for time remaining
 *    - Detects deadline pressure situations
 *    - Checks for delayed starts or slow progress
 * 
 * WEIGHT CUSTOMIZATION:
 * - Weights are configurable per organization via settings
 * - Preset profiles available: balanced, conservative, aggressive, quality_focused, time_critical
 * - Weights must sum to 100%
 * 
 * MODES:
 * - Fast Mode (default): Rule-based calculation for instant results
 * - AI Mode (optional): Enhanced with AI-powered risk analysis (slower)
 * 
 * The service provides both pitch-level and cycle-level health summaries,
 * with automatic sorting and visual indicators for critical items.
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
    private final BugReportRepository bugReportRepository;
    private final HillChartPointRepository hillChartPointRepository;
    
    @Autowired(required = false)
    private RiskAnalysisService riskAnalysisService;
    
    @Autowired(required = false)
    private OrganizationSettingsService organizationSettingsService;
    
    /**
     * Get risk thresholds from organization settings, or use defaults if not available.
     */
    private com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.RiskThresholds getThresholds() {
        if (organizationSettingsService != null) {
            try {
                var settings = organizationSettingsService.getSettings();
                if (settings != null && settings.getRiskThresholds() != null) {
                    return settings.getRiskThresholds();
                }
            } catch (Exception e) {
                log.warn("Failed to load organization settings, using defaults: {}", e.getMessage());
            }
        }
        // Return defaults if service not available or settings not configured
        return new com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.RiskThresholds();
    }

    /**
     * Get risk weights from organization settings, or use defaults if not available.
     */
    private com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.RiskWeights getWeights() {
        if (organizationSettingsService != null) {
            try {
                var settings = organizationSettingsService.getSettings();
                if (settings != null && settings.getRiskWeights() != null) {
                    return settings.getRiskWeights();
                }
            } catch (Exception e) {
                log.warn("Failed to load organization settings, using default weights: {}", e.getMessage());
            }
        }
        // Return defaults if service not available or settings not configured
        return com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.RiskWeights.builder()
                .budgetWeight(25)
                .bugsWeight(30)
                .scopeWeight(25)
                .timeWeight(20)
                .build();
    }

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
        
        // Calculate risk trend
        String riskTrend = calculateRiskTrend(pitch, appetiteUsed, daysLeft);

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
                .riskTrend(riskTrend)
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
     * Includes automated detection based on:
     * - Budget vs time progress (weighted 25%)
     * - Critical/blocker bug counts (weighted 30%)
     * - Scope completion status (weighted 25%)
     * - Time remaining and status (weighted 20%)
     * 
     * This provides immediate, data-driven risk assessment without manual input.
     */
    private PitchRiskDTO.RiskLevel calculateRuleBasedRiskLevel(Pitch pitch, Map<Long, Double> hoursMap) {
        // Get configurable thresholds and weights from organization settings
        var thresholds = getThresholds();
        var weights = getWeights();
        
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
        
        // Enhanced rule-based risk calculation with weighted scoring
        // Calculate individual risk factor scores (0-100 scale)
        double budgetRisk = calculateBudgetRisk(appetiteUsed, cycleProgress, thresholds);
        double bugsRisk = calculateBugsRisk(pitch, daysElapsed, totalCycleDays, thresholds);
        double scopeRisk = calculateScopeRisk(pitch, cycleProgress, thresholds);
        double timeRisk = calculateTimeRisk(pitch, daysElapsed, totalCycleDays, thresholds);
        
        // Apply configurable weights to calculate weighted risk score
        double weightedRiskScore = 
            (budgetRisk * weights.getBudgetWeight() / 100.0) +
            (bugsRisk * weights.getBugsWeight() / 100.0) +
            (scopeRisk * weights.getScopeWeight() / 100.0) +
            (timeRisk * weights.getTimeWeight() / 100.0);
        
        // Also consider the maximum individual risk to ensure single severe factors trigger high risk
        double maxIndividualRisk = Math.max(Math.max(budgetRisk, bugsRisk), 
                                           Math.max(scopeRisk, timeRisk));
        
        // Use the maximum of:
        // 1. Weighted average (honors user's importance ratings)
        // 2. 76% of max individual risk (ensures severe single issues are reflected)
        // This allows both weighted importance and critical single factors to drive risk level
        double finalRiskScore = Math.max(weightedRiskScore, maxIndividualRisk * 0.76);
        
        // Determine risk level from blended score (using configurable thresholds)
        if (finalRiskScore > thresholds.getHighMax()) {
            return PitchRiskDTO.RiskLevel.CRITICAL;
        } else if (finalRiskScore > thresholds.getMediumMax()) {
            return PitchRiskDTO.RiskLevel.HIGH;
        } else if (finalRiskScore > thresholds.getLowMax()) {
            return PitchRiskDTO.RiskLevel.MEDIUM;
        }
        return PitchRiskDTO.RiskLevel.LOW;
    }
    
    /**
     * Calculate budget risk score (0-100 scale).
     */
    private double calculateBudgetRisk(double appetiteUsed, double cycleProgress, 
            com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.RiskThresholds thresholds) {
        double riskScore = 0;
        
        // Check if behind schedule (work progress vs time progress)
        if (cycleProgress > appetiteUsed + thresholds.getScheduleSignificantGap()) {
            riskScore += 50; // Significantly behind schedule
        } else if (cycleProgress > appetiteUsed + thresholds.getScheduleModerateGap()) {
            riskScore += 30; // Moderately behind schedule
        }
        
        // Check if over budget - scale based on severity
        if (appetiteUsed > thresholds.getBudgetCritical()) {
            riskScore += 80; // Way over budget (120%+)
        } else if (appetiteUsed > thresholds.getBudgetOverrun()) {
            // Scale from 50-70 based on how far over 100%
            double overrun = appetiteUsed - thresholds.getBudgetOverrun();
            riskScore += 50 + Math.min(30, overrun * 1.5);
        } else if (appetiteUsed > thresholds.getBudgetWarning()) {
            // Scale from 20-40 based on how close to 100%
            double warningLevel = (appetiteUsed - thresholds.getBudgetWarning()) / 
                                 (thresholds.getBudgetOverrun() - thresholds.getBudgetWarning());
            riskScore += 20 + (warningLevel * 30);
        }
        
        return Math.min(riskScore, 100); // Cap at 100
    }
    
    /**
     * Calculate bugs risk score (0-100 scale).
     */
    private double calculateBugsRisk(Pitch pitch, long daysElapsed, long totalCycleDays,
            com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.RiskThresholds thresholds) {
        double riskScore = 0;
        int daysLeft = (int) Math.max(0, totalCycleDays - daysElapsed);
        
        List<BugReport> bugs = bugReportRepository.findByPitchId(pitch.getId());
        
        // Critical/blocker bugs - highest priority
        long criticalBugs = bugs.stream()
                .filter(b -> (b.getSeverity() == BugSeverity.CRITICAL || b.getSeverity() == BugSeverity.BLOCKER)
                        && b.getStatus() != BugStatus.RESOLVED && b.getStatus() != BugStatus.CLOSED)
                .count();
        
        if (criticalBugs >= thresholds.getCriticalBugsSevere()) {
            riskScore += 80; // Critical: Many blocker bugs
        } else if (criticalBugs >= thresholds.getCriticalBugsModerate()) {
            riskScore += 60; // Multiple critical bugs
        } else if (criticalBugs >= thresholds.getCriticalBugsMinor()) {
            riskScore += 40; // At least one critical bug
        }
        
        // Major severity bugs
        long majorSeverityBugs = bugs.stream()
                .filter(b -> b.getSeverity() == BugSeverity.MAJOR
                        && b.getStatus() != BugStatus.RESOLVED && b.getStatus() != BugStatus.CLOSED)
                .count();
        
        if (majorSeverityBugs > thresholds.getMajorBugsHigh() && daysLeft < thresholds.getDaysWarning()) {
            riskScore += 30; // Many major-severity bugs near deadline
        } else if (majorSeverityBugs > thresholds.getMajorBugsThreshold()) {
            riskScore += 15; // Several major-severity bugs
        }
        
        // Count all open bugs
        long openBugs = bugs.stream()
                .filter(b -> b.getStatus() != BugStatus.RESOLVED && b.getStatus() != BugStatus.CLOSED)
                .count();
        
        // Bug density analysis
        if (openBugs > thresholds.getOpenBugsCritical() && daysLeft < thresholds.getDaysWarning()) {
            riskScore += 35; // High bug count near deadline
        } else if (openBugs > thresholds.getOpenBugsHigh() && daysLeft < thresholds.getDaysWarning()) {
            riskScore += 25; // Too many open bugs with little time
        } else if (openBugs > thresholds.getOpenBugsModerate() && daysLeft < thresholds.getDaysUrgent()) {
            riskScore += 15; // Several bugs with very little time
        }
        
        // Check for recent bug influx
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        long recentBugs = bugs.stream()
                .filter(b -> b.getCreatedAt().isAfter(threeDaysAgo))
                .count();
        
        if (recentBugs > thresholds.getRecentBugInflux() && pitch.getStatus() == PitchStatus.TESTING) {
            riskScore += 20; // Many bugs found in testing recently
        }
        
        // Bug resolution rate analysis
        long resolvedBugs = bugs.stream()
                .filter(b -> b.getStatus() == BugStatus.RESOLVED || b.getStatus() == BugStatus.CLOSED)
                .count();
        
        double bugResolutionRate = bugs.size() > 0 ? (double) resolvedBugs / bugs.size() * 100 : 100;
        
        if (bugResolutionRate < thresholds.getBugResolutionRateMin() && 
                openBugs > thresholds.getMajorBugsThreshold() && daysLeft < thresholds.getDaysWarning()) {
            riskScore += 20; // Low resolution rate with many open bugs near deadline
        }
        
        return Math.min(riskScore, 100); // Cap at 100
    }
    
    /**
     * Calculate scope risk score (0-100 scale) based on hill chart progress.
     */
    private double calculateScopeRisk(Pitch pitch, double cycleProgress,
            com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.RiskThresholds thresholds) {
        double riskScore = 0;
        
        List<HillChartPoint> scopes = hillChartPointRepository.findByPitchId(pitch.getId());
        if (!scopes.isEmpty()) {
            double avgPosition = scopes.stream()
                    .mapToInt(HillChartPoint::getPosition)
                    .average()
                    .orElse(0);
            
            // Calculate expected position based on cycle progress
            double expectedPosition = cycleProgress * thresholds.getScopeExpectedProgressRate();
            double positionGap = expectedPosition - avgPosition;
            
            // If we're late in the cycle but scopes are still uphill, that's risky
            if (cycleProgress > thresholds.getCycleFinalQuarter() && avgPosition < thresholds.getScopeUphillMax()) {
                riskScore += 60; // Critical: Still figuring things out in the final quarter
            } else if (cycleProgress > thresholds.getCycleLatePhase() && avgPosition < thresholds.getScopeMidPhase()) {
                riskScore += 45; // High risk: Past 60% but scopes still mostly uphill
            } else if (cycleProgress > thresholds.getCycleMidpoint() && avgPosition < thresholds.getScopeEarlyPhase()) {
                riskScore += 30; // Halfway through but still early in understanding
            } else if (positionGap > thresholds.getScopeLagSignificant()) {
                riskScore += 20; // Scope progress lagging behind time progress
            }
            
            // Check for stagnant scopes
            long stagnantScopes = scopes.stream()
                    .filter(s -> s.getPosition() < thresholds.getScopeUphillMax() && 
                            s.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(thresholds.getScopeStagnationDays())))
                    .count();
            
            if (stagnantScopes > 0 && cycleProgress > 40) {
                riskScore += 25 * (int) Math.min(stagnantScopes, 3); // Up to 75 points
            }
            
            // Check for scopes stuck at the peak
            long peakStuckScopes = scopes.stream()
                    .filter(s -> s.getPosition() >= thresholds.getScopePeakMin() && s.getPosition() <= thresholds.getScopePeakMax() &&
                            s.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(thresholds.getPeakStuckDays())))
                    .count();
            
            if (peakStuckScopes > 0 && cycleProgress > thresholds.getCycleMidpoint()) {
                riskScore += 20 * (int) Math.min(peakStuckScopes, 2);
            }
        } else if (cycleProgress > thresholds.getCycleMinForScopes()) {
            // No scopes defined after threshold % of cycle
            riskScore += 40;
        }
        
        return Math.min(riskScore, 100); // Cap at 100
    }
    
    /**
     * Calculate time risk score (0-100 scale) based on status vs time remaining.
     */
    private double calculateTimeRisk(Pitch pitch, long daysElapsed, long totalCycleDays,
            com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.RiskThresholds thresholds) {
        double riskScore = 0;
        int daysLeft = (int) Math.max(0, totalCycleDays - daysElapsed);
        PitchStatus status = pitch.getStatus();
        
        if (daysLeft <= thresholds.getDaysUrgent() && status != PitchStatus.DONE && status != PitchStatus.TESTING) {
            riskScore += 60; // Not in testing/done with only few days left
        } else if (daysLeft <= thresholds.getDaysWarning() && status == PitchStatus.PENDING) {
            riskScore += 40; // Not started with only a week left
        }
        
        return Math.min(riskScore, 100); // Cap at 100
    }
    
    /**
     * Calculate risk trend based on recent changes.
     * Analyzes if conditions are improving, stable, or worsening.
     */
    private String calculateRiskTrend(Pitch pitch, double appetiteUsed, int daysLeft) {
        var thresholds = getThresholds();
        int trendScore = 0;
        
        // 1. Check if appetite usage is accelerating
        // Compare recent work rate vs overall average
        LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
        List<WorkLog> allLogs = workLogRepository.findByPitchId(pitch.getId());
        List<WorkLog> recentLogs = allLogs.stream()
                .filter(log -> log.getDate().isAfter(threeDaysAgo))
                .collect(Collectors.toList());
        
        double recentHours = recentLogs.stream()
                .mapToDouble(log -> log.getHoursSpent() != null ? log.getHoursSpent().doubleValue() : 0.0)
                .sum();
        
        // If spending is accelerating and already over budget, that's worsening
        if (appetiteUsed > thresholds.getAppetiteHighUsage() && recentHours > thresholds.getRecentWorkHighHours()) {
            trendScore -= 2; // Worsening
        }
        
        // 2. Check hill chart movement
        List<HillChartPoint> scopes = hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(pitch.getId());
        if (!scopes.isEmpty()) {
            // Check if any scopes moved recently
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(thresholds.getNoProgressDays());
            boolean hasRecentProgress = scopes.stream()
                    .anyMatch(s -> s.getUpdatedAt().isAfter(sevenDaysAgo));
            
            if (!hasRecentProgress && daysLeft < thresholds.getDaysConcern()) {
                trendScore -= 1; // No progress recently
            } else if (hasRecentProgress) {
                trendScore += 1; // Making progress
            }
        }
        
        // 3. Check bug trend
        LocalDateTime threeDaysAgoTime = LocalDateTime.now().minusDays(3);
        List<BugReport> recentBugs = bugReportRepository.findByPitchId(pitch.getId()).stream()
                .filter(b -> b.getCreatedAt().isAfter(threeDaysAgoTime))
                .collect(Collectors.toList());
        
        long recentCriticalBugs = recentBugs.stream()
                .filter(b -> b.getSeverity() == BugSeverity.CRITICAL || b.getSeverity() == BugSeverity.BLOCKER)
                .count();
        
        if (recentCriticalBugs > 0) {
            trendScore -= 2; // New critical bugs = worsening
        }
        
        // 4. Check if getting closer to deadline without being done
        if (daysLeft <= 5 && pitch.getStatus() != PitchStatus.DONE && pitch.getStatus() != PitchStatus.TESTING) {
            trendScore -= 1; // Time pressure increasing
        }
        
        // Determine trend
        if (trendScore <= -2) {
            return "WORSENING";
        } else if (trendScore >= 1) {
            return "IMPROVING";
        } else {
            return "STABLE";
        }
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

    /**
     * Generate human-readable status summary with automated risk indicators.
     * Provides context-aware messages based on budget, time, and QA status.
     */
    private String generateStatusSummary(Pitch pitch, double appetiteUsed, int daysLeft, String qaStatus) {
        var thresholds = getThresholds();
        StringBuilder summary = new StringBuilder();
        
        PitchStatus status = pitch.getStatus();
        
        // Get bug info for enhanced status messages
        List<BugReport> bugs = bugReportRepository.findByPitchId(pitch.getId());
        long criticalBugs = bugs.stream()
                .filter(b -> (b.getSeverity() == BugSeverity.CRITICAL || b.getSeverity() == BugSeverity.BLOCKER)
                        && b.getStatus() != BugStatus.RESOLVED && b.getStatus() != BugStatus.CLOSED)
                .count();
        long openBugs = bugs.stream()
                .filter(b -> b.getStatus() != BugStatus.RESOLVED && b.getStatus() != BugStatus.CLOSED)
                .count();
        
        switch (status) {
            case PENDING:
                if (daysLeft < thresholds.getDaysWarning()) {
                    summary.append("Not started - Only ").append(daysLeft).append(" days left");
                } else {
                    summary.append("Not started");
                }
                break;
            case STARTED:
                summary.append("Just kicked off");
                break;
            case SHAPED:
                summary.append("Shaped and ready");
                break;
            case IN_PROGRESS:
                if (appetiteUsed > thresholds.getBudgetCritical()) {
                    summary.append("Significantly over budget (").append(String.format("%.0f", appetiteUsed)).append("%)");
                } else if (appetiteUsed > thresholds.getBudgetOverrun()) {
                    summary.append("Over budget (").append(String.format("%.0f", appetiteUsed)).append("%)");
                } else if (appetiteUsed > thresholds.getBudgetWarning() && daysLeft > thresholds.getDaysWarning()) {
                    summary.append("On track");
                } else if (appetiteUsed < 30 && daysLeft < 10) {
                    summary.append("Behind schedule - needs attention");
                } else if (appetiteUsed > thresholds.getBudgetWarning() && daysLeft < thresholds.getDaysUrgent()) {
                    summary.append("High budget usage with limited time");
                } else {
                    summary.append("In progress");
                }
                break;
            case TESTING:
                if (criticalBugs > 0) {
                    summary.append("In QA - ").append(criticalBugs).append(" critical bug");
                    if (criticalBugs > 1) summary.append("s");
                } else if (openBugs > thresholds.getOpenBugsModerate()) {
                    summary.append("In QA - ").append(openBugs).append(" open bugs");
                } else {
                    summary.append("In QA - looking good");
                }
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
            summary.append(" - QA not started");
        }

        // Add time pressure note
        if (daysLeft <= thresholds.getDaysUrgent() && daysLeft > 0 && status != PitchStatus.DONE && status != PitchStatus.CANCELLED) {
            summary.append(" - ").append(daysLeft).append(" days remaining");
        } else if (daysLeft == 0 && status != PitchStatus.DONE) {
            summary.append(" - DUE TODAY");
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
