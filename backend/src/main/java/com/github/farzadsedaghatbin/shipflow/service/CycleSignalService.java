package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.config.AIConfig;
import com.github.farzadsedaghatbin.shipflow.dto.signal.*;
import com.github.farzadsedaghatbin.shipflow.dto.signal.AppetiteAccuracySignalDTO.CycleAccuracyData;
import com.github.farzadsedaghatbin.shipflow.dto.signal.AppetiteAccuracySignalDTO.TrendDirection;
import com.github.farzadsedaghatbin.shipflow.dto.signal.RetroFollowThroughSignalDTO.RecurringTheme;
import com.github.farzadsedaghatbin.shipflow.dto.signal.RetroFollowThroughSignalDTO.RetroFollowThroughData;
import com.github.farzadsedaghatbin.shipflow.dto.signal.RetroFollowThroughSignalDTO.UnactedActionItem;
import com.github.farzadsedaghatbin.shipflow.dto.signal.ShapingPatternSignalDTO.ShapingPatternPitch;
import com.github.farzadsedaghatbin.shipflow.dto.signal.ShapingPatternSignalDTO.ShapingQuality;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for calculating cycle signals - insights derived from historical data.
 * Implements the "Insight, Not Metrics" philosophy of v0.5.
 * 
 * Signals are computed insights that help teams learn and improve,
 * rather than vanity metrics that just track activity.
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class CycleSignalService {

  private static final double HOURS_PER_DAY = 8.0;
  private static final double DEFAULT_TOLERANCE_PERCENT = 15.0;
  private static final int MIN_CYCLES_FOR_TREND = 3;

  private final CycleRepository cycleRepository;
  private final PitchRepository pitchRepository;
  private final WorkLogRepository workLogRepository;
  private final RetroRepository retroRepository;
  private final RetroItemRepository retroItemRepository;
  private final PitchRiskHistoryRepository riskHistoryRepository;
  private final ProjectRepository projectRepository;
  private final AIConfig aiConfig;
  private final ChatLanguageModel chatLanguageModel;

  @Autowired
  public CycleSignalService(
      CycleRepository cycleRepository,
      PitchRepository pitchRepository,
      WorkLogRepository workLogRepository,
      RetroRepository retroRepository,
      RetroItemRepository retroItemRepository,
      PitchRiskHistoryRepository riskHistoryRepository,
      ProjectRepository projectRepository,
      AIConfig aiConfig,
      @Autowired(required = false) ChatLanguageModel chatLanguageModel) {
    this.cycleRepository = cycleRepository;
    this.pitchRepository = pitchRepository;
    this.workLogRepository = workLogRepository;
    this.retroRepository = retroRepository;
    this.retroItemRepository = retroItemRepository;
    this.riskHistoryRepository = riskHistoryRepository;
    this.projectRepository = projectRepository;
    this.aiConfig = aiConfig;
    this.chatLanguageModel = chatLanguageModel;
  }

  /**
   * Get all signals for a project across recent cycles.
   */
  public CycleSignalsDTO getProjectSignals(Long projectId, int cycleCount) {
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

    List<Cycle> cycles = cycleRepository.findByProjectIdOrderByStartDateDesc(projectId)
        .stream()
        .limit(cycleCount)
        .collect(Collectors.toList());

    if (cycles.isEmpty()) {
      return buildEmptySignals(projectId, project.getName());
    }

    AppetiteAccuracySignalDTO appetiteAccuracy = calculateAppetiteAccuracySignal(cycles);
    ShapingPatternSignalDTO shapingPattern = calculateShapingPatternSignal(cycles);
    RiskCorrelationSignalDTO riskCorrelation = calculateRiskCorrelationSignal(cycles);
    RetroFollowThroughSignalDTO retroFollowThrough = calculateRetroFollowThroughSignal(cycles);

    List<String> keyInsights = generateKeyInsights(appetiteAccuracy, shapingPattern, riskCorrelation, retroFollowThrough);
    List<String> topRecommendations = generateTopRecommendations(appetiteAccuracy, shapingPattern, riskCorrelation, retroFollowThrough);
    int healthScore = calculateOverallHealthScore(appetiteAccuracy, shapingPattern, riskCorrelation, retroFollowThrough);

    return CycleSignalsDTO.builder()
        .projectId(projectId)
        .projectName(project.getName())
        .cyclesAnalyzed(cycles.size())
        .appetiteAccuracy(appetiteAccuracy)
        .shapingPattern(shapingPattern)
        .riskCorrelation(riskCorrelation)
        .retroFollowThrough(retroFollowThrough)
        .keyInsights(keyInsights)
        .topRecommendations(topRecommendations)
        .overallHealthScore(healthScore)
        .analyzedAt(LocalDateTime.now())
        .aiEnhanced(aiConfig.isAiRiskAnalysisEnabled() && chatLanguageModel != null)
        .build();
  }

  /**
   * Get signals for a specific cycle.
   */
  public CycleSignalsDTO getCycleSignals(Long cycleId) {
    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found: " + cycleId));

    // Get this cycle plus previous cycles for trend context
    List<Cycle> cycles = cycleRepository.findByProjectIdOrderByStartDateDesc(cycle.getProject().getId())
        .stream()
        .filter(c -> !c.getStartDate().isAfter(cycle.getStartDate()))
        .limit(6)
        .collect(Collectors.toList());

    AppetiteAccuracySignalDTO appetiteAccuracy = calculateAppetiteAccuracySignal(cycles);
    ShapingPatternSignalDTO shapingPattern = calculateShapingPatternSignal(List.of(cycle));
    RiskCorrelationSignalDTO riskCorrelation = calculateRiskCorrelationSignal(List.of(cycle));
    RetroFollowThroughSignalDTO retroFollowThrough = calculateRetroFollowThroughSignal(List.of(cycle));

    List<String> keyInsights = generateKeyInsights(appetiteAccuracy, shapingPattern, riskCorrelation, retroFollowThrough);
    List<String> topRecommendations = generateTopRecommendations(appetiteAccuracy, shapingPattern, riskCorrelation, retroFollowThrough);
    int healthScore = calculateOverallHealthScore(appetiteAccuracy, shapingPattern, riskCorrelation, retroFollowThrough);

    return CycleSignalsDTO.builder()
        .projectId(cycle.getProject().getId())
        .projectName(cycle.getProject().getName())
        .cycleId(cycleId)
        .cycleName(cycle.getName())
        .cyclesAnalyzed(cycles.size())
        .appetiteAccuracy(appetiteAccuracy)
        .shapingPattern(shapingPattern)
        .riskCorrelation(riskCorrelation)
        .retroFollowThrough(retroFollowThrough)
        .keyInsights(keyInsights)
        .topRecommendations(topRecommendations)
        .overallHealthScore(healthScore)
        .analyzedAt(LocalDateTime.now())
        .aiEnhanced(aiConfig.isAiRiskAnalysisEnabled() && chatLanguageModel != null)
        .build();
  }

  /**
   * Calculate appetite accuracy signal across cycles.
   */
  public AppetiteAccuracySignalDTO calculateAppetiteAccuracySignal(List<Cycle> cycles) {
    if (cycles.isEmpty()) {
      return AppetiteAccuracySignalDTO.builder()
          .trend(TrendDirection.INSUFFICIENT_DATA)
          .cyclesAnalyzed(0)
          .interpretation("Not enough data to analyze appetite accuracy.")
          .recommendations(List.of("Complete at least one cycle to start tracking accuracy."))
          .build();
    }

    List<CycleAccuracyData> cycleData = new ArrayList<>();
    List<Double> variancePercents = new ArrayList<>();

    for (Cycle cycle : cycles) {
      // Only include completed pitches (DONE or CANCELLED) to avoid partial work log data
      List<Pitch> pitches = pitchRepository.findByCycleIdNotDeleted(cycle.getId())
          .stream()
          .filter(p -> p.getStatus() == PitchStatus.DONE || p.getStatus() == PitchStatus.CANCELLED)
          .collect(Collectors.toList());
      if (pitches.isEmpty()) continue;

      double totalAppetite = pitches.stream()
          .mapToDouble(p -> p.getAppetiteDays() * HOURS_PER_DAY)
          .sum();

      // Calculate total actual hours from work logs per pitch
      double totalActual = 0.0;
      Map<Long, Double> pitchHoursMap = batchLoadWorkHoursForCycle(cycle.getId());
      for (Pitch pitch : pitches) {
        totalActual += pitchHoursMap.getOrDefault(pitch.getId(), 0.0);
      }

      double variance = totalActual - totalAppetite;
      double variancePercent = totalAppetite > 0 ? (variance / totalAppetite) * 100 : 0;

      int overBudget = 0;
      int underBudget = 0;
      for (Pitch pitch : pitches) {
        Double pitchActual = pitchHoursMap.getOrDefault(pitch.getId(), 0.0);
        double pitchAppetite = pitch.getAppetiteDays() * HOURS_PER_DAY;
        if (pitchActual > pitchAppetite * 1.15) overBudget++;
        else if (pitchActual < pitchAppetite * 0.85) underBudget++;
      }

      cycleData.add(CycleAccuracyData.builder()
          .cycleId(cycle.getId())
          .cycleName(cycle.getName())
          .startDate(cycle.getStartDate())
          .endDate(cycle.getEndDate())
          .totalAppetiteHours(totalAppetite)
          .totalActualHours(totalActual)
          .varianceHours(variance)
          .variancePercent(variancePercent)
          .pitchCount(pitches.size())
          .overBudgetCount(overBudget)
          .underBudgetCount(underBudget)
          .build());

      variancePercents.add(variancePercent);
    }

    if (cycleData.isEmpty()) {
      return AppetiteAccuracySignalDTO.builder()
          .trend(TrendDirection.INSUFFICIENT_DATA)
          .averageVariancePercent(0.0)
          .varianceStdDev(0.0)
          .cyclesAnalyzed(0)
          .cycleData(List.of())
          .interpretation("No completed work found in analyzed cycles.")
          .recommendations(List.of())
          .build();
    }

    // Calculate statistics (use absolute values for std dev but keep signed average)
    double avgVariance = variancePercents.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    List<Double> absVariances = variancePercents.stream()
        .filter(Objects::nonNull)
        .map(v -> Math.abs(v))
        .collect(Collectors.toList());
    double avgAbsVariance = absVariances.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    double stdDev = calculateStdDev(absVariances, avgAbsVariance);
    // Reverse list before trend calculation - cycles loaded DESC but regression needs oldest-to-newest
    List<Double> reversedVariances = new ArrayList<>(variancePercents);
    Collections.reverse(reversedVariances);
    TrendDirection trend = calculateTrend(reversedVariances);

    // Generate interpretation and recommendations (use absolute variance for accuracy grading)
    String interpretation = generateAppetiteInterpretation(avgAbsVariance, trend, cycleData.size());
    List<String> recommendations = generateAppetiteRecommendations(avgAbsVariance, trend, cycleData);

    return AppetiteAccuracySignalDTO.builder()
        .trend(trend)
        .averageVariancePercent(avgVariance)
        .varianceStdDev(stdDev)
        .cyclesAnalyzed(cycleData.size())
        .cycleData(cycleData)
        .interpretation(interpretation)
        .recommendations(recommendations)
        .build();
  }

  /**
   * Calculate shaping pattern signal - identify over/under shaping.
   */
  public ShapingPatternSignalDTO calculateShapingPatternSignal(List<Cycle> cycles) {
    List<ShapingPatternPitch> overShaped = new ArrayList<>();
    List<ShapingPatternPitch> underShaped = new ArrayList<>();
    int accurateCount = 0;
    int totalPitches = 0;

    for (Cycle cycle : cycles) {
      List<Pitch> pitches = pitchRepository.findByCycleIdNotDeleted(cycle.getId())
          .stream()
          .filter(p -> p.getStatus() == PitchStatus.DONE || p.getStatus() == PitchStatus.CANCELLED)
          .collect(Collectors.toList());

      // Batch load work hours for all pitches in this cycle (avoid N+1 query)
      Map<Long, Double> workHoursMap = batchLoadWorkHoursForCycle(cycle.getId());

      for (Pitch pitch : pitches) {
        Double actualHours = workHoursMap.getOrDefault(pitch.getId(), 0.0);
        double appetiteHours = pitch.getAppetiteDays() * HOURS_PER_DAY;
        double variancePercent = appetiteHours > 0 ? ((actualHours - appetiteHours) / appetiteHours) * 100 : 0;

        totalPitches++;

        ShapingPatternPitch patternPitch = ShapingPatternPitch.builder()
            .pitchId(pitch.getId())
            .pitchTitle(pitch.getTitle())
            .cycleId(cycle.getId())
            .cycleName(cycle.getName())
            .appetiteDays(pitch.getAppetiteDays())
            .actualHours(actualHours)
            .variancePercent(variancePercent)
            .status(pitch.getStatus().name())
            .build();

        if (variancePercent < -DEFAULT_TOLERANCE_PERCENT) {
          // Under budget = over-shaped (more detail than needed)
          overShaped.add(patternPitch);
        } else if (variancePercent > DEFAULT_TOLERANCE_PERCENT) {
          // Over budget = under-shaped (not enough detail)
          underShaped.add(patternPitch);
        } else {
          accurateCount++;
        }
      }
    }

    ShapingQuality quality = determineShapingQuality(totalPitches, accurateCount, overShaped.size(), underShaped.size());
    String interpretation = generateShapingInterpretation(quality, overShaped.size(), underShaped.size(), totalPitches);
    List<String> recommendations = generateShapingRecommendations(quality, overShaped, underShaped);

    return ShapingPatternSignalDTO.builder()
        .overallQuality(quality)
        .overShapedCount(overShaped.size())
        .underShapedCount(underShaped.size())
        .accuratelyShapedCount(accurateCount)
        .totalPitchesAnalyzed(totalPitches)
        .tolerancePercent(DEFAULT_TOLERANCE_PERCENT)
        .overShapedPitches(overShaped.stream().limit(5).collect(Collectors.toList()))
        .underShapedPitches(underShaped.stream().limit(5).collect(Collectors.toList()))
        .interpretation(interpretation)
        .recommendations(recommendations)
        .build();
  }

  /**
   * Calculate risk-outcome correlation signal.
   */
  public RiskCorrelationSignalDTO calculateRiskCorrelationSignal(List<Cycle> cycles) {
    int truePositives = 0;  // Flagged high risk, had problems
    int falseAlarms = 0;    // Flagged high risk, completed fine
    int missedRisks = 0;    // Not flagged, had problems
    int trueNegatives = 0;  // Not flagged, completed fine

    List<RiskCorrelationSignalDTO.MissedRiskPitch> missedRiskDetails = new ArrayList<>();
    List<RiskCorrelationSignalDTO.FalseAlarmPitch> falseAlarmDetails = new ArrayList<>();

    // Batch prefetch all risk histories for all pitches (avoid N+1 query)
    List<Long> allPitchIds = new ArrayList<>();
    Map<Long, List<Pitch>> pitchesByCycle = new HashMap<>();
    Map<Long, Double> workHoursMap = new HashMap<>();
    
    for (Cycle cycle : cycles) {
      List<Pitch> pitches = pitchRepository.findByCycleIdNotDeleted(cycle.getId());
      pitchesByCycle.put(cycle.getId(), pitches);
      pitches.forEach(p -> allPitchIds.add(p.getId()));
      
      // Load work hours for this cycle and merge into the overall map
      Map<Long, Double> cycleHours = batchLoadWorkHoursForCycle(cycle.getId());
      workHoursMap.putAll(cycleHours);
    }

    List<PitchRiskHistory> allHistories = allPitchIds.isEmpty() ? Collections.emptyList() 
        : riskHistoryRepository.findByPitchIdInOrderByRecordedAtDesc(allPitchIds);
    
    // Build map of pitchId -> earliest risk level
    Map<Long, RiskLevel> initialRiskByPitch = new HashMap<>();
    Map<Long, List<PitchRiskHistory>> historiesByPitch = allHistories.stream()
        .collect(Collectors.groupingBy(h -> h.getPitch().getId()));
    
    for (Long pitchId : allPitchIds) {
      List<PitchRiskHistory> histories = historiesByPitch.getOrDefault(pitchId, Collections.emptyList());
      RiskLevel initialRisk = histories.isEmpty() ? RiskLevel.LOW 
          : histories.get(histories.size() - 1).getRiskLevel();
      initialRiskByPitch.put(pitchId, initialRisk);
    }

    for (Cycle cycle : cycles) {
      List<Pitch> pitches = pitchesByCycle.get(cycle.getId());

      for (Pitch pitch : pitches) {
        // Get initial risk from prefetched map
        RiskLevel initialRisk = initialRiskByPitch.getOrDefault(pitch.getId(), RiskLevel.LOW);
        boolean wasHighRisk = initialRisk == RiskLevel.HIGH || initialRisk == RiskLevel.CRITICAL;

        // Determine outcome using preloaded work hours
        boolean hadProblems = pitch.getIsCircuitBreakerTriggered() ||
            pitch.getStatus() == PitchStatus.CANCELLED ||
            isSignificantlyOverBudget(pitch, workHoursMap);

        if (wasHighRisk && hadProblems) {
          truePositives++;
        } else if (wasHighRisk && !hadProblems) {
          falseAlarms++;
          falseAlarmDetails.add(RiskCorrelationSignalDTO.FalseAlarmPitch.builder()
              .pitchId(pitch.getId())
              .pitchTitle(pitch.getTitle())
              .cycleName(cycle.getName())
              .flaggedRiskLevel(initialRisk.name())
              .actualOutcome(pitch.getStatus().name())
              .build());
        } else if (!wasHighRisk && hadProblems) {
          missedRisks++;
          missedRiskDetails.add(RiskCorrelationSignalDTO.MissedRiskPitch.builder()
              .pitchId(pitch.getId())
              .pitchTitle(pitch.getTitle())
              .cycleName(cycle.getName())
              .initialRiskLevel(initialRisk.name())
              .finalOutcome(pitch.getStatus().name())
              .whatWentWrong(determineWhatWentWrong(pitch, workHoursMap))
              .build());
        } else {
          trueNegatives++;
        }
      }
    }

    int total = truePositives + falseAlarms + missedRisks + trueNegatives;
    double accuracy = total > 0 ? ((double) (truePositives + trueNegatives) / total) * 100 : 0;

    String interpretation = generateRiskCorrelationInterpretation(accuracy, missedRisks, falseAlarms, total);
    List<String> recommendations = generateRiskCorrelationRecommendations(missedRisks, falseAlarms, missedRiskDetails);

    return RiskCorrelationSignalDTO.builder()
        .predictiveAccuracyPercent(accuracy)
        .pitchesAnalyzed(total)
        .truePositives(truePositives)
        .falseAlarms(falseAlarms)
        .missedRisks(missedRisks)
        .trueNegatives(trueNegatives)
        .missedRiskDetails(missedRiskDetails.stream().limit(5).collect(Collectors.toList()))
        .falseAlarmDetails(falseAlarmDetails.stream().limit(5).collect(Collectors.toList()))
        .interpretation(interpretation)
        .recommendations(recommendations)
        .mostPredictiveFactors(Collections.emptyList())
        .leastPredictiveFactors(Collections.emptyList())
        .build();
  }

  /**
   * Calculate retrospective follow-through signal.
   */
  public RetroFollowThroughSignalDTO calculateRetroFollowThroughSignal(List<Cycle> cycles) {
    List<RetroFollowThroughData> retroData = new ArrayList<>();
    List<UnactedActionItem> unactedItems = new ArrayList<>();
    int totalActions = 0;
    int actedOnCount = 0;

    for (Cycle cycle : cycles) {
      List<Retrospective> retros = retroRepository.findByCycleIdOrderByCreatedAtDesc(cycle.getId());

      for (Retrospective retro : retros) {
        List<RetroItem> actionItems = retroItemRepository.findByRetrospectiveIdAndColumnType(
            retro.getId(), RetroColumnType.ACTIONS);

        int retroActedOn = 0;
        for (RetroItem item : actionItems) {
          totalActions++;
          if (Boolean.TRUE.equals(item.getActedOn())) {
            actedOnCount++;
            retroActedOn++;
          } else {
            long daysSinceCreated = ChronoUnit.DAYS.between(item.getCreatedAt(), LocalDateTime.now());
            unactedItems.add(UnactedActionItem.builder()
                .retroItemId(item.getId())
                .content(item.getContent())
                .retrospectiveName(retro.getTitle())
                .cycleName(cycle.getName())
                .voteCount(item.getVoteCount())
                .daysSinceCreated((int) daysSinceCreated)
                .tags(item.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
                .build());
          }
        }

        if (!actionItems.isEmpty()) {
          retroData.add(RetroFollowThroughData.builder()
              .retrospectiveId(retro.getId())
              .retrospectiveName(retro.getTitle())
              .cycleId(cycle.getId())
              .cycleName(cycle.getName())
              .totalActions(actionItems.size())
              .actedOnCount(retroActedOn)
              .followThroughRate(actionItems.isEmpty() ? 0 : (double) retroActedOn / actionItems.size() * 100)
              .build());
        }
      }
    }

    double followThroughRate = totalActions > 0 ? (double) actedOnCount / totalActions * 100 : 0;

    // Sort unacted items by vote count (priority) descending
    unactedItems.sort((a, b) -> b.getVoteCount().compareTo(a.getVoteCount()));

    // Identify recurring themes
    List<RecurringTheme> recurringThemes = identifyRecurringThemes(unactedItems);

    TrendDirection trend = calculateFollowThroughTrend(retroData);
    String interpretation = generateFollowThroughInterpretation(followThroughRate, totalActions, trend);
    List<String> recommendations = generateFollowThroughRecommendations(followThroughRate, unactedItems, recurringThemes);

    return RetroFollowThroughSignalDTO.builder()
        .followThroughRatePercent(followThroughRate)
        .totalActionItems(totalActions)
        .actedOnCount(actedOnCount)
        .pendingCount(totalActions - actedOnCount)
        .retrospectivesAnalyzed(retroData.size())
        .trend(trend)
        .highPriorityUnacted(unactedItems.stream().limit(5).collect(Collectors.toList()))
        .recurringThemes(recurringThemes)
        .retroData(retroData)
        .interpretation(interpretation)
        .recommendations(recommendations)
        .build();
  }

  // ============================================
  // Helper methods
  // ============================================

  private CycleSignalsDTO buildEmptySignals(Long projectId, String projectName) {
    return CycleSignalsDTO.builder()
        .projectId(projectId)
        .projectName(projectName)
        .cyclesAnalyzed(0)
        .keyInsights(List.of("No cycles found for analysis."))
        .topRecommendations(List.of("Create your first cycle to start generating insights."))
        .overallHealthScore(0)
        .analyzedAt(LocalDateTime.now())
        .aiEnhanced(false)
        .build();
  }

  private double calculateStdDev(List<Double> values, double mean) {
    if (values.size() < 2) return 0;
    double sumSquaredDiff = values.stream()
        .mapToDouble(v -> Math.pow(v - mean, 2))
        .sum();
    return Math.sqrt(sumSquaredDiff / (values.size() - 1));
  }

  private TrendDirection calculateTrend(List<Double> values) {
    if (values.size() < MIN_CYCLES_FOR_TREND) {
      return TrendDirection.INSUFFICIENT_DATA;
    }

    // Simple linear regression to determine trend
    int n = values.size();
    double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
    for (int i = 0; i < n; i++) {
      sumX += i;
      sumY += values.get(i);
      sumXY += i * values.get(i);
      sumX2 += i * i;
    }
    double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);

    // Negative slope = improving (variance decreasing)
    // Positive slope = declining (variance increasing)
    if (Math.abs(slope) < 2) return TrendDirection.STABLE;
    return slope < 0 ? TrendDirection.IMPROVING : TrendDirection.DECLINING;
  }

  private TrendDirection calculateFollowThroughTrend(List<RetroFollowThroughData> retroData) {
    if (retroData.size() < MIN_CYCLES_FOR_TREND) {
      return TrendDirection.INSUFFICIENT_DATA;
    }
    List<Double> rates = retroData.stream()
        .map(RetroFollowThroughData::getFollowThroughRate)
        .collect(Collectors.toList());
    // Reverse so data is ordered oldest-to-newest for regression-based trend calculation
    Collections.reverse(rates);
    return calculateTrend(rates);
  }

  private boolean isSignificantlyOverBudget(Pitch pitch, Map<Long, Double> workHoursMap) {
    Double actual = workHoursMap.get(pitch.getId());
    if (actual == null) return false;
    double appetite = pitch.getAppetiteDays() * HOURS_PER_DAY;
    return appetite > 0 && actual > appetite * 1.3; // 30% over
  }

  private String determineWhatWentWrong(Pitch pitch, Map<Long, Double> workHoursMap) {
    if (pitch.getIsCircuitBreakerTriggered()) {
      return "Circuit breaker triggered: " + pitch.getCircuitBreakerReason();
    }
    if (pitch.getStatus() == PitchStatus.CANCELLED) {
      return "Pitch was cancelled";
    }
    if (isSignificantlyOverBudget(pitch, workHoursMap)) {
      return "Significantly over budget";
    }
    return "Unknown issue";
  }

  private ShapingQuality determineShapingQuality(int total, int accurate, int overShaped, int underShaped) {
    if (total == 0) return ShapingQuality.INSUFFICIENT_DATA;
    double accurateRate = (double) accurate / total;
    if (accurateRate >= 0.8) return ShapingQuality.EXCELLENT;
    if (accurateRate >= 0.6) return ShapingQuality.GOOD;
    if (accurateRate >= 0.4) return ShapingQuality.NEEDS_ATTENTION;
    return ShapingQuality.POOR;
  }

  private List<RecurringTheme> identifyRecurringThemes(List<UnactedActionItem> items) {
    // Group by tags to find recurring themes
    Map<String, List<UnactedActionItem>> tagGroups = new HashMap<>();
    for (UnactedActionItem item : items) {
      for (String tag : item.getTags()) {
        tagGroups.computeIfAbsent(tag, k -> new ArrayList<>()).add(item);
      }
    }

    return tagGroups.entrySet().stream()
        .filter(e -> e.getValue().size() >= 2)
        .map(e -> RecurringTheme.builder()
            .theme(e.getKey())
            .occurrenceCount(e.getValue().size())
            .cyclesSpanned((int) e.getValue().stream()
                .map(UnactedActionItem::getCycleName)
                .distinct()
                .count())
            .hasBeenAddressed(false)
            .relatedTags(List.of(e.getKey()))
            .build())
        .sorted((a, b) -> b.getOccurrenceCount().compareTo(a.getOccurrenceCount()))
        .limit(5)
        .collect(Collectors.toList());
  }

  // ============================================
  // Interpretation generators
  // ============================================

  private String generateAppetiteInterpretation(double avgVariance, TrendDirection trend, int cycles) {
    StringBuilder sb = new StringBuilder();
    if (avgVariance < 10) {
      sb.append("Excellent appetite accuracy! ");
    } else if (avgVariance < 20) {
      sb.append("Good appetite accuracy with room for improvement. ");
    } else if (avgVariance < 35) {
      sb.append("Moderate appetite accuracy - estimates need refinement. ");
    } else {
      sb.append("Appetite estimates are significantly off target. ");
    }

    switch (trend) {
      case IMPROVING:
        sb.append("Trend is improving - teams are getting better at estimating.");
        break;
      case DECLINING:
        sb.append("Trend is declining - estimation accuracy has dropped recently.");
        break;
      case STABLE:
        sb.append("Accuracy has been consistent across recent cycles.");
        break;
      default:
        sb.append("Need more data to determine trend direction.");
    }
    return sb.toString();
  }

  private List<String> generateAppetiteRecommendations(double avgVariance, TrendDirection trend, List<CycleAccuracyData> data) {
    List<String> recommendations = new ArrayList<>();
    if (avgVariance > 25) {
      recommendations.add("Consider breaking large pitches into smaller, more estimable chunks.");
      recommendations.add("Review past estimates vs actuals in betting meetings to calibrate.");
    }
    if (trend == TrendDirection.DECLINING) {
      recommendations.add("Investigate recent changes in team composition or scope definition.");
    }
    
    // Check for consistent pattern
    long overBudgetCycles = data.stream().filter(d -> d.getVariancePercent() > 10).count();
    if (overBudgetCycles > data.size() / 2) {
      recommendations.add("Team consistently runs over budget - consider adding buffer to appetite.");
    }
    
    if (recommendations.isEmpty()) {
      recommendations.add("Keep doing what you're doing - estimation accuracy is solid.");
    }
    return recommendations;
  }

  private String generateShapingInterpretation(ShapingQuality quality, int overShaped, int underShaped, int total) {
    if (total == 0) return "No completed pitches to analyze shaping patterns.";
    
    StringBuilder sb = new StringBuilder();
    switch (quality) {
      case EXCELLENT:
        sb.append("Shaping quality is excellent - pitches are well-scoped.");
        break;
      case GOOD:
        sb.append("Shaping quality is good with some outliers.");
        break;
      case NEEDS_ATTENTION:
        sb.append("Shaping needs attention - significant variance in outcomes.");
        break;
      case POOR:
        sb.append("Shaping quality is poor - most pitches miss their targets.");
        break;
      default:
        sb.append("Insufficient data to assess shaping quality.");
    }

    if (overShaped > underShaped && overShaped > 2) {
      sb.append(" Tendency to over-shape (pitches finish under budget).");
    } else if (underShaped > overShaped && underShaped > 2) {
      sb.append(" Tendency to under-shape (pitches run over budget).");
    }
    return sb.toString();
  }

  private List<String> generateShapingRecommendations(ShapingQuality quality, 
      List<ShapingPatternPitch> overShaped, List<ShapingPatternPitch> underShaped) {
    List<String> recommendations = new ArrayList<>();
    
    if (overShaped.size() > 3) {
      recommendations.add("Review over-shaped pitches - appetite may be too generous for the actual work.");
      recommendations.add("Consider more aggressive scoping to free up capacity for additional work.");
    }
    if (underShaped.size() > 3) {
      recommendations.add("Review under-shaped pitches - identify what was missed in shaping.");
      recommendations.add("Dedicate more time to identifying rabbit holes and risks during shaping.");
    }
    if (quality == ShapingQuality.POOR) {
      recommendations.add("Consider pairing shapers with developers during shaping sessions.");
    }
    
    if (recommendations.isEmpty()) {
      recommendations.add("Shaping process is working well - maintain current practices.");
    }
    return recommendations;
  }

  private String generateRiskCorrelationInterpretation(double accuracy, int missedRisks, int falseAlarms, int total) {
    if (total == 0) return "No pitches analyzed for risk correlation.";
    
    StringBuilder sb = new StringBuilder();
    if (accuracy >= 80) {
      sb.append("Risk prediction is highly accurate. ");
    } else if (accuracy >= 60) {
      sb.append("Risk prediction is moderately accurate. ");
    } else {
      sb.append("Risk prediction needs improvement. ");
    }

    if (missedRisks > 0) {
      sb.append(String.format("%d pitches had problems that weren't flagged early. ", missedRisks));
    }
    if (falseAlarms > 0) {
      sb.append(String.format("%d pitches were flagged as risky but completed fine.", falseAlarms));
    }
    return sb.toString();
  }

  private List<String> generateRiskCorrelationRecommendations(int missedRisks, int falseAlarms,
      List<RiskCorrelationSignalDTO.MissedRiskPitch> missedDetails) {
    List<String> recommendations = new ArrayList<>();
    
    if (missedRisks > 2) {
      recommendations.add("Review missed risks to identify common patterns not being detected.");
      recommendations.add("Consider adding questions about these patterns to risk assessment.");
    }
    if (falseAlarms > 3) {
      recommendations.add("Risk thresholds may be too sensitive - consider calibration.");
    }
    
    if (recommendations.isEmpty()) {
      recommendations.add("Risk detection is working well - continue monitoring.");
    }
    return recommendations;
  }

  private String generateFollowThroughInterpretation(double rate, int total, TrendDirection trend) {
    if (total == 0) return "No action items found in retrospectives.";
    
    StringBuilder sb = new StringBuilder();
    if (rate >= 80) {
      sb.append("Excellent follow-through on retro action items! ");
    } else if (rate >= 60) {
      sb.append("Good follow-through with room for improvement. ");
    } else if (rate >= 40) {
      sb.append("Moderate follow-through - many action items remain unaddressed. ");
    } else {
      sb.append("Low follow-through on action items - retros may not be driving change. ");
    }

    switch (trend) {
      case IMPROVING:
        sb.append("Trend is improving - team is getting better at following through.");
        break;
      case DECLINING:
        sb.append("Trend is declining - fewer items being acted upon recently.");
        break;
      default:
        break;
    }
    return sb.toString();
  }

  private List<String> generateFollowThroughRecommendations(double rate, 
      List<UnactedActionItem> unacted, List<RecurringTheme> themes) {
    List<String> recommendations = new ArrayList<>();
    
    if (rate < 50) {
      recommendations.add("Assign owners to action items with specific due dates.");
      recommendations.add("Review action items at the start of each cycle.");
    }
    
    if (!themes.isEmpty()) {
      recommendations.add("Recurring themes detected - prioritize addressing: " + 
          themes.stream().map(RecurringTheme::getTheme).limit(3).collect(Collectors.joining(", ")));
    }
    
    if (!unacted.isEmpty() && unacted.get(0).getVoteCount() >= 3) {
      recommendations.add("High-vote items are pending - consider dedicated time to address them.");
    }
    
    if (recommendations.isEmpty()) {
      recommendations.add("Keep up the good work on following through with action items.");
    }
    return recommendations;
  }

  private List<String> generateKeyInsights(
      AppetiteAccuracySignalDTO appetite,
      ShapingPatternSignalDTO shaping,
      RiskCorrelationSignalDTO risk,
      RetroFollowThroughSignalDTO retro) {
    
    List<String> insights = new ArrayList<>();
    
    if (appetite != null && appetite.getTrend() == TrendDirection.IMPROVING) {
      insights.add("Appetite estimation accuracy is improving over time.");
    } else if (appetite != null && appetite.getAverageVariancePercent() != null && appetite.getAverageVariancePercent() > 30) {
      insights.add("Appetite estimates are significantly off - averaging " + 
          String.format("%.0f%%", appetite.getAverageVariancePercent()) + " variance.");
    }
    
    if (shaping != null && shaping.getOverallQuality() == ShapingQuality.POOR) {
      insights.add("Shaping quality needs attention - most pitches miss their targets.");
    }
    
    if (risk != null && risk.getMissedRisks() != null && risk.getMissedRisks() > 0) {
      insights.add(risk.getMissedRisks() + " risks were missed that led to problems.");
    }
    
    if (retro != null && retro.getFollowThroughRatePercent() != null && retro.getFollowThroughRatePercent() < 50) {
      insights.add("Less than half of retro action items are being acted upon.");
    }
    
    if (insights.isEmpty()) {
      insights.add("Overall project health is good - no major concerns detected.");
    }
    
    return insights.stream().limit(4).collect(Collectors.toList());
  }

  private List<String> generateTopRecommendations(
      AppetiteAccuracySignalDTO appetite,
      ShapingPatternSignalDTO shaping,
      RiskCorrelationSignalDTO risk,
      RetroFollowThroughSignalDTO retro) {
    
    List<String> all = new ArrayList<>();
    if (appetite != null && appetite.getRecommendations() != null) {
      all.addAll(appetite.getRecommendations().stream().limit(2).collect(Collectors.toList()));
    }
    if (shaping != null && shaping.getRecommendations() != null) {
      all.addAll(shaping.getRecommendations().stream().limit(2).collect(Collectors.toList()));
    }
    if (risk != null && risk.getRecommendations() != null) {
      all.addAll(risk.getRecommendations().stream().limit(1).collect(Collectors.toList()));
    }
    if (retro != null && retro.getRecommendations() != null) {
      all.addAll(retro.getRecommendations().stream().limit(1).collect(Collectors.toList()));
    }
    
    return all.stream().distinct().limit(5).collect(Collectors.toList());
  }

  private int calculateOverallHealthScore(
      AppetiteAccuracySignalDTO appetite,
      ShapingPatternSignalDTO shaping,
      RiskCorrelationSignalDTO risk,
      RetroFollowThroughSignalDTO retro) {
    
    int score = 50; // Base score
    
    // Appetite accuracy contribution (±15 points)
    if (appetite != null && appetite.getAverageVariancePercent() != null) {
      double absVariance = Math.abs(appetite.getAverageVariancePercent());
      if (absVariance < 10) score += 15;
      else if (absVariance < 20) score += 10;
      else if (absVariance < 30) score += 5;
      else if (absVariance > 40) score -= 10;
    }
    
    // Shaping quality contribution (±15 points)
    if (shaping != null && shaping.getOverallQuality() != null) {
      switch (shaping.getOverallQuality()) {
        case EXCELLENT: score += 15; break;
        case GOOD: score += 10; break;
        case NEEDS_ATTENTION: score -= 5; break;
        case POOR: score -= 15; break;
        default: break;
      }
    }
    
    // Risk prediction contribution (±10 points)
    if (risk != null && risk.getPredictiveAccuracyPercent() != null) {
      double accuracy = risk.getPredictiveAccuracyPercent();
      if (accuracy >= 80) score += 10;
      else if (accuracy >= 60) score += 5;
      else if (accuracy < 40) score -= 10;
    }
    
    // Retro follow-through contribution (±10 points)
    if (retro != null && retro.getFollowThroughRatePercent() != null) {
      double rate = retro.getFollowThroughRatePercent();
      if (rate >= 80) score += 10;
      else if (rate >= 60) score += 5;
      else if (rate < 30) score -= 10;
    }
    
    return Math.max(0, Math.min(100, score));
  }

  /** Batch load work hours for a cycle using a single query. */
  private Map<Long, Double> batchLoadWorkHoursForCycle(Long cycleId) {
    Map<Long, Double> hoursMap = new java.util.HashMap<>();
    List<Object[]> results = workLogRepository.getTotalHoursByCycleId(cycleId);
    if (results == null) {
      return hoursMap;
    }
    for (Object[] row : results) {
      if (row[0] == null) {
        continue;
      }
      Long pitchId = ((Number) row[0]).longValue();
      Double hours = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
      hoursMap.put(pitchId, hours);
    }
    return hoursMap;
  }
}
