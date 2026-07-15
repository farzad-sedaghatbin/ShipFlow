package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.config.AIConfig;
import com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.*;
import com.github.farzadsedaghatbin.shipflow.dto.risk.CycleRiskOverviewDTO.*;
import com.github.farzadsedaghatbin.shipflow.entity.BettingSlot;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.PitchRiskHistory;
import com.github.farzadsedaghatbin.shipflow.entity.WorkLog;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import com.github.farzadsedaghatbin.shipflow.repository.BettingSlotRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRiskHistoryRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for AI-powered risk analysis of pitches and cycles using LangChain4j.
 * Supports multiple AI providers: RunPod (cloud GPU) and Ollama
 * (local/self-hosted).
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.ai.risk-analysis.enabled", havingValue = "true", matchIfMissing = false)
public class RiskAnalysisService {

  private final AIConfig aiConfig;
  private final ChatLanguageModel chatLanguageModel;
  private final PitchRepository pitchRepository;
  private final CycleRepository cycleRepository;
  private final WorkLogRepository workLogRepository;
  private final AICacheService cacheService;
  private final PitchRiskHistoryRepository riskHistoryRepository;
  private final OrganizationSettingsService organizationSettingsService;
  private final RiskHistoryService riskHistoryService;
  private final CapacityConfigService capacityConfigService;
  private final EpicRepository epicRepository;
  private final InitiativeRepository initiativeRepository;
  private final BettingSlotRepository bettingSlotRepository;

  @Autowired(required = false)
  @Lazy
  private PitchHealthService pitchHealthService;

  @Autowired(required = false)
  @Lazy
  private KnowledgeIngestionService knowledgeIngestionService;

  @Autowired
  public RiskAnalysisService(AIConfig aiConfig, @Autowired(required = false) ChatLanguageModel chatLanguageModel,
      PitchRepository pitchRepository, CycleRepository cycleRepository, WorkLogRepository workLogRepository,
      AICacheService cacheService, PitchRiskHistoryRepository riskHistoryRepository,
      OrganizationSettingsService organizationSettingsService, RiskHistoryService riskHistoryService,
      CapacityConfigService capacityConfigService, EpicRepository epicRepository,
      InitiativeRepository initiativeRepository, BettingSlotRepository bettingSlotRepository) {
    this.aiConfig = aiConfig;
    this.chatLanguageModel = chatLanguageModel;
    this.pitchRepository = pitchRepository;
    this.cycleRepository = cycleRepository;
    this.workLogRepository = workLogRepository;
    this.cacheService = cacheService;
    this.riskHistoryRepository = riskHistoryRepository;
    this.organizationSettingsService = organizationSettingsService;
    this.riskHistoryService = riskHistoryService;
    this.capacityConfigService = capacityConfigService;
    this.epicRepository = epicRepository;
    this.initiativeRepository = initiativeRepository;
    this.bettingSlotRepository = bettingSlotRepository;
  }

  /** Analyze risk for a single pitch (with AI if enabled). */
  public PitchRiskDTO analyzePitchRisk(Long pitchId) {
    return analyzePitchRisk(pitchId, true);
  }

  /**
   * Analyze risk for a single pitch.
   *
   * @param pitchId
   *            The pitch ID
   * @param useAI
   *            If true and AI is enabled, includes AI-powered analysis
   */
  @Transactional(readOnly = false)
  public PitchRiskDTO analyzePitchRisk(Long pitchId, boolean useAI) {
    // Check cache first
    Optional<PitchRiskDTO> cachedResult = cacheService.getCachedPitchRisk(pitchId, useAI);
    if (cachedResult.isPresent()) {
      log.debug("Returning cached risk analysis for pitch {}", pitchId);
      return cachedResult.get();
    }

    Pitch pitch = pitchRepository.findByIdWithTeamAndAssignments(pitchId)
        .orElseThrow(() -> new RuntimeException("Pitch not found with id: " + pitchId));

    return analyzePitchRisk(pitch, useAI);
  }

  /** Analyze risk for a pitch entity (with AI if enabled). */
  public PitchRiskDTO analyzePitchRisk(Pitch pitch) {
    return analyzePitchRisk(pitch, true);
  }

  /**
   * Analyze risk for a pitch entity.
   *
   * @param pitch
   *            The pitch entity
   * @param useAI
   *            If true and AI is enabled, includes AI-powered analysis
   */
  @Transactional(readOnly = false)
  public PitchRiskDTO analyzePitchRisk(Pitch pitch, boolean useAI) {
    // Check cache first (for entity-based calls)
    Optional<PitchRiskDTO> cachedResult = cacheService.getCachedPitchRisk(pitch.getId(), useAI);
    if (cachedResult.isPresent()) {
      log.debug("Returning cached risk analysis for pitch {}", pitch.getId());
      return cachedResult.get();
    }

    boolean aiEnabled = useAI && aiConfig.isAiRiskAnalysisEnabled() && chatLanguageModel != null;

    try {
      // Calculate basic metrics
      Double totalHours = workLogRepository.getTotalHoursByPitchId(pitch.getId());
      if (totalHours == null)
        totalHours = 0.0;

      double appetiteHours = capacityConfigService.calculatePitchAppetiteHours(pitch);
      double progress = appetiteHours > 0 ? (totalHours / appetiteHours) * 100 : 0;

      // Calculate days elapsed and remaining in cycle
      Cycle cycle = pitch.getCycle();
      LocalDate today = LocalDate.now();
      long daysElapsed = 0;
      long totalCycleDays = 0;
      double cycleProgress = 0;
      if (cycle != null && cycle.getStartDate() != null && cycle.getEndDate() != null) {
        daysElapsed = ChronoUnit.DAYS.between(cycle.getStartDate(), today);
        totalCycleDays = ChronoUnit.DAYS.between(cycle.getStartDate(), cycle.getEndDate());
        double rawCycleProgress = totalCycleDays > 0 ? (double) daysElapsed / totalCycleDays * 100 : 0;
        cycleProgress = Math.max(0, Math.min(100, rawCycleProgress));
      } else {
        log.debug("Pitch {} has no cycle assigned, using zero cycle progress", pitch.getId());
      }

      // Create data hash for change detection
      String dataHash = createPitchDataHash(pitch, totalHours, progress, cycleProgress);

      // Identify risk factors based on metrics
      List<RiskFactor> riskFactors = identifyRiskFactors(pitch, progress, cycleProgress, totalHours,
          appetiteHours);

      // Calculate risk score using PitchHealthService's comprehensive 4-factor
      // weighted algorithm (budget, bugs, scope, time) for consistency between
      // /api/risk and /api/health endpoints. Falls back to legacy factor-based
      // calculation if PitchHealthService is not available.
      int baseRiskScore;
      if (pitchHealthService != null) {
        baseRiskScore = pitchHealthService.calculateRuleBasedRiskScore(pitch, null);
      } else {
        baseRiskScore = calculateBaseRiskScore(riskFactors, pitch, progress, cycleProgress);
      }

      // Generate insights - ONLY when AI is requested (useAI=true)
      // In fast mode, leave empty so UI shows loading state for AI sections
      List<String> insights = new ArrayList<>();
      List<String> recommendations = new ArrayList<>();
      int confidenceScore = 70; // Default confidence without AI

      if (useAI) {
        if (aiEnabled) {
          try {
            AIAnalysisResult aiResult = generateAIInsights(pitch, riskFactors, progress, cycleProgress);
            insights = aiResult.insights;
            recommendations = aiResult.recommendations;
            confidenceScore = aiResult.confidenceScore;

            // Adjust risk score based on AI analysis
            baseRiskScore = adjustRiskScoreWithAI(baseRiskScore, aiResult);
          } catch (Exception e) {
            log.warn("AI analysis failed, falling back to rule-based: {}", e.getMessage());
            insights = generateRuleBasedInsights(pitch, riskFactors, progress, cycleProgress);
            recommendations = generateRuleBasedRecommendations(riskFactors);
          }
        } else {
          // AI requested but not available - use rule-based as fallback
          insights = generateRuleBasedInsights(pitch, riskFactors, progress, cycleProgress);
          recommendations = generateRuleBasedRecommendations(riskFactors);
        }
      }
      // When useAI=false (fast mode), insights and recommendations remain empty

      // Ensure risk score is within bounds
      int finalRiskScore = Math.max(0, Math.min(100, baseRiskScore));
      RiskLevel riskLevel = determineRiskLevel(finalRiskScore);

      PitchRiskDTO result = PitchRiskDTO.builder().pitchId(pitch.getId()).pitchTitle(pitch.getTitle())
          .riskScore(finalRiskScore).riskLevel(riskLevel)
          .explanation(buildScoreExplanation(finalRiskScore, riskFactors)).riskFactors(riskFactors)
          .insights(insights).recommendations(recommendations).confidenceScore(confidenceScore)
          .analyzedAt(LocalDateTime.now()).aiEnabled(aiEnabled).build();

      // Cache the result
      cacheService.cachePitchRisk(pitch.getId(), useAI, result, dataHash);

      // Save to risk history (only for AI-enabled or manual analyses, not fast mode)
      if (useAI) {
        riskHistoryService.saveRiskHistory(pitch, result, PitchRiskHistory.TriggerType.MANUAL);

        // Ingest risk summary into vector store for future historical pattern RAG
        if (knowledgeIngestionService != null && result.getInsights() != null) {
          Long cycleId = pitch.getCycle() != null ? pitch.getCycle().getId() : null;
          knowledgeIngestionService.ingestRiskSummary(
              pitch.getId(), pitch.getTitle(),
              result.getRiskLevel() != null ? result.getRiskLevel().name() : "UNKNOWN",
              result.getRiskScore(),
              result.getInsights(),
              result.getRecommendations(),
              cycleId);
        }
      }

      return result;

    } catch (Exception e) {
      log.error("Error analyzing pitch risk: {}", e.getMessage(), e);
      return PitchRiskDTO.builder().pitchId(pitch.getId()).pitchTitle(pitch.getTitle()).riskScore(50)
          .riskLevel(RiskLevel.MEDIUM).explanation(buildScoreExplanation(50, Collections.emptyList()))
          .riskFactors(Collections.emptyList()).insights(Collections.emptyList())
          .recommendations(Collections.emptyList()).confidenceScore(0).analyzedAt(LocalDateTime.now())
          .aiEnabled(aiEnabled).errorMessage("Failed to analyze risk: " + e.getMessage()).build();
    }
  }

