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
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.HillChartPointRepository;
import com.github.farzadsedaghatbin.shipflow.repository.MeetingRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for generating pitch health summaries for non-technical stakeholders.
 *
 * <p>
 * AUTOMATED RISK DETECTION: This service implements sophisticated automated
 * risk detection that analyzes multiple data points to calculate health status
 * WITHOUT requiring manual assignment:
 *
 * <p>
 * 1. Budget Analysis (configurable weight, default 25%): - Compares actual
 * hours spent vs. appetite (budget) - Flags when spending is ahead of schedule
 * or over budget - Considers burn rate acceleration
 *
 * <p>
 * 2. Bug Analysis (configurable weight, default 30%): - Tracks critical/blocker
 * bugs (highest priority) - Monitors high-severity bug counts - Analyzes bug
 * density and resolution rates - Detects recent bug influx (regression
 * indicators)
 *
 * <p>
 * 3. Scope Completion Analysis (configurable weight, default 25%): - Uses hill
 * chart positions to track understanding and progress - Compares expected
 * progress vs. actual scope completion - Identifies stagnant scopes (no recent
 * movement) - Flags scopes stuck at decision points
 *
 * <p>
 * 4. Time & Status Analysis (configurable weight, default 20%): - Evaluates
 * status appropriateness for time remaining - Detects deadline pressure
 * situations - Checks for delayed starts or slow progress
 *
 * <p>
 * WEIGHT CUSTOMIZATION: - Weights are configurable per organization via
 * settings - Preset profiles available: balanced, conservative, aggressive,
 * quality_focused, time_critical - Weights must sum to 100%
 *
 * <p>
 * MODES: - Fast Mode (default): Rule-based calculation for instant results - AI
 * Mode (optional): Enhanced with AI-powered risk analysis (slower)
 *
 * <p>
 * The service provides both pitch-level and cycle-level health summaries, with
 * automatic sorting and visual indicators for critical items.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PitchHealthService {

  /**
   * Maximum risk factor weight for hybrid risk scoring.
   *
   * <p>
   * This value (0.76 = 76%) determines the balance between weighted average risk
   * and maximum individual risk factor when calculating final risk scores. It
   * ensures that a single severe factor can trigger appropriate risk levels even
   * when other factors would dilute the weighted average.
   *
   * <p>
   * WHY 76%? - Must be > 60% to ensure a risk factor score of 80 (severe) crosses
   * the MEDIUM threshold (60) - 0.75 (75%) results in exactly 60 (80 × 0.75 =
   * 60), which fails boundary conditions - 0.76 (76%) yields 60.8 (80 × 0.76 =
   * 60.8), safely exceeding the MEDIUM threshold - Prevents weighted averaging
   * from masking critical single-factor risks - Example: 150% budget overrun
   * (risk=80) → 80×0.76=60.8 → HIGH risk level
   *
   * <p>
   * NOTE: This constant is based on default risk thresholds (MEDIUM=25-49,
   * HIGH=50-69). If organization settings customize these thresholds
   * significantly, this multiplier may need adjustment to maintain the intended
   * behavior.
   */
  private static final double MAX_RISK_FACTOR_WEIGHT = 0.76;

  private final PitchRepository pitchRepository;
  private final CycleRepository cycleRepository;
  private final WorkLogRepository workLogRepository;
  private final MeetingRepository meetingRepository;
  private final BugReportRepository bugReportRepository;
  private final HillChartPointRepository hillChartPointRepository;
  private final CapacityConfigService capacityConfigService;

  @Autowired(required = false)
  private RiskAnalysisService riskAnalysisService;

  @Autowired(required = false)
  private OrganizationSettingsService organizationSettingsService;

  /**
   * Get risk thresholds from organization settings, or use defaults if not
   * available.
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
   * Get risk weights from organization settings, or use defaults if not
   * available.
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
        .budgetWeight(
            com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.RiskWeights.DEFAULT_BUDGET_WEIGHT)
        .bugsWeight(
            com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.RiskWeights.DEFAULT_BUGS_WEIGHT)
        .scopeWeight(
            com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.RiskWeights.DEFAULT_SCOPE_WEIGHT)
        .timeWeight(
            com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.RiskWeights.DEFAULT_TIME_WEIGHT)
        .build();
  }

  /** Get health summary for a single pitch (fast mode - no AI). */
  public PitchHealthDTO getPitchHealth(Long pitchId) {
    return getPitchHealth(pitchId, false);
  }

  /**
   * Get health summary for a single pitch.
   *
   * @param pitchId
   *            The pitch ID
   * @param includeAI
   *            If true, includes AI-based risk analysis (slower)
   */
  public PitchHealthDTO getPitchHealth(Long pitchId, boolean includeAI) {
    Pitch pitch = pitchRepository.findByIdNotDeleted(pitchId)
        .orElseThrow(() -> new RuntimeException("Pitch not found with id: " + pitchId));
    return buildPitchHealth(pitch, includeAI, null);
  }

  /** Get health summary for all pitches in a cycle (fast mode - no AI). */
  public CycleHealthSummaryDTO getCycleHealthSummary(Long cycleId) {
    return getCycleHealthSummary(cycleId, false);
  }

  /**
   * Get health summary for all pitches in a cycle.
   *
   * @param cycleId
   *            The cycle ID
   * @param includeAI
   *            If true, includes AI-based risk analysis (slower)
   */
  public CycleHealthSummaryDTO getCycleHealthSummary(Long cycleId, boolean includeAI) {
    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new RuntimeException("Cycle not found with id: " + cycleId));

    List<Pitch> pitches = pitchRepository.findByCycleIdNotDeleted(cycleId);

    // Batch load all work hours for the cycle to avoid N+1 queries
    Map<Long, Double> hoursMap = batchLoadWorkHoursForCycle(cycleId);

    List<PitchHealthDTO> pitchHealthList = pitches.stream().map(p -> buildPitchHealth(p, includeAI, hoursMap))
        .collect(Collectors.toList());

    // Calculate statistics
    LocalDate today = LocalDate.now();
    int daysLeft = (int) Math.max(0, ChronoUnit.DAYS.between(today, cycle.getEndDate()));
    long totalDays = ChronoUnit.DAYS.between(cycle.getStartDate(), cycle.getEndDate());
    long elapsedDays = ChronoUnit.DAYS.between(cycle.getStartDate(), today);
    double cycleProgress = totalDays > 0 ? Math.min(100, (double) elapsedDays / totalDays * 100) : 0;

    // Risk breakdown
    int healthyCount = (int) pitchHealthList.stream().filter(p -> p.getRiskLevel() == RiskLevel.LOW).count();
    int atRiskCount = (int) pitchHealthList.stream().filter(p -> p.getRiskLevel() == RiskLevel.MEDIUM).count();
    int criticalCount = (int) pitchHealthList.stream()
        .filter(p -> p.getRiskLevel() == RiskLevel.HIGH || p.getRiskLevel() == RiskLevel.CRITICAL).count();

    // Budget calculations
    double totalAppetiteHours = pitchHealthList.stream()
        .mapToDouble(p -> p.getAppetiteHours() != null ? p.getAppetiteHours() : 0).sum();
    double totalActualHours = pitchHealthList.stream()
        .mapToDouble(p -> p.getActualHours() != null ? p.getActualHours() : 0).sum();
    double budgetUsed = totalAppetiteHours > 0 ? (totalActualHours / totalAppetiteHours) * 100 : 0;

    // Status breakdown
    int pendingCount = (int) pitchHealthList.stream()
        .filter(p -> "PENDING".equals(p.getStatus()) || "STARTED".equals(p.getStatus())).count();
    int inProgressCount = (int) pitchHealthList.stream().filter(p -> "IN_PROGRESS".equals(p.getStatus())).count();
    int testingCount = (int) pitchHealthList.stream().filter(p -> "TESTING".equals(p.getStatus())).count();
    int doneCount = (int) pitchHealthList.stream().filter(p -> "DONE".equals(p.getStatus())).count();

    // Determine overall health
    RiskLevel overallHealth;
    if (criticalCount > 0) {
      overallHealth = RiskLevel.HIGH;
    } else if (atRiskCount > healthyCount) {
      overallHealth = RiskLevel.MEDIUM;
    } else {
      overallHealth = RiskLevel.LOW;
    }

    return CycleHealthSummaryDTO.builder().cycleId(cycle.getId()).cycleName(cycle.getName())
        .projectName(cycle.getProject() != null ? cycle.getProject().getName() : null)
        .projectKey(cycle.getProject() != null ? cycle.getProject().getProjectKey() : null)
        .overallHealth(overallHealth).healthColor(getRiskColor(overallHealth)).startDate(cycle.getStartDate())
        .endDate(cycle.getEndDate()).daysLeft(daysLeft).cycleProgressPercent(cycleProgress)
        .totalPitches(pitches.size()).healthyPitches(healthyCount).atRiskPitches(atRiskCount)
        .criticalPitches(criticalCount).totalAppetiteHours(totalAppetiteHours)
        .totalActualHours(totalActualHours).budgetUsedPercent(budgetUsed).pendingCount(pendingCount)
        .inProgressCount(inProgressCount).testingCount(testingCount).doneCount(doneCount)
        .pitchHealthList(pitchHealthList).build();
  }

  /** Get health summary for all active cycles (fast mode - no AI). */
  public List<CycleHealthSummaryDTO> getAllActiveCycleHealth() {
    return getAllActiveCycleHealth(false);
  }

  /**
   * Get health summary for all active cycles.
   * Excludes cycles from KANBAN projects since they are long-term and health metrics would be incorrect.
   *
   * @param includeAI
   *            If true, includes AI-based risk analysis (slower)
   */
  public List<CycleHealthSummaryDTO> getAllActiveCycleHealth(boolean includeAI) {
    List<Cycle> activeCycles = cycleRepository.findByIsActiveTrue();
    // Filter out cycles from KANBAN projects - they are long-term and health data would be wrong
    return activeCycles.stream()
        .filter(cycle -> cycle.getProject() != null 
            && cycle.getProject().getProjectType() != ProjectType.KANBAN)
        .map(cycle -> getCycleHealthSummary(cycle.getId(), includeAI))
        .collect(Collectors.toList());
  }

  /** Batch load work hours for a cycle using a single query. */
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

  private PitchHealthDTO buildPitchHealth(Pitch pitch, boolean includeAI, Map<Long, Double> hoursMap) {
    // Get risk level - use rule-based calculation for fast mode
    RiskLevel riskLevel;

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
      if (totalHours == null)
        totalHours = 0.0;
    }

    double appetiteHours = capacityConfigService.calculatePitchAppetiteHours(pitch);
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

    // Build team budget information
    var teamBudgetInfo = buildTeamBudgetInfo(pitch, totalHours);

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
        // Team budget fields
        .teamMemberCount(teamBudgetInfo.memberCount)
        .appetiteDays(pitch.getAppetiteDays())
        .totalBudgetPersonDays(teamBudgetInfo.totalBudgetPersonDays)
        .totalHoursSpent(totalHours)
        .totalPersonDaysSpent(teamBudgetInfo.totalPersonDaysSpent)
        .budgetUtilizationPercent(teamBudgetInfo.utilizationPercent)
        .bottleneckMember(teamBudgetInfo.bottleneckMember)
        .memberBudgets(teamBudgetInfo.memberBudgets)
        .hasOverBudgetMember(teamBudgetInfo.hasOverBudgetMember)
        .build();
  }

  /**
   * Helper record for team budget calculation results.
   */
  private record TeamBudgetInfo(
      int memberCount,
      double totalBudgetPersonDays,
      double totalPersonDaysSpent,
      double utilizationPercent,
      PitchHealthDTO.MemberBudgetSummary bottleneckMember,
      java.util.List<PitchHealthDTO.MemberBudgetSummary> memberBudgets,
      boolean hasOverBudgetMember) {}

  /**
   * Build team budget information for a pitch.
   */
  private TeamBudgetInfo buildTeamBudgetInfo(Pitch pitch, Double totalHoursSpent) {
    if (pitch.getTeam() == null || pitch.getAppetiteDays() == null) {
      return new TeamBudgetInfo(0, 0, 0, 0, null, java.util.List.of(), false);
    }

    var team = pitch.getTeam();
    var teamBudget = capacityConfigService.calculateTeamBudget(team, pitch.getAppetiteDays());
    
    // Get hours spent per person from work logs
    Map<Long, Double> personHoursMap = getPersonHoursForPitch(pitch.getId());
    
    // Calculate average hours per day for person-days conversion
    double avgHoursPerDay = teamBudget.getMemberBudgets().isEmpty() ? 
        capacityConfigService.getOrganizationDefaultHoursPerDay() :
        teamBudget.getTotalDailyCapacityHours() / teamBudget.getMemberCount();
    
    // Convert member budgets to DTOs with actual hours spent
    java.util.List<PitchHealthDTO.MemberBudgetSummary> memberBudgetDTOs = teamBudget.getMemberBudgets().stream()
        .map(pb -> {
          double hoursSpent = personHoursMap.getOrDefault(pb.getPersonId(), 0.0);
          double hoursRemaining = Math.max(0, pb.getTotalBudgetHours() - hoursSpent);
          double utilization = pb.getTotalBudgetHours() > 0 ? 
              (hoursSpent / pb.getTotalBudgetHours()) * 100 : 0;
          return PitchHealthDTO.MemberBudgetSummary.builder()
              .personId(pb.getPersonId())
              .personName(pb.getPersonName())
              .role(pb.getRole() != null ? pb.getRole().name() : null)
              .hoursPerDay(pb.getHoursPerDay())
              .capacitySource(pb.getCapacitySource())
              .totalBudgetHours(pb.getTotalBudgetHours())
              .hoursSpent(hoursSpent)
              .hoursRemaining(hoursRemaining)
              .utilizationPercent(Math.round(utilization * 10.0) / 10.0)
              .isOverBudget(hoursSpent > pb.getTotalBudgetHours())
              .build();
        })
        .toList();
    
    // Find bottleneck member (highest utilization)
    PitchHealthDTO.MemberBudgetSummary bottleneck = memberBudgetDTOs.stream()
        .max((a, b) -> Double.compare(a.getUtilizationPercent(), b.getUtilizationPercent()))
        .orElse(null);
    
    // Check for any over-budget members
    boolean hasOverBudget = memberBudgetDTOs.stream()
        .anyMatch(m -> Boolean.TRUE.equals(m.getIsOverBudget()));
    
    // Calculate person-days
    double totalBudgetPersonDays = avgHoursPerDay > 0 ? 
        teamBudget.getTotalBudgetHours() / avgHoursPerDay : 0;
    double totalPersonDaysSpent = avgHoursPerDay > 0 && totalHoursSpent != null ? 
        totalHoursSpent / avgHoursPerDay : 0;
    double utilization = totalBudgetPersonDays > 0 ? 
        (totalPersonDaysSpent / totalBudgetPersonDays) * 100 : 0;
    
    return new TeamBudgetInfo(
        teamBudget.getMemberCount(),
        Math.round(totalBudgetPersonDays * 10.0) / 10.0,
        Math.round(totalPersonDaysSpent * 10.0) / 10.0,
        Math.round(utilization * 10.0) / 10.0,
        bottleneck,
        memberBudgetDTOs,
        hasOverBudget
    );
  }

  /**
   * Get hours spent by each person on a pitch.
   */
  private Map<Long, Double> getPersonHoursForPitch(Long pitchId) {
    List<WorkLog> workLogs = workLogRepository.findByPitchId(pitchId);
    return workLogs.stream()
        .filter(wl -> wl.getPerson() != null)
        .collect(Collectors.groupingBy(
            wl -> wl.getPerson().getId(),
            Collectors.summingDouble(wl -> wl.getHoursSpent() != null ? wl.getHoursSpent().doubleValue() : 0.0)
        ));
  }

  /**
   * Fast rule-based risk level calculation without AI. Includes automated
   * detection based on: - Budget vs time progress (weighted 25%) -
   * Critical/blocker bug counts (weighted 30%) - Scope completion status
   * (weighted 25%) - Time remaining and status (weighted 20%)
   *
   * <p>
   * This provides immediate, data-driven risk assessment without manual input.
   */
  /**
   * Calculate a rule-based risk score (0-100) for a pitch using the comprehensive
   * 4-factor weighted algorithm. This is the canonical risk scoring method used
   * by both the health check and risk advisory endpoints for consistency.
   *
   * @param pitch    The pitch to score
   * @param hoursMap Optional pre-loaded hours map for batch optimization (can be null)
   * @return Risk score 0-100
   */
  public int calculateRuleBasedRiskScore(Pitch pitch, Map<Long, Double> hoursMap) {
    // Get configurable thresholds and weights from organization settings
    var thresholds = getThresholds();
    var weights = getWeights();

    // Get hours
    Double totalHours;
    if (hoursMap != null && hoursMap.containsKey(pitch.getId())) {
      totalHours = hoursMap.get(pitch.getId());
    } else {
      totalHours = workLogRepository.getTotalHoursByPitchId(pitch.getId());
      if (totalHours == null)
        totalHours = 0.0;
    }

    double appetiteHours = capacityConfigService.calculatePitchAppetiteHours(pitch);
    double appetiteUsed = appetiteHours > 0 ? (totalHours / appetiteHours) * 100 : 0;

    // Calculate cycle progress
    Cycle cycle = pitch.getCycle();
    LocalDate today = LocalDate.now();
    long daysElapsed = ChronoUnit.DAYS.between(cycle.getStartDate(), today);
    long totalCycleDays = ChronoUnit.DAYS.between(cycle.getStartDate(), cycle.getEndDate());
    double cycleProgress = totalCycleDays > 0 ? (double) daysElapsed / totalCycleDays * 100 : 0;

    // Enhanced rule-based risk calculation with weighted scoring
    // Calculate individual risk factor scores (0-100 scale)
    double budgetRisk = calculateBudgetRisk(pitch, appetiteUsed, cycleProgress, totalHours, thresholds);
    double bugsRisk = calculateBugsRisk(pitch, daysElapsed, totalCycleDays, thresholds);
    double scopeRisk = calculateScopeRisk(pitch, cycleProgress, thresholds);
    double timeRisk = calculateTimeRisk(pitch, daysElapsed, totalCycleDays, thresholds);

    // Apply configurable weights to calculate weighted risk score
    double weightedRiskScore = (budgetRisk * weights.getBudgetWeight() / 100.0)
        + (bugsRisk * weights.getBugsWeight() / 100.0) + (scopeRisk * weights.getScopeWeight() / 100.0)
        + (timeRisk * weights.getTimeWeight() / 100.0);

    // Also consider the maximum individual risk to ensure single severe factors
    // trigger high risk
    double maxIndividualRisk = Math.max(Math.max(budgetRisk, bugsRisk), Math.max(scopeRisk, timeRisk));

    // Use the maximum of:
    // 1. Weighted average (honors user's importance ratings)
    // 2. Max risk factor weight (ensures severe single issues are reflected)
    // This allows both weighted importance and critical single factors to drive
    // risk level
    double finalRiskScore = Math.max(weightedRiskScore, maxIndividualRisk * MAX_RISK_FACTOR_WEIGHT);

    return (int) Math.round(Math.max(0, Math.min(100, finalRiskScore)));
  }

  private RiskLevel calculateRuleBasedRiskLevel(Pitch pitch, Map<Long, Double> hoursMap) {
    int score = calculateRuleBasedRiskScore(pitch, hoursMap);
    var thresholds = getThresholds();

    // Determine risk level from score (using configurable thresholds)
    if (score > thresholds.getHighMax()) {
      return RiskLevel.CRITICAL;
    } else if (score > thresholds.getMediumMax()) {
      return RiskLevel.HIGH;
    } else if (score > thresholds.getLowMax()) {
      return RiskLevel.MEDIUM;
    }
    return RiskLevel.LOW;
  }

  /** Calculate budget risk score (0-100 scale) including individual member analysis. */
  private double calculateBudgetRisk(Pitch pitch, double appetiteUsed, double cycleProgress, double totalHours,
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
      double warningLevel = (appetiteUsed - thresholds.getBudgetWarning())
          / (thresholds.getBudgetOverrun() - thresholds.getBudgetWarning());
      riskScore += 20 + (warningLevel * 30);
    }

    // NEW: Check individual member budget constraints
    // Even if team total looks OK, individual members may be over budget
    if (pitch.getTeam() != null && pitch.getAppetiteDays() != null) {
      var teamBudgetInfo = buildTeamBudgetInfo(pitch, totalHours);
      
      if (teamBudgetInfo.hasOverBudgetMember()) {
        // At least one member is over their individual budget - add significant risk
        riskScore += 40;
        log.debug("Pitch {} has over-budget team member(s), adding 40 to risk score", pitch.getId());
      } else if (teamBudgetInfo.bottleneckMember() != null) {
        // Check if bottleneck member is approaching their limit
        double bottleneckUtilization = teamBudgetInfo.bottleneckMember().getUtilizationPercent();
        if (bottleneckUtilization > 90) {
          riskScore += 25; // Bottleneck member nearly exhausted
          log.debug("Pitch {} bottleneck member at {}% utilization, adding 25 to risk", 
              pitch.getId(), bottleneckUtilization);
        } else if (bottleneckUtilization > 75) {
          riskScore += 10; // Bottleneck member getting close
        }
      }
    }

    return Math.min(riskScore, 100); // Cap at 100
  }

  /** Calculate bugs risk score (0-100 scale). */
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
    long majorSeverityBugs = bugs.stream().filter(b -> b.getSeverity() == BugSeverity.MAJOR
        && b.getStatus() != BugStatus.RESOLVED && b.getStatus() != BugStatus.CLOSED).count();

    if (majorSeverityBugs > thresholds.getMajorBugsHigh() && daysLeft < thresholds.getDaysWarning()) {
      riskScore += 30; // Many major-severity bugs near deadline
    } else if (majorSeverityBugs > thresholds.getMajorBugsThreshold()) {
      riskScore += 15; // Several major-severity bugs
    }

    // Count all open bugs
    long openBugs = bugs.stream()
        .filter(b -> b.getStatus() != BugStatus.RESOLVED && b.getStatus() != BugStatus.CLOSED).count();

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
    long recentBugs = bugs.stream().filter(b -> b.getCreatedAt().isAfter(threeDaysAgo)).count();

    if (recentBugs > thresholds.getRecentBugInflux() && pitch.getStatus() == PitchStatus.TESTING) {
      riskScore += 20; // Many bugs found in testing recently
    }

    // Bug resolution rate analysis
    long resolvedBugs = bugs.stream()
        .filter(b -> b.getStatus() == BugStatus.RESOLVED || b.getStatus() == BugStatus.CLOSED).count();

    double bugResolutionRate = bugs.size() > 0 ? (double) resolvedBugs / bugs.size() * 100 : 100;

    if (bugResolutionRate < thresholds.getBugResolutionRateMin() && openBugs > thresholds.getMajorBugsThreshold()
        && daysLeft < thresholds.getDaysWarning()) {
      riskScore += 20; // Low resolution rate with many open bugs near deadline
    }

    return Math.min(riskScore, 100); // Cap at 100
  }

  /** Calculate scope risk score (0-100 scale) based on hill chart progress. */
  private double calculateScopeRisk(Pitch pitch, double cycleProgress,
      com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.RiskThresholds thresholds) {
    double riskScore = 0;

    List<HillChartPoint> scopes = hillChartPointRepository.findByPitchId(pitch.getId());
    if (!scopes.isEmpty()) {
      double avgPosition = scopes.stream().mapToInt(HillChartPoint::getPosition).average().orElse(0);

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
      long stagnantScopes = scopes.stream().filter(s -> s.getPosition() < thresholds.getScopeUphillMax()
          && s.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(thresholds.getScopeStagnationDays())))
          .count();

      if (stagnantScopes > 0 && cycleProgress > 40) {
        riskScore += 25 * (int) Math.min(stagnantScopes, 3); // Up to 75 points
      }

      // Check for scopes stuck at the peak
      long peakStuckScopes = scopes.stream()
          .filter(s -> s.getPosition() >= thresholds.getScopePeakMin()
              && s.getPosition() <= thresholds.getScopePeakMax()
              && s.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(thresholds.getPeakStuckDays())))
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
   * Calculate risk trend based on recent changes. Analyzes if conditions are
   * improving, stable, or worsening.
   */
  private String calculateRiskTrend(Pitch pitch, double appetiteUsed, int daysLeft) {
    var thresholds = getThresholds();
    int trendScore = 0;

    // 1. Check if appetite usage is accelerating
    // Compare recent work rate vs overall average
    LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
    List<WorkLog> allLogs = workLogRepository.findByPitchId(pitch.getId());
    List<WorkLog> recentLogs = allLogs.stream().filter(log -> log.getDate().isAfter(threeDaysAgo))
        .collect(Collectors.toList());

    double recentHours = recentLogs.stream()
        .mapToDouble(log -> log.getHoursSpent() != null ? log.getHoursSpent().doubleValue() : 0.0).sum();

    // If spending is accelerating and already over budget, that's worsening
    if (appetiteUsed > thresholds.getAppetiteHighUsage() && recentHours > thresholds.getRecentWorkHighHours()) {
      trendScore -= 2; // Worsening
    }

    // 2. Check hill chart movement
    List<HillChartPoint> scopes = hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(pitch.getId());
    if (!scopes.isEmpty()) {
      // Check if any scopes moved recently
      LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(thresholds.getNoProgressDays());
      boolean hasRecentProgress = scopes.stream().anyMatch(s -> s.getUpdatedAt().isAfter(sevenDaysAgo));

      if (!hasRecentProgress && daysLeft < thresholds.getDaysConcern()) {
        trendScore -= 1; // No progress recently
      } else if (hasRecentProgress) {
        trendScore += 1; // Making progress
      }
    }

    // 3. Check bug trend
    LocalDateTime threeDaysAgoTime = LocalDateTime.now().minusDays(3);
    List<BugReport> recentBugs = bugReportRepository.findByPitchId(pitch.getId()).stream()
        .filter(b -> b.getCreatedAt().isAfter(threeDaysAgoTime)).collect(Collectors.toList());

    long recentCriticalBugs = recentBugs.stream()
        .filter(b -> b.getSeverity() == BugSeverity.CRITICAL || b.getSeverity() == BugSeverity.BLOCKER).count();

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
    boolean hasQAActivity = meetings.stream().anyMatch(m -> {
      String type = m.getType() != null ? m.getType().trim().toUpperCase() : "";
      return "DEMO".equals(type)
          || (m.getNotes() != null && m.getNotes().toLowerCase().contains("qa"));
    });

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
        .filter(b -> b.getStatus() != BugStatus.RESOLVED && b.getStatus() != BugStatus.CLOSED).count();

    switch (status) {
      case PENDING :
        if (daysLeft < thresholds.getDaysWarning()) {
          summary.append("Not started - Only ").append(daysLeft).append(" days left");
        } else {
          summary.append("Not started");
        }
        break;
      case STARTED :
        summary.append("Just kicked off");
        break;
      case SHAPED :
        summary.append("Shaped and ready");
        break;
      case IN_PROGRESS :
        if (appetiteUsed > thresholds.getBudgetCritical()) {
          summary.append("Significantly over budget (").append(String.format("%.0f", appetiteUsed))
              .append("%)");
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
      case TESTING :
        if (criticalBugs > 0) {
          summary.append("In QA - ").append(criticalBugs).append(" critical bug");
          if (criticalBugs > 1)
            summary.append("s");
        } else if (openBugs > thresholds.getOpenBugsModerate()) {
          summary.append("In QA - ").append(openBugs).append(" open bugs");
        } else {
          summary.append("In QA - looking good");
        }
        break;
      case DONE :
        summary.append("Completed");
        break;
      case COOLDOWN :
        summary.append("In cooldown");
        break;
      case CANCELLED :
        summary.append("Cancelled");
        break;
      default :
        break;
    }

    // Add QA note if relevant
    if ("NOT_STARTED".equals(qaStatus) && status == PitchStatus.IN_PROGRESS && appetiteUsed > 60) {
      summary.append(" - QA not started");
    }

    // Add time pressure note
    if (daysLeft <= thresholds.getDaysUrgent() && daysLeft > 0 && status != PitchStatus.DONE
        && status != PitchStatus.CANCELLED) {
      summary.append(" - ").append(daysLeft).append(" days remaining");
    } else if (daysLeft == 0 && status != PitchStatus.DONE) {
      summary.append(" - DUE TODAY");
    }

    return summary.toString();
  }

  private String getRiskColor(RiskLevel level) {
    if (level == null)
      return "#9e9e9e"; // grey
    switch (level) {
      case LOW :
        return "#4caf50"; // green
      case MEDIUM :
        return "#ff9800"; // orange
      case HIGH :
        return "#f44336"; // red
      case CRITICAL :
        return "#d32f2f"; // dark red
      default :
        return "#9e9e9e"; // grey
    }
  }

  // ========== STAGNATION DETECTION METHODS ==========

  /**
   * Detect stagnating pitches in a cycle.
   * Returns a list of pitches that show signs of stagnation based on:
   * - Hill chart movement (or lack thereof)
   * - Work log activity
   * - Status changes
   * - Budget vs progress mismatches
   */
  public List<com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO> detectStagnatingPitches(Long cycleId) {
    List<com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO> alerts = new java.util.ArrayList<>();
    
    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new RuntimeException("Cycle not found: " + cycleId));
    
    List<Pitch> pitches = pitchRepository.findByCycleIdNotDeleted(cycleId);
    var thresholds = getThresholds();
    
    long totalDays = ChronoUnit.DAYS.between(cycle.getStartDate(), cycle.getEndDate());
    long daysElapsed = ChronoUnit.DAYS.between(cycle.getStartDate(), LocalDate.now());
    double cycleProgress = totalDays > 0 ? (daysElapsed * 100.0 / totalDays) : 0;
    
    for (Pitch pitch : pitches) {
      // Skip completed or cancelled pitches
      if (pitch.getStatus() == PitchStatus.DONE || pitch.getStatus() == PitchStatus.CANCELLED) {
        continue;
      }
      
      List<HillChartPoint> scopes = hillChartPointRepository.findByPitchId(pitch.getId());
      List<WorkLog> workLogs = workLogRepository.findByPitchId(pitch.getId());
      
      // Calculate average hill chart position
      double avgPosition = scopes.isEmpty() ? 0 : 
          scopes.stream().mapToDouble(HillChartPoint::getPosition).average().orElse(0);
      
      // Find last activity date
      LocalDateTime lastActivity = findLastActivityDate(pitch, scopes, workLogs);
      int daysSinceActivity = (int) ChronoUnit.DAYS.between(
          lastActivity.toLocalDate(), LocalDate.now());
      
      // Check for various stagnation types
      var alert = detectStagnationType(pitch, cycle, scopes, daysSinceActivity, 
          avgPosition, cycleProgress, thresholds, lastActivity);
      
      if (alert != null) {
        alerts.add(alert);
      }
    }
    
    // Sort by severity (CRITICAL first) then by days since activity
    alerts.sort((a, b) -> {
      int severityCompare = b.getSeverity().compareTo(a.getSeverity());
      if (severityCompare != 0) return severityCompare;
      return Integer.compare(b.getDaysSinceActivity(), a.getDaysSinceActivity());
    });
    
    log.info("Detected {} stagnating pitches in cycle {}", alerts.size(), cycleId);
    return alerts;
  }

  /**
   * Detect all stagnating pitches across active cycles.
   */
  public List<com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO> detectAllStagnatingPitches() {
    List<com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO> allAlerts = new java.util.ArrayList<>();
    
    // Get all active cycles (in BUILD phase)
    List<Cycle> activeCycles = cycleRepository.findAll().stream()
        .filter(c -> c.getPhase() == com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase.SHAPING_BUILDING)
        .collect(Collectors.toList());
    
    for (Cycle cycle : activeCycles) {
      allAlerts.addAll(detectStagnatingPitches(cycle.getId()));
    }
    
    return allAlerts;
  }

  private LocalDateTime findLastActivityDate(Pitch pitch, List<HillChartPoint> scopes, 
      List<WorkLog> workLogs) {
    LocalDateTime lastActivity = pitch.getUpdatedAt() != null ? pitch.getUpdatedAt() : pitch.getCreatedAt();
    
    // Check hill chart updates
    for (HillChartPoint scope : scopes) {
      if (scope.getUpdatedAt() != null && scope.getUpdatedAt().isAfter(lastActivity)) {
        lastActivity = scope.getUpdatedAt();
      }
    }
    
    // Check work log dates - find most recent
    for (WorkLog workLog : workLogs) {
      if (workLog.getDate() != null) {
        LocalDateTime workLogDate = workLog.getDate().atStartOfDay();
        if (workLogDate.isAfter(lastActivity)) {
          lastActivity = workLogDate;
        }
      }
    }
    
    return lastActivity;
  }

  private com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO detectStagnationType(
      Pitch pitch, Cycle cycle, List<HillChartPoint> scopes, int daysSinceActivity,
      double avgPosition, double cycleProgress,
      com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO.RiskThresholds thresholds,
      LocalDateTime lastActivity) {
    
    List<String> stagnantScopeNames = new java.util.ArrayList<>();
    List<String> recommendations = new java.util.ArrayList<>();
    com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO.StagnationType type = null;
    String description = null;
    
    // Check for stagnant scopes (not moved in threshold days)
    int stagnationDays = thresholds.getScopeStagnationDays();
    LocalDateTime stagnationThreshold = LocalDateTime.now().minusDays(stagnationDays);
    
    for (HillChartPoint scope : scopes) {
      if (scope.getPosition() < thresholds.getScopeUphillMax() 
          && scope.getUpdatedAt() != null 
          && scope.getUpdatedAt().isBefore(stagnationThreshold)) {
        stagnantScopeNames.add(scope.getScope());
      }
    }
    
    // Check for scopes stuck at peak (decision point)
    List<HillChartPoint> peakStuckScopes = scopes.stream()
        .filter(s -> s.getPosition() >= thresholds.getScopePeakMin() 
            && s.getPosition() <= thresholds.getScopePeakMax()
            && s.getUpdatedAt() != null
            && s.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(thresholds.getPeakStuckDays())))
        .collect(Collectors.toList());
    
    // Determine stagnation type and severity
    if (!stagnantScopeNames.isEmpty() && !peakStuckScopes.isEmpty()) {
      type = com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO.StagnationType.COMPOUND_STAGNATION;
      description = String.format("%d scopes stalled, %d stuck at decision point", 
          stagnantScopeNames.size(), peakStuckScopes.size());
      recommendations.add("Break down complex work into smaller tasks");
      recommendations.add("Schedule a team sync to discuss blockers");
      recommendations.add("Consider descoping or simplifying problem areas");
    } else if (!peakStuckScopes.isEmpty()) {
      type = com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO.StagnationType.STUCK_AT_PEAK;
      description = String.format("%d scopes stuck at the hill peak (decision point)", peakStuckScopes.size());
      stagnantScopeNames.addAll(peakStuckScopes.stream().map(HillChartPoint::getScope).collect(Collectors.toList()));
      recommendations.add("Review understanding - are requirements clear?");
      recommendations.add("Make a decision on uncertain areas to move downhill");
    } else if (!stagnantScopeNames.isEmpty()) {
      type = com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO.StagnationType.HILL_CHART_STALLED;
      description = String.format("%d scopes haven't moved in %d+ days", stagnantScopeNames.size(), stagnationDays);
      recommendations.add("Update hill chart to reflect current understanding");
      recommendations.add("If blocked, flag the issue in standup");
    } else if (daysSinceActivity > 7 && cycleProgress > 25) {
      type = com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO.StagnationType.NO_RECENT_WORK;
      description = String.format("No activity recorded for %d days", daysSinceActivity);
      recommendations.add("Log recent work to track progress");
      recommendations.add("If deprioritized, update status accordingly");
    }
    
    // Only create alert if we detected a stagnation type
    if (type == null) {
      return null;
    }
    
    var severity = com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO
        .calculateSeverity(daysSinceActivity, avgPosition, cycleProgress);
    
    return com.github.farzadsedaghatbin.shipflow.dto.health.StagnationAlertDTO.builder()
        .pitchId(pitch.getId())
        .pitchTitle(pitch.getTitle())
        .cycleId(cycle.getId())
        .cycleName(cycle.getName())
        .stagnationType(type)
        .description(description)
        .daysSinceActivity(daysSinceActivity)
        .hillChartPosition(avgPosition)
        .lastActivityAt(lastActivity)
        .stagnantScopes(stagnantScopeNames)
        .recommendations(recommendations)
        .severity(severity)
        .build();
  }
}