  /** Get risk overview for an entire cycle (with AI if enabled). */
  public CycleRiskOverviewDTO getCycleRiskOverview(Long cycleId) {
    return getCycleRiskOverview(cycleId, true);
  }

  /**
   * Get risk overview for an entire cycle.
   *
   * @param cycleId
   *            The cycle ID
   * @param useAI
   *            If true and AI is enabled, includes AI-powered analysis
   */
  @Transactional
  public CycleRiskOverviewDTO getCycleRiskOverview(Long cycleId, boolean useAI) {
    // Check cache first
    Optional<CycleRiskOverviewDTO> cachedResult = cacheService.getCachedCycleRisk(cycleId, useAI);
    if (cachedResult.isPresent()) {
      log.debug("Returning cached risk overview for cycle {}", cycleId);
      return cachedResult.get();
    }

    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new RuntimeException("Cycle not found with id: " + cycleId));

    List<Pitch> pitches = pitchRepository.findByCycleIdNotDeleted(cycleId);
    boolean aiEnabled = useAI && aiConfig.isAiRiskAnalysisEnabled() && chatLanguageModel != null;

    // Analyze each pitch (using fast mode if AI is disabled)
    List<PitchRiskDTO> pitchRisks = pitches.stream().map(p -> analyzePitchRisk(p, useAI))
        .collect(Collectors.toList());

    // Calculate statistics
    int totalPitches = pitchRisks.size();
    int highRiskCount = (int) pitchRisks.stream()
        .filter(r -> r.getRiskLevel() == RiskLevel.HIGH || r.getRiskLevel() == RiskLevel.CRITICAL).count();
    int mediumRiskCount = (int) pitchRisks.stream().filter(r -> r.getRiskLevel() == RiskLevel.MEDIUM).count();
    int lowRiskCount = (int) pitchRisks.stream().filter(r -> r.getRiskLevel() == RiskLevel.LOW).count();

    // Calculate overall risk score (weighted average with higher weight for
    // high-risk items)
    int overallRiskScore = calculateOverallRiskScore(pitchRisks);
    RiskLevel overallRiskLevel = determineRiskLevel(overallRiskScore);

    // Create pitch risk summaries
    List<PitchRiskSummary> pitchSummaries = pitchRisks.stream().map(this::toPitchRiskSummary)
        .sorted(Comparator.comparing(PitchRiskSummary::getRiskScore).reversed()).collect(Collectors.toList());

    // Aggregate common risk factors
    List<CommonRiskFactor> topRiskFactors = aggregateRiskFactors(pitchRisks);

    // Calculate cycle stats
    CycleRiskStats stats = calculateCycleStats(pitchRisks, pitches);

    // Generate cycle-level insights - ONLY when AI is requested
    // In fast mode, leave empty so UI shows loading state for AI sections
    List<String> cycleInsights = new ArrayList<>();
    List<String> cycleRecommendations = new ArrayList<>();

    if (useAI) {
      if (aiEnabled) {
        try {
          CycleAIAnalysisResult aiResult = generateCycleAIInsights(cycle, pitchRisks, stats);
          cycleInsights = aiResult.insights;
          cycleRecommendations = aiResult.recommendations;
        } catch (Exception e) {
          log.warn("AI cycle analysis failed, falling back to rule-based: {}", e.getMessage());
          cycleInsights = generateRuleBasedCycleInsights(pitchRisks, stats, highRiskCount);
          cycleRecommendations = generateRuleBasedCycleRecommendations(topRiskFactors, highRiskCount);
        }
      } else {
        // AI requested but not available - use rule-based as fallback
        cycleInsights = generateRuleBasedCycleInsights(pitchRisks, stats, highRiskCount);
        cycleRecommendations = generateRuleBasedCycleRecommendations(topRiskFactors, highRiskCount);
      }
    }
    // When useAI=false (fast mode), cycleInsights and cycleRecommendations remain
    // empty

    CycleRiskOverviewDTO result = CycleRiskOverviewDTO.builder().cycleId(cycle.getId()).cycleName(cycle.getName())
        .projectName(cycle.getProject() != null ? cycle.getProject().getName() : null)
        .overallRiskScore(overallRiskScore).overallRiskLevel(overallRiskLevel).totalPitches(totalPitches)
        .highRiskPitches(highRiskCount).mediumRiskPitches(mediumRiskCount).lowRiskPitches(lowRiskCount)
        .stats(stats).pitchRisks(pitchSummaries).cycleInsights(cycleInsights)
        .cycleRecommendations(cycleRecommendations).topRiskFactors(topRiskFactors)
        .analyzedAt(LocalDateTime.now()).aiEnabled(aiEnabled).build();

    // Cache the result
    cacheService.cacheCycleRisk(cycleId, useAI, result);

    return result;
  }

  /** Check if AI risk analysis is available. */
  public boolean isAIAvailable() {
    return aiConfig.isAiRiskAnalysisEnabled() && chatLanguageModel != null;
  }

  /**
   * Answer a question about pitch risk using AI.
   *
   * Runs inside a read-only transaction so that lazy associations on the pitch
   * (team assignments → person) can be navigated while building the prompt
   * context. With {@code spring.jpa.open-in-view=false}, touching these outside
   * a tx throws {@code LazyInitializationException}.
   */
  @Transactional(readOnly = true)
  public RiskQuestionResponse answerRiskQuestion(Long pitchId, String question) {
    // Get pitch first
    Pitch pitch;
    try {
      pitch = pitchRepository.findByIdWithTeamAndAssignments(pitchId)
          .orElseThrow(() -> new RuntimeException("Pitch not found with id: " + pitchId));
    } catch (Exception e) {
      return RiskQuestionResponse.builder().pitchId(pitchId).pitchTitle("Unknown").question(question).answer(null)
          .confidenceScore(0).aiEnabled(false).answeredAt(LocalDateTime.now())
          .errorMessage("Pitch not found: " + e.getMessage()).build();
    }

    boolean aiEnabled = aiConfig.isAiRiskAnalysisEnabled() && chatLanguageModel != null;

    if (!aiEnabled) {
      return RiskQuestionResponse.builder().pitchId(pitchId).pitchTitle(pitch.getTitle()).question(question)
          .answer("AI risk advisor is not enabled. Please configure Ollama/Mistral to use the Q&A feature.")
          .confidenceScore(0).aiEnabled(false).answeredAt(LocalDateTime.now()).build();
    }

    try {
      // Get pitch context
      PitchRiskDTO riskAnalysis = analyzePitchRisk(pitch);

      // Calculate basic metrics for context
      Double totalHours = workLogRepository.getTotalHoursByPitchId(pitch.getId());
      if (totalHours == null)
        totalHours = 0.0;
      double appetiteHours = capacityConfigService.calculatePitchAppetiteHours(pitch);

      Cycle cycle = pitch.getCycle();
      LocalDate today = LocalDate.now();
      long daysRemaining =
          (cycle != null && cycle.getEndDate() != null)
              ? ChronoUnit.DAYS.between(today, cycle.getEndDate())
              : 0L;

      // Build context for AI - include all Shape Up methodology fields
      StringBuilder pitchDetails = new StringBuilder();
      if (pitch.getDescription() != null && !pitch.getDescription().isBlank()) {
        pitchDetails.append("Description: ").append(pitch.getDescription()).append("\n");
      }
      if (pitch.getProblemStatement() != null && !pitch.getProblemStatement().isBlank()) {
        pitchDetails.append("Problem Statement: ").append(pitch.getProblemStatement()).append("\n");
      }
      if (pitch.getSolution() != null && !pitch.getSolution().isBlank()) {
        pitchDetails.append("Proposed Solution: ").append(pitch.getSolution()).append("\n");
      }
      if (pitch.getRabbitHoles() != null && !pitch.getRabbitHoles().isBlank()) {
        pitchDetails.append("Rabbit Holes: ").append(pitch.getRabbitHoles()).append("\n");
      }
      if (pitch.getRisks() != null && !pitch.getRisks().isBlank()) {
        pitchDetails.append("Known Risks: ").append(pitch.getRisks()).append("\n");
      }
      if (pitch.getNoGos() != null && !pitch.getNoGos().isBlank()) {
        pitchDetails.append("No-Gos (out of scope): ").append(pitch.getNoGos()).append("\n");
      }
      if (pitchDetails.isEmpty()) {
        pitchDetails.append("No detailed pitch information available.");
      }

      String context = String.format("""
          Pitch: %s
          Status: %s
          Team: %s
          Cycle: %s (ends in %d days)

          === PITCH DETAILS ===
          %s

          Budget: %.1f hours used of %.1f hours appetite (%.1f%% used)

          Current Risk Analysis:
          - Risk Score: %d/100
          - Risk Level: %s
          - Confidence: %d%%

          Risk Factors:
          %s

          Current Insights:
          %s

          Current Recommendations:
          %s
          """, pitch.getTitle(), pitch.getStatus(),
          pitch.getTeam() != null ? pitch.getTeam().getName() : "No team assigned",
          cycle != null ? cycle.getName() : "No cycle assigned",
          daysRemaining, pitchDetails.toString(), totalHours, appetiteHours,
          riskAnalysis.getRiskScore() > 0 ? (totalHours / appetiteHours * 100) : 0,
          riskAnalysis.getRiskScore(), riskAnalysis.getRiskLevel(), riskAnalysis.getConfidenceScore(),
          riskAnalysis.getRiskFactors().stream().map(f -> "- " + f.getCategory() + ": " + f.getDescription())
              .collect(Collectors.joining("\n")),
          String.join("\n- ", riskAnalysis.getInsights()),
          String.join("\n- ", riskAnalysis.getRecommendations()));

      // Enrich Q&A context with epic, initiative, cycle load, and risk history
      String broadContext = buildEpicAndInitiativeContext(pitch);
      if (!broadContext.isBlank()) {
        context = context + "\n" + broadContext;
      }

      String prompt = String.format(
          """
              You are an AI Risk Advisor for a ShipFlow project management system.

              Based on the following pitch context, answer the user's question helpfully and concisely.
              Focus on practical, actionable advice related to risk management and project success.

              CONTEXT:
              %s

              USER QUESTION: %s

              Provide a helpful, specific answer based on the context. Keep your response under 300 words.
              If the question is not related to the pitch or risk management, politely redirect to relevant topics.
              """,
          context, question);

      log.info("🤖 Answering risk question using provider: {} (model: {})", aiConfig.getProvider(),
          aiConfig.getModelName());

      long startTime = System.currentTimeMillis();
      String answer = chatLanguageModel.generate(prompt);
      long duration = System.currentTimeMillis() - startTime;

      log.info("✅ AI answer received in {}ms", duration);

      return RiskQuestionResponse.builder().pitchId(pitchId).pitchTitle(pitch.getTitle()).question(question)
          .answer(answer.trim()).confidenceScore(85).aiEnabled(true).answeredAt(LocalDateTime.now()).build();

    } catch (Exception e) {
      log.error("Error answering risk question: {}", e.getMessage(), e);

      // Provide user-friendly error messages
      String errorMsg;
      if (e.getMessage() != null && e.getMessage().contains("Connection refused")) {
        errorMsg = "AI service (Ollama) is not running. Please start Ollama with 'ollama serve' or disable AI features.";
      } else if (e.getMessage() != null && e.getMessage().contains("connect")) {
        errorMsg = "Cannot connect to AI service. Please ensure Ollama is running on port 11434.";
      } else {
        errorMsg = "Failed to generate answer: " + e.getMessage();
      }

      return RiskQuestionResponse.builder().pitchId(pitchId).pitchTitle(pitch.getTitle()).question(question)
          .answer(null).confidenceScore(0).aiEnabled(aiEnabled).answeredAt(LocalDateTime.now())
          .errorMessage(errorMsg).build();
    }
  }

  // ==================== Private Helper Methods ====================

  private List<RiskFactor> identifyRiskFactors(Pitch pitch, double progress, double cycleProgress, double totalHours,
      double appetiteHours) {
    List<RiskFactor> factors = new ArrayList<>();

    // Check for time overrun risk
    if (cycleProgress > progress + 20) {
      int impact = (int) Math.min(10, (cycleProgress - progress) / 10);
      factors.add(RiskFactor.builder().category(RiskFactor.RiskCategory.TIME_OVERRUN)
          .description(
              "Cycle is " + String.format("%.0f", cycleProgress - progress) + "% ahead of pitch progress")
          .impactLevel(impact).probability(Math.min(10, impact + 2)).build());
    }

    // Check for appetite mismatch
    if (totalHours > appetiteHours * 0.8 && progress < 70) {
      factors.add(RiskFactor.builder().category(RiskFactor.RiskCategory.APPETITE_MISMATCH)
          .description("80% of appetite used but only " + String.format("%.0f", progress) + "% complete")
          .impactLevel(8).probability(7).build());
    }

    // Check for progress stagnation (no recent work logs)
    List<WorkLog> recentLogs = workLogRepository.findByPitchId(pitch.getId()).stream()
        .filter(wl -> wl.getDate().isAfter(LocalDate.now().minusDays(3))).collect(Collectors.toList());

    if (recentLogs.isEmpty() && pitch.getStatus() == PitchStatus.IN_PROGRESS) {
      factors.add(RiskFactor.builder().category(RiskFactor.RiskCategory.PROGRESS_STAGNATION)
          .description("No work logged in the last 3 days while pitch is in progress").impactLevel(6)
          .probability(8).build());
    }

    // Check for team capacity issues
    if (pitch.getTeam() == null) {
      factors.add(RiskFactor.builder().category(RiskFactor.RiskCategory.RESOURCE_CONSTRAINT)
          .description("No team assigned to this pitch").impactLevel(9).probability(9).build());
    }

    // Check for unclear requirements - consider ALL Shape Up methodology fields
    // A pitch has sufficient context if it has either:
    // 1. A detailed description (>= 50 chars), OR
    // 2. A meaningful problemStatement + solution combination
    boolean hasDescription = pitch.getDescription() != null && pitch.getDescription().length() >= 50;
    boolean hasProblemStatement = pitch.getProblemStatement() != null && !pitch.getProblemStatement().isBlank();
    boolean hasSolution = pitch.getSolution() != null && !pitch.getSolution().isBlank();
    boolean hasShapeUpContext = hasProblemStatement && hasSolution;

    if (!hasDescription && !hasShapeUpContext) {
      factors.add(RiskFactor.builder().category(RiskFactor.RiskCategory.UNCLEAR_REQUIREMENTS).description(
          "Pitch is missing key context: needs either a detailed description or problem statement with solution")
          .impactLevel(5).probability(6).build());
    }

    // Check for scope creep (hours spent way over appetite)
    if (totalHours > appetiteHours * 1.2) {
      factors.add(RiskFactor.builder().category(RiskFactor.RiskCategory.SCOPE_CREEP)
          .description("Hours spent exceed appetite by "
              + String.format("%.0f", ((totalHours / appetiteHours) - 1) * 100) + "%")
          .impactLevel(7).probability(10).build());
    }

    // Check status-based risks
    if (pitch.getStatus() == PitchStatus.PENDING && cycleProgress > 30) {
      factors.add(RiskFactor.builder().category(RiskFactor.RiskCategory.TIME_OVERRUN)
          .description(
              "Pitch still pending but cycle is " + String.format("%.0f", cycleProgress) + "% complete")
          .impactLevel(8).probability(9).build());
    }

    return factors;
  }

  private int calculateBaseRiskScore(List<RiskFactor> factors, Pitch pitch, double progress, double cycleProgress) {
    if (factors.isEmpty()) {
      // Base score from progress vs cycle progress
      if (cycleProgress > progress) {
        return (int) Math.min(50, (cycleProgress - progress));
      }
      return 20; // Low baseline risk
    }

    // Calculate weighted risk from factors
    double totalWeightedRisk = factors.stream().mapToDouble(f -> f.getImpactLevel() * f.getProbability() / 10.0)
        .sum();

    // Normalize to 0-100 scale
    int factorScore = (int) Math.min(80, totalWeightedRisk * 5);

    // Add status-based adjustment
    int statusAdjustment = switch (pitch.getStatus()) {
      case DONE -> -30;
      case TESTING -> -10;
      case IN_PROGRESS -> 0;
      case STARTED -> 5;
      case SHAPED -> 10;
      case PENDING -> 15;
      case DRAFT -> 20; // Early shaping - more uncertainty
      case IDEA -> 25; // Raw idea - highest uncertainty
      case COOLDOWN -> -20;
      case CANCELLED -> -50;
      case CIRCUIT_BREAKER -> 50; // High risk when circuit breaker is triggered
    };

    return Math.max(0, Math.min(100, factorScore + statusAdjustment + 20));
  }

  private AIAnalysisResult generateAIInsights(Pitch pitch, List<RiskFactor> factors, double progress,
      double cycleProgress) {
    log.info("🤖 Generating AI insights using provider: {} (model: {})", aiConfig.getProvider(),
        aiConfig.getModelName());

    String prompt = buildAIPrompt(pitch, factors, progress, cycleProgress);

    long startTime = System.currentTimeMillis();
    String response = chatLanguageModel.generate(prompt);
    long duration = System.currentTimeMillis() - startTime;

    log.info("✅ AI response received in {}ms", duration);
    return parseAIResponse(response);
  }

  /**
   * Builds a structured context block covering the pitch's epic, sibling pitches
   * within that epic, initiative/roadmap alignment, cycle-level load, and recent
   * risk history — all fetched directly from the DB so the LLM gets ground truth.
   */
  private String buildEpicAndInitiativeContext(Pitch pitch) {
    StringBuilder sb = new StringBuilder();

    try {
      // Resolve this pitch's current team from the Betting Table (the actual
      // per-cycle team-slot assignment), not pitch.getTeam() — that field is
      // also written by the standalone assign-team/edit endpoints and isn't
      // cleared on slot removal, so it can drift from what the Betting Table
      // UI shows and falsely group pitches under the wrong team.
      Long pitchTeamId = bettingSlotRepository.findByPitchId(pitch.getId())
          .map(slot -> slot.getTeam().getId()).orElse(null);

      Epic epic = pitch.getEpic();
      if (epic == null && pitch.getEpic() != null) {
        epic = epicRepository.findById(pitch.getEpic().getId()).orElse(null);
      }

      if (epic != null) {
        sb.append("\n=== EPIC & ROADMAP CONTEXT ===\n");
        sb.append("Epic: ").append(epic.getName()).append("\n");
        if (epic.getDescription() != null && !epic.getDescription().isBlank()) {
          String desc = epic.getDescription().length() > 300
              ? epic.getDescription().substring(0, 300) + "..."
              : epic.getDescription();
          sb.append("Epic Goal: ").append(desc).append("\n");
        }
        if (epic.getStatus() != null) {
          sb.append("Epic Status: ").append(epic.getStatus()).append("\n");
        }
        if (epic.getTargetEndDate() != null) {
          sb.append("Epic Target End: ").append(epic.getTargetEndDate()).append("\n");
        }

        // Initiative / roadmap alignment
        Initiative initiative = epic.getInitiative();
        if (initiative != null) {
          sb.append("\nInitiative: ").append(initiative.getName()).append("\n");
          if (initiative.getDescription() != null && !initiative.getDescription().isBlank()) {
            String idesc = initiative.getDescription().length() > 200
                ? initiative.getDescription().substring(0, 200) + "..."
                : initiative.getDescription();
            sb.append("Initiative Goal: ").append(idesc).append("\n");
          }
          if (initiative.getStatus() != null) {
            sb.append("Initiative Status: ").append(initiative.getStatus()).append("\n");
          }
        }

        // Sibling pitches in same epic
        List<Pitch> siblings = pitchRepository.findByEpicIdNotDeleted(epic.getId());
        List<Pitch> otherSiblings = siblings.stream()
            .filter(p -> !p.getId().equals(pitch.getId()))
            .collect(Collectors.toList());

        if (!otherSiblings.isEmpty()) {
          sb.append("\nOther pitches in this epic (").append(otherSiblings.size()).append(" total):\n");
          for (Pitch sibling : otherSiblings) {
            sb.append("  - ").append(sibling.getTitle())
                .append(" [").append(sibling.getStatus()).append("]");
            if (sibling.getAppetiteDays() != null) {
              sb.append(" [Appetite: ").append(sibling.getAppetiteDays()).append(" days]");
            }
            Optional<BettingSlot> siblingSlot = bettingSlotRepository.findByPitchId(sibling.getId());
            if (siblingSlot.isPresent()) {
              Long siblingTeamId = siblingSlot.get().getTeam().getId();
              if (pitchTeamId != null && siblingTeamId.equals(pitchTeamId)) {
                sb.append(" SAME TEAM");
              } else {
                // Always state the actual team, even when it differs — an omitted team
                // reads to the LLM as "unknown", and it will otherwise assume this pitch
                // shares the analyzed pitch's team since no other team is mentioned.
                // "DIFFERENT TEAM" (not the subtler "[Team: X]") because the LLM was still
                // narrating these as the analyzed pitch's own team-capacity load even with
                // the team name present — see buildAIPrompt's STRICT RULES for the matching
                // instruction that tells it how to use this tag.
                sb.append(" [DIFFERENT TEAM: ").append(siblingSlot.get().getTeam().getName()).append("]");
              }
            }
            sb.append("\n");
          }
        }
      }

      // Cycle-level load: how many other pitches share this cycle
      if (pitch.getCycle() != null) {
        List<Pitch> cyclePitches = pitchRepository.findByCycleIdNotDeleted(pitch.getCycle().getId());
        List<Pitch> otherCyclePitches = cyclePitches.stream()
            .filter(p -> !p.getId().equals(pitch.getId()))
            .collect(Collectors.toList());

        if (!otherCyclePitches.isEmpty()) {
          // Batch-resolve team-slot assignments for this cycle in one query rather
          // than one findByPitchId per pitch.
          Map<Long, BettingSlot> cycleSlotByPitchId = bettingSlotRepository
              .findByCycleId(pitch.getCycle().getId()).stream()
              .filter(slot -> slot.getPitch() != null)
              .collect(Collectors.toMap(slot -> slot.getPitch().getId(), slot -> slot, (a, b) -> a));

          sb.append("\n=== CYCLE LOAD ===\n");
          sb.append("Other pitches in this cycle (").append(otherCyclePitches.size()).append("):\n");
          for (Pitch cp : otherCyclePitches) {
            sb.append("  - ").append(cp.getTitle())
                .append(" [").append(cp.getStatus()).append("]");
            BettingSlot cpSlot = cycleSlotByPitchId.get(cp.getId());
            if (cpSlot != null) {
              Long cpTeamId = cpSlot.getTeam().getId();
              if (pitchTeamId != null && cpTeamId.equals(pitchTeamId)) {
                sb.append(" SAME TEAM");
              } else {
                // Always state the actual team, even when it differs — an omitted team
                // reads to the LLM as "unknown", and it will otherwise assume this pitch
                // shares the analyzed pitch's team since no other team is mentioned.
                // "DIFFERENT TEAM" (not the subtler "[Team: X]") — see the note above and
                // buildAIPrompt's STRICT RULES for why.
                sb.append(" [DIFFERENT TEAM: ").append(cpSlot.getTeam().getName()).append("]");
              }
            }
            sb.append("\n");
          }
        }
      }

      // Recent risk history for this pitch
      List<PitchRiskHistory> history =
          riskHistoryRepository.findByPitchIdOrderByRecordedAtDesc(pitch.getId());
      if (history != null && !history.isEmpty()) {
        sb.append("\n=== RISK HISTORY (last 3 snapshots) ===\n");
        history.stream().limit(3).forEach(h -> {
          sb.append("  ").append(h.getRecordedAt().toLocalDate())
              .append(" — Level: ").append(h.getRiskLevel())
              .append(", Score: ").append(h.getRiskScore()).append("/100\n");
        });
      }

    } catch (Exception e) {
      log.warn("Failed to build epic/initiative context for pitch {}: {}", pitch.getId(), e.getMessage());
    }

    return sb.toString();
  }

  private String buildAIPrompt(Pitch pitch, List<RiskFactor> factors, double progress, double cycleProgress) {
    // Resolve from the Betting Table slot, same as buildEpicAndInitiativeContext — pitch.getTeam()
    // is the drift-prone field written by the standalone assign-team/edit endpoints and can disagree
    // with what the Betting Table (and this prompt's own sibling/cycle-mate tags) actually shows.
    String pitchTeamName = bettingSlotRepository.findByPitchId(pitch.getId())
        .map(slot -> slot.getTeam().getName())
        .orElse(pitch.getTeam() != null ? pitch.getTeam().getName() : "Not assigned");

    StringBuilder sb = new StringBuilder();
    sb.append("You are a project risk advisor for a software development team using the Shape Up methodology.\n\n");
    sb.append("Analyze the following pitch and provide risk insights:\n\n");
    sb.append("Pitch Title: ").append(pitch.getTitle()).append("\n");
    sb.append("Appetite: ").append(pitch.getAppetiteDays()).append(" days\n");
    sb.append("Status: ").append(pitch.getStatus()).append("\n");
    sb.append("Progress: ").append(String.format("%.1f", progress)).append("%\n");
    sb.append("Cycle Progress: ").append(String.format("%.1f", cycleProgress)).append("%\n");
    sb.append("Team: ").append(pitchTeamName).append("\n\n");

    // Include all Shape Up methodology fields for comprehensive analysis
    sb.append("=== PITCH DETAILS ===\n");
    if (pitch.getDescription() != null && !pitch.getDescription().isBlank()) {
      sb.append("Description: ").append(pitch.getDescription()).append("\n\n");
    }
    if (pitch.getProblemStatement() != null && !pitch.getProblemStatement().isBlank()) {
      sb.append("Problem Statement: ").append(pitch.getProblemStatement()).append("\n\n");
    }
    if (pitch.getSolution() != null && !pitch.getSolution().isBlank()) {
      sb.append("Proposed Solution: ").append(pitch.getSolution()).append("\n\n");
    }
    if (pitch.getRabbitHoles() != null && !pitch.getRabbitHoles().isBlank()) {
      sb.append("Rabbit Holes (areas to avoid): ").append(pitch.getRabbitHoles()).append("\n\n");
    }
    if (pitch.getRisks() != null && !pitch.getRisks().isBlank()) {
      sb.append("Known Risks: ").append(pitch.getRisks()).append("\n\n");
    }
    if (pitch.getNoGos() != null && !pitch.getNoGos().isBlank()) {
      sb.append("No-Gos (out of scope): ").append(pitch.getNoGos()).append("\n\n");
    }
    if (pitch.getWireframeLinks() != null && !pitch.getWireframeLinks().isBlank()) {
      sb.append("Wireframe/Design Links: ").append(pitch.getWireframeLinks()).append("\n\n");
    }

    if (!factors.isEmpty()) {
      sb.append("Identified Risk Factors:\n");
      for (RiskFactor factor : factors) {
        sb.append("- ").append(factor.getCategory()).append(": ").append(factor.getDescription())
            .append(" (Impact: ").append(factor.getImpactLevel()).append("/10)\n");
      }
    }

    // Inject epic, initiative, cycle-load, and risk-history context
    String broadContext = buildEpicAndInitiativeContext(pitch);
    if (!broadContext.isBlank()) {
      sb.append(broadContext);
    }

    sb.append("\nProvide your analysis in the following format:\n");
    sb.append("INSIGHTS:\n- [insight 1]\n- [insight 2]\n- [insight 3]\n\n");
    sb.append("RECOMMENDATIONS:\n- [recommendation 1]\n- [recommendation 2]\n- [recommendation 3]\n\n");
    sb.append("CONFIDENCE: [0-100]\n\n");
    sb.append("STRICT RULES:\n");
    sb.append("- Reference sibling pitches by name if they share the same team or create competing scope.\n");
    sb.append(
        "- A sibling/cycle-mate pitch tagged \"SAME TEAM\" above shares this pitch's team; one tagged "
            + "\"[DIFFERENT TEAM: X]\" belongs to team X, NOT this pitch's team. NEVER describe a "
            + "\"[DIFFERENT TEAM: X]\" pitch as adding to \"Team ").append(pitchTeamName)
        .append("'s\" workload or say this pitch's team is \"running\", \"carrying\", or \"simultaneously "
            + "handling\" it — attribute any capacity pressure from it to team X instead, or describe the "
            + "overlap as a cross-team dependency/scope-conflict risk without naming a team as the executor.\n");
    sb.append("- Flag initiative/roadmap pressure explicitly if the initiative is delayed or at risk.\n");
    sb.append("- If risk history shows a worsening trend, call it out.\n");
    sb.append("- Keep insights actionable and specific to this pitch — do not repeat generic advice.");

    return sb.toString();
  }

  private AIAnalysisResult parseAIResponse(String response) {
    List<String> insights = new ArrayList<>();
    List<String> recommendations = new ArrayList<>();
    int confidence = 70;

    String[] sections = response.split("(?i)(INSIGHTS:|RECOMMENDATIONS:|CONFIDENCE:)");

    for (int i = 1; i < sections.length; i++) {
      String section = sections[i].trim();
      String prevHeader = findPreviousHeader(response, sections[i]);

      if (prevHeader.equalsIgnoreCase("INSIGHTS:")) {
        insights = extractBulletPoints(section);
      } else if (prevHeader.equalsIgnoreCase("RECOMMENDATIONS:")) {
        recommendations = extractBulletPoints(section);
      } else if (prevHeader.equalsIgnoreCase("CONFIDENCE:")) {
        try {
          confidence = Integer
              .parseInt(section.replaceAll("[^0-9]", "").substring(0, Math.min(3, section.length())));
          confidence = Math.max(0, Math.min(100, confidence));
        } catch (Exception e) {
          confidence = 70;
        }
      }
    }

    // Fallback if parsing failed
    if (insights.isEmpty()) {
      insights = List.of("Analysis completed - review risk factors for detailed assessment");
    }
    if (recommendations.isEmpty()) {
      recommendations = List.of("Monitor progress closely and address identified risk factors");
    }

    return new AIAnalysisResult(insights, recommendations, confidence);
  }

  private String findPreviousHeader(String fullResponse, String section) {
    int sectionStart = fullResponse.indexOf(section);
    String beforeSection = fullResponse.substring(0, sectionStart);

    if (beforeSection.toUpperCase().contains("CONFIDENCE"))
      return "CONFIDENCE:";
    if (beforeSection.toUpperCase().contains("RECOMMENDATIONS"))
      return "RECOMMENDATIONS:";
    if (beforeSection.toUpperCase().contains("INSIGHTS"))
      return "INSIGHTS:";
    return "";
  }

  private List<String> extractBulletPoints(String text) {
    return Arrays.stream(text.split("\n")).map(String::trim).filter(line -> !line.isEmpty())
        .map(line -> line.replaceFirst("^[-•*]\\s*", "")).filter(line -> !line.isEmpty() && line.length() > 3)
        .limit(5).collect(Collectors.toList());
  }

  private int adjustRiskScoreWithAI(int baseScore, AIAnalysisResult aiResult) {
    // If AI has high confidence and provided good insights, adjust slightly
    if (aiResult.confidenceScore > 80 && aiResult.insights.size() >= 3) {
      return baseScore; // Trust the base calculation
    }
    return baseScore;
  }

  private List<String> generateRuleBasedInsights(Pitch pitch, List<RiskFactor> factors, double progress,
      double cycleProgress) {
    List<String> insights = new ArrayList<>();

    if (cycleProgress > progress + 20) {
      insights.add(
          "The pitch is falling behind the cycle timeline. Consider re-evaluating scope or adding resources.");
    }

    if (progress > 90) {
      insights.add("The pitch is nearing completion. Final testing and documentation should be prioritized.");
    }

    for (RiskFactor factor : factors) {
      switch (factor.getCategory()) {
        case SCOPE_CREEP ->
          insights.add("Scope creep detected - the pitch has exceeded its original appetite estimate.");
        case PROGRESS_STAGNATION ->
          insights.add("Work on this pitch has stalled. Consider a team check-in to identify blockers.");
        case RESOURCE_CONSTRAINT ->
          insights.add("No team assigned - this pitch cannot progress without resources.");
        case APPETITE_MISMATCH ->
          insights.add("The original appetite may have been underestimated for this pitch.");
        default -> {
        }
      }
    }

    if (insights.isEmpty()) {
      insights.add("The pitch appears to be progressing normally with no significant risks identified.");
    }

    return insights.stream().limit(5).collect(Collectors.toList());
  }

  private List<String> generateRuleBasedRecommendations(List<RiskFactor> factors) {
    List<String> recommendations = new ArrayList<>();

    for (RiskFactor factor : factors) {
      switch (factor.getCategory()) {
        case TIME_OVERRUN -> recommendations
            .add("Consider cutting scope to meet the deadline, or adjust timeline expectations.");
        case SCOPE_CREEP -> recommendations
            .add("Review and document the actual scope versus original pitch to prevent further creep.");
        case RESOURCE_CONSTRAINT ->
          recommendations.add("Assign a team to this pitch immediately to enable progress.");
        case PROGRESS_STAGNATION ->
          recommendations.add("Schedule a sync meeting to identify and remove blockers.");
        case UNCLEAR_REQUIREMENTS ->
          recommendations.add("Add more detail to the pitch description to clarify requirements.");
        case APPETITE_MISMATCH ->
          recommendations.add("Document lessons learned about estimation for future cycles.");
        default -> {
        }
      }
    }

    if (recommendations.isEmpty()) {
      recommendations.add("Continue monitoring progress and maintain current pace.");
    }

    return recommendations.stream().distinct().limit(5).collect(Collectors.toList());
  }

  // Default risk-level boundaries, used when organization settings are
  // unavailable. Kept in sync with OrganizationSettingsService defaults.
  static final int DEFAULT_LOW_MAX = 30;
  static final int DEFAULT_MEDIUM_MAX = 60;
  static final int DEFAULT_HIGH_MAX = 85;

  /**
   * Determine risk level from score using configurable thresholds. Falls back to
   * defaults if organization settings are unavailable.
   */
  private RiskLevel determineRiskLevel(int score) {
    int[] b = resolveBandBoundaries();
    if (score > b[2])
      return RiskLevel.CRITICAL;
    if (score > b[1])
      return RiskLevel.HIGH;
    if (score > b[0])
      return RiskLevel.MEDIUM;
    return RiskLevel.LOW;
  }

  /**
   * Resolve the three risk-level boundaries (lowMax, mediumMax, highMax) from
   * organization settings, falling back to defaults if settings are unavailable.
   *
   * @return a 3-element array {@code [lowMax, mediumMax, highMax]}
   */
  private int[] resolveBandBoundaries() {
    try {
      OrganizationSettingsDTO settings = organizationSettingsService.getSettings();
      OrganizationSettingsDTO.RiskThresholds thresholds = settings.getRiskThresholds();
      return new int[] {thresholds.getLowMax(), thresholds.getMediumMax(), thresholds.getHighMax()};
    } catch (Exception e) {
      log.warn("Failed to fetch risk thresholds, using defaults: {}", e.getMessage());
      return new int[] {DEFAULT_LOW_MAX, DEFAULT_MEDIUM_MAX, DEFAULT_HIGH_MAX};
    }
  }

  /**
   * Build a structured, UI-legible explanation of a score: the four-band
   * threshold legend (with the active band flagged) plus each risk factor's
   * weighted contribution. The factor weighting mirrors the
   * {@code impactLevel * probability / 10} term summed in
   * {@link #calculateBaseRiskScore}, so the numbers are derived from the real
   * scoring pipeline rather than fabricated.
   *
   * <p>
   * Package-private for unit testing.
   */
  RiskScoreExplanation buildScoreExplanation(int score, List<RiskFactor> factors) {
    int[] b = resolveBandBoundaries();
    int lowMax = b[0];
    int mediumMax = b[1];
    int highMax = b[2];

    RiskLevel active = determineRiskLevel(score);

    List<RiskScoreExplanation.RiskBand> bands = new ArrayList<>();
    bands.add(band(RiskLevel.LOW, 0, lowMax, active));
    bands.add(band(RiskLevel.MEDIUM, lowMax + 1, mediumMax, active));
    bands.add(band(RiskLevel.HIGH, mediumMax + 1, highMax, active));
    bands.add(band(RiskLevel.CRITICAL, highMax + 1, 100, active));

    List<RiskScoreExplanation.FactorContribution> contributions = (factors == null
        ? Collections.<RiskFactor>emptyList()
        : factors).stream()
            .map(f -> RiskScoreExplanation.FactorContribution.builder().category(f.getCategory())
                .description(f.getDescription()).impactLevel(f.getImpactLevel())
                .probability(f.getProbability()).weightedPoints(weightedPoints(f)).build())
            // Heaviest contributors first so the UI surfaces what drives the score.
            .sorted(Comparator.comparing(RiskScoreExplanation.FactorContribution::getWeightedPoints,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());

    return RiskScoreExplanation.builder().score(score).activeBand(active).bands(bands)
        .factorContributions(contributions).build();
  }

  private RiskScoreExplanation.RiskBand band(RiskLevel level, int min, int max, RiskLevel active) {
    return RiskScoreExplanation.RiskBand.builder().level(level).minScore(min).maxScore(max)
        .active(level == active).build();
  }

  private Double weightedPoints(RiskFactor f) {
    int impact = f.getImpactLevel() != null ? f.getImpactLevel() : 0;
    int probability = f.getProbability() != null ? f.getProbability() : 0;
    return Math.round((impact * probability / 10.0) * 10.0) / 10.0;
  }

  private int calculateOverallRiskScore(List<PitchRiskDTO> pitchRisks) {
    if (pitchRisks.isEmpty())
      return 0;

    // Weighted average: high-risk pitches contribute more
    double totalWeight = 0;
    double weightedSum = 0;

    for (PitchRiskDTO risk : pitchRisks) {
      double weight = switch (risk.getRiskLevel()) {
        case CRITICAL -> 3.0;
        case HIGH -> 2.0;
        case MEDIUM -> 1.5;
        case LOW -> 1.0;
      };
      weightedSum += risk.getRiskScore() * weight;
      totalWeight += weight;
    }

    return (int) (weightedSum / totalWeight);
  }

  private PitchRiskSummary toPitchRiskSummary(PitchRiskDTO risk) {
    String topRisk = risk.getRiskFactors().isEmpty()
        ? "No significant risks"
        : risk.getRiskFactors().get(0).getDescription();

    return PitchRiskSummary.builder().pitchId(risk.getPitchId()).pitchTitle(risk.getPitchTitle())
        .riskScore(risk.getRiskScore()).riskLevel(risk.getRiskLevel()).topRisk(topRisk).build();
  }

  private List<CommonRiskFactor> aggregateRiskFactors(List<PitchRiskDTO> pitchRisks) {
    Map<RiskFactor.RiskCategory, List<RiskFactor>> groupedFactors = pitchRisks.stream()
        .flatMap(r -> r.getRiskFactors().stream()).collect(Collectors.groupingBy(RiskFactor::getCategory));

    return groupedFactors.entrySet().stream()
        .map(entry -> CommonRiskFactor.builder().category(entry.getKey())
            .occurrenceCount(entry.getValue().size())
            .averageImpact(
                entry.getValue().stream().mapToDouble(RiskFactor::getImpactLevel).average().orElse(0.0))
            .build())
        .sorted(Comparator.comparing(CommonRiskFactor::getOccurrenceCount).reversed()).limit(5)
        .collect(Collectors.toList());
  }

  private CycleRiskStats calculateCycleStats(List<PitchRiskDTO> pitchRisks, List<Pitch> pitches) {
    double avgRisk = pitchRisks.stream().mapToInt(PitchRiskDTO::getRiskScore).average().orElse(0.0);

    int maxRisk = pitchRisks.stream().mapToInt(PitchRiskDTO::getRiskScore).max().orElse(0);

    int minRisk = pitchRisks.stream().mapToInt(PitchRiskDTO::getRiskScore).min().orElse(0);

    // Calculate average progress
    double totalProgress = 0;
    double totalAppetite = 0;
    double totalActual = 0;

    for (Pitch pitch : pitches) {
      Double hours = workLogRepository.getTotalHoursByPitchId(pitch.getId());
      if (hours == null)
        hours = 0.0;
      double appetite = capacityConfigService.calculatePitchAppetiteHours(pitch);
      totalActual += hours;
      totalAppetite += appetite;
      if (appetite > 0) {
        totalProgress += (hours / appetite) * 100;
      }
    }

    double avgProgress = pitches.isEmpty() ? 0 : totalProgress / pitches.size();
    double appetiteUtilization = totalAppetite > 0 ? (totalActual / totalAppetite) * 100 : 0;

    return CycleRiskStats.builder().averageRiskScore(avgRisk).maxRiskScore(maxRisk).minRiskScore(minRisk)
        .averageProgress(avgProgress).appetiteUtilization(appetiteUtilization).build();
  }

  private CycleAIAnalysisResult generateCycleAIInsights(Cycle cycle, List<PitchRiskDTO> pitchRisks,
      CycleRiskStats stats) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("You are a project portfolio risk advisor analyzing a Shape Up development cycle.\n\n");
    prompt.append("Cycle: ").append(cycle.getName()).append("\n");
    prompt.append("Total Pitches: ").append(pitchRisks.size()).append("\n");
    prompt.append("Average Risk Score: ").append(String.format("%.1f", stats.getAverageRiskScore())).append("\n");
    prompt.append("Average Progress: ").append(String.format("%.1f", stats.getAverageProgress())).append("%\n");
    prompt.append("Appetite Utilization: ").append(String.format("%.1f", stats.getAppetiteUtilization()))
        .append("%\n\n");

    prompt.append("High-risk pitches:\n");
    pitchRisks.stream().filter(r -> r.getRiskLevel() == RiskLevel.HIGH || r.getRiskLevel() == RiskLevel.CRITICAL)
        .forEach(r -> prompt.append("- ").append(r.getPitchTitle()).append(" (Risk: ").append(r.getRiskScore())
            .append(")\n"));

    prompt.append("\nProvide cycle-level insights and recommendations:\n");
    prompt.append("INSIGHTS:\n- [insight 1]\n- [insight 2]\n\n");
    prompt.append("RECOMMENDATIONS:\n- [recommendation 1]\n- [recommendation 2]\n");

    String response = chatLanguageModel.generate(prompt.toString());

    List<String> insights = new ArrayList<>();
    List<String> recommendations = new ArrayList<>();

    String[] sections = response.split("(?i)(INSIGHTS:|RECOMMENDATIONS:)");
    for (int i = 1; i < sections.length; i++) {
      String section = sections[i].trim();
      if (i == 1) {
        insights = extractBulletPoints(section);
      } else if (i == 2) {
        recommendations = extractBulletPoints(section);
      }
    }

    return new CycleAIAnalysisResult(insights, recommendations);
  }

  private List<String> generateRuleBasedCycleInsights(List<PitchRiskDTO> pitchRisks, CycleRiskStats stats,
      int highRiskCount) {
    List<String> insights = new ArrayList<>();

    if (highRiskCount > 0) {
      insights.add(String.format("%d pitch(es) are at high risk and need immediate attention.", highRiskCount));
    }

    if (stats.getAverageProgress() < 50 && stats.getAppetiteUtilization() > 70) {
      insights.add("The cycle is consuming appetite faster than delivering progress - consider scope review.");
    }

    if (stats.getAverageRiskScore() > 60) {
      insights.add("The overall cycle risk is elevated - proactive management recommended.");
    } else if (stats.getAverageRiskScore() < 30) {
      insights.add("The cycle is progressing well with low overall risk.");
    }

    if (insights.isEmpty()) {
      insights.add("The cycle is progressing within normal parameters.");
    }

    return insights;
  }

  private List<String> generateRuleBasedCycleRecommendations(List<CommonRiskFactor> topFactors, int highRiskCount) {
    List<String> recommendations = new ArrayList<>();

    if (highRiskCount > 0) {
      recommendations.add("Schedule a risk review meeting to address high-risk pitches.");
    }

    for (CommonRiskFactor factor : topFactors) {
      if (factor.getOccurrenceCount() > 1) {
        switch (factor.getCategory()) {
          case TIME_OVERRUN -> recommendations
              .add("Multiple pitches face time pressure - consider a cycle-wide scope review.");
          case RESOURCE_CONSTRAINT ->
            recommendations.add("Address team assignment gaps across multiple pitches.");
          case PROGRESS_STAGNATION -> recommendations.add("Investigate blockers affecting multiple pitches.");
          default -> {
          }
        }
      }
    }

    if (recommendations.isEmpty()) {
      recommendations.add("Continue regular monitoring and maintain current practices.");
    }

    return recommendations.stream().distinct().limit(5).collect(Collectors.toList());
  }

  /**
   * Create a hash of pitch data for change detection. When this hash changes,
   * cached results should be invalidated.
   */
  private String createPitchDataHash(Pitch pitch, Double totalHours, double progress, double cycleProgress) {
    String cycleStart = "";
    String cycleEnd = "";
    if (pitch.getCycle() != null) {
      if (pitch.getCycle().getStartDate() != null) cycleStart = pitch.getCycle().getStartDate().toString();
      if (pitch.getCycle().getEndDate() != null) cycleEnd = pitch.getCycle().getEndDate().toString();
    }
    String data = String.format("%d|%s|%.2f|%.2f|%.2f|%s|%s|%s", pitch.getId(), pitch.getStatus(), totalHours,
        progress, cycleProgress, pitch.getUpdatedAt() != null ? pitch.getUpdatedAt().toString() : "",
        cycleStart, cycleEnd);

    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1)
          hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception e) {
      // Fallback to simple hash
      return Integer.toHexString(data.hashCode());
    }
  }

  /** Invalidate cache for a specific pitch (called when pitch data changes). */
  public void invalidatePitchCache(Long pitchId) {
    cacheService.invalidatePitchRiskCache(pitchId);

    // Also invalidate cycle cache if pitch belongs to a cycle
    pitchRepository.findById(pitchId).ifPresent(pitch -> {
      if (pitch.getCycle() != null) {
        cacheService.invalidateCycleRiskCache(pitch.getCycle().getId());
      }
    });
  }

  /** Invalidate cache for a specific cycle (called when cycle data changes). */
  public void invalidateCycleCache(Long cycleId) {
    cacheService.invalidateCycleRiskCache(cycleId);
  }

  /**
   * Get risk history for a pitch within a date range.
   *
   * @param pitchId
   *            The pitch ID
   * @param days
   *            Number of days to look back (default: 30)
   * @return List of risk history entries
   */
  @Transactional(readOnly = true)
  public List<PitchRiskHistory> getRiskHistory(Long pitchId, Integer days) {
    if (days == null || days <= 0) {
      days = 30; // Default to last 30 days
    }

    LocalDateTime startDate = LocalDateTime.now().minusDays(days);
    LocalDateTime endDate = LocalDateTime.now();

    return riskHistoryRepository.findByPitchIdAndDateRange(pitchId, startDate, endDate);
  }

  /**
   * Scheduled job to take daily risk snapshots of all active pitches. Runs at 2
   * AM daily.
   */
  @Scheduled(cron = "0 0 2 * * *") // 2 AM every day
  @Transactional(readOnly = false)
  public void takeScheduledRiskSnapshots() {
    log.info("Starting scheduled risk snapshot job");

    try {
      // Find all active pitches from active cycles
      List<Pitch> activePitches = pitchRepository.findAll().stream()
          .filter(p -> p.getCycle() != null && p.getCycle().getIsActive())
          .filter(p -> p.getStatus() != PitchStatus.DONE && p.getStatus() != PitchStatus.CANCELLED)
          .collect(Collectors.toList());

      log.info("Found {} active pitches for risk snapshot", activePitches.size());

      // Get pitches that already have a snapshot today to avoid duplicates
      LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
      LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
      List<Long> pitchesWithTodaySnapshot = riskHistoryRepository.findPitchIdsWithHistoryToday(startOfDay,
          endOfDay);

      int snapshotCount = 0;
      for (Pitch pitch : activePitches) {
        // Skip if already snapshotted today
        if (pitchesWithTodaySnapshot.contains(pitch.getId())) {
          log.debug("Skipping pitch {} - already snapshotted today", pitch.getId());
          continue;
        }

        try {
          // Analyze risk (without AI for scheduled job to keep it fast)
          PitchRiskDTO riskDTO = analyzePitchRisk(pitch, false);

          // Save to history with SCHEDULED trigger
          riskHistoryService.saveRiskHistory(pitch, riskDTO, PitchRiskHistory.TriggerType.SCHEDULED);
          snapshotCount++;

        } catch (Exception e) {
          log.error("Failed to snapshot risk for pitch {}: {}", pitch.getId(), e.getMessage());
        }
      }

      log.info("Completed scheduled risk snapshot job. Processed {} pitches", snapshotCount);

    } catch (Exception e) {
      log.error("Scheduled risk snapshot job failed: {}", e.getMessage(), e);
    }
  }

  // Inner classes for AI results
  private record AIAnalysisResult(List<String> insights, List<String> recommendations, int confidenceScore) {
  }

  private record CycleAIAnalysisResult(List<String> insights, List<String> recommendations) {
  }
}
