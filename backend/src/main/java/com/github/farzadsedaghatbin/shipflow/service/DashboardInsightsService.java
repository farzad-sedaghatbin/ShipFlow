package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardInsightDTO;
import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardInsightsResponseDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Computes proactive dashboard insights for a project.
 *
 * <p>Insight categories:
 * <ul>
 *   <li>OVERDUE_PITCHES – in-progress pitches whose cycle has already ended
 *   <li>AT_RISK_CYCLE – active cycles where more than half of pitches are not done and the cycle
 *       ends within 7 days
 *   <li>SCOPE_CREEP – pitches added to a cycle more than 3 days after the cycle started
 *   <li>VELOCITY_TREND – comparison of completed pitches across the last 3 cycles
 * </ul>
 *
 * <p>Results are cached in Redis under {@code dashboardInsights} with a 60-minute TTL (configured
 * in {@link com.github.farzadsedaghatbin.shipflow.config.RedisConfig}).
 */
@Service
@Slf4j
public class DashboardInsightsService {

  private static final List<PitchStatus> IN_PROGRESS_STATUSES =
      List.of(PitchStatus.PENDING, PitchStatus.STARTED, PitchStatus.IN_PROGRESS, PitchStatus.TESTING);

  private static final List<PitchStatus> COMPLETE_STATUSES =
      List.of(PitchStatus.DONE, PitchStatus.COOLDOWN, PitchStatus.CANCELLED, PitchStatus.CIRCUIT_BREAKER);

  private final CycleRepository cycleRepository;
  private final PitchRepository pitchRepository;
  private final ChatLanguageModel chatLanguageModel;

  @Autowired
  public DashboardInsightsService(
      CycleRepository cycleRepository,
      PitchRepository pitchRepository,
      @Autowired(required = false) ChatLanguageModel chatLanguageModel) {
    this.cycleRepository = cycleRepository;
    this.pitchRepository = pitchRepository;
    this.chatLanguageModel = chatLanguageModel;
  }

  /**
   * Returns proactive insights for the given project, cached for 60 minutes.
   *
   * @param projectId the project to analyse
   * @return computed insights response (served from cache on subsequent calls)
   */
  @Cacheable(value = "dashboardInsights", key = "#projectId")
  public DashboardInsightsResponseDTO getInsights(Long projectId) {
    log.debug("Computing dashboard insights for project {}", projectId);

    List<Cycle> allCycles = cycleRepository.findByProjectIdOrderByStartDateDesc(projectId);
    List<DashboardInsightDTO> insights = new ArrayList<>();

    LocalDate today = LocalDate.now();

    // ── 1. Overdue pitches ───────────────────────────────────────────────────
    // Pitches in active work states whose cycle end date has already passed.
    List<Pitch> overduePitches = new ArrayList<>();
    for (Cycle cycle : allCycles) {
      if (cycle.getEndDate() != null && cycle.getEndDate().isBefore(today)) {
        List<Pitch> cyclePitches = pitchRepository.findByCycleIdNotDeleted(cycle.getId());
        for (Pitch pitch : cyclePitches) {
          if (IN_PROGRESS_STATUSES.contains(pitch.getStatus())) {
            overduePitches.add(pitch);
          }
        }
      }
    }
    if (!overduePitches.isEmpty()) {
      insights.add(
          DashboardInsightDTO.builder()
              .type("OVERDUE_PITCHES")
              .severity("HIGH")
              .title("Overdue Pitches")
              .detail(
                  overduePitches.size()
                      + " pitch"
                      + (overduePitches.size() == 1 ? "" : "es")
                      + " still in progress after the cycle end date.")
              .count(overduePitches.size())
              .build());
    }

    // ── 2. At-risk cycles ────────────────────────────────────────────────────
    // Active cycles ending within 7 days that still have more than half their pitches incomplete.
    for (Cycle cycle : allCycles) {
      if (!Boolean.TRUE.equals(cycle.getIsActive())) continue;
      if (cycle.getEndDate() == null) continue;
      long daysLeft = today.until(cycle.getEndDate()).getDays();
      if (daysLeft < 0 || daysLeft > 7) continue;

      List<Pitch> cyclePitches = pitchRepository.findByCycleIdNotDeleted(cycle.getId());
      if (cyclePitches.isEmpty()) continue;

      long notDone =
          cyclePitches.stream()
              .filter(p -> !COMPLETE_STATUSES.contains(p.getStatus()))
              .count();
      double pctIncomplete = (double) notDone / cyclePitches.size();
      if (pctIncomplete > 0.5) {
        insights.add(
            DashboardInsightDTO.builder()
                .type("AT_RISK_CYCLE")
                .severity("MEDIUM")
                .title("Cycle at Risk: " + cycle.getName())
                .detail(
                    notDone
                        + " of "
                        + cyclePitches.size()
                        + " pitches incomplete with "
                        + daysLeft
                        + " day"
                        + (daysLeft == 1 ? "" : "s")
                        + " remaining.")
                .count((int) notDone)
                .targetId(String.valueOf(cycle.getId()))
                .targetRoute("/cycles/" + cycle.getId())
                .build());
      }
    }

    // ── 3. Scope creep ───────────────────────────────────────────────────────
    // Pitches created more than 3 days after their cycle's start date.
    int scopeCreepCount = 0;
    for (Cycle cycle : allCycles) {
      if (cycle.getStartDate() == null) continue;
      LocalDateTime threshold = cycle.getStartDate().plusDays(3).atStartOfDay();
      List<Pitch> cyclePitches = pitchRepository.findByCycleIdNotDeleted(cycle.getId());
      for (Pitch pitch : cyclePitches) {
        if (pitch.getCreatedAt() != null && pitch.getCreatedAt().isAfter(threshold)) {
          scopeCreepCount++;
        }
      }
    }
    if (scopeCreepCount > 0) {
      insights.add(
          DashboardInsightDTO.builder()
              .type("SCOPE_CREEP")
              .severity("MEDIUM")
              .title("Scope Creep Detected")
              .detail(
                  scopeCreepCount
                      + " pitch"
                      + (scopeCreepCount == 1 ? "" : "es")
                      + " added more than 3 days after the cycle started.")
              .count(scopeCreepCount)
              .build());
    }

    // ── 4. Velocity trend ────────────────────────────────────────────────────
    // Compare completed-pitch count in the most recent cycle vs. the two before it.
    List<Cycle> completedCycles =
        allCycles.stream()
            .filter(c -> !Boolean.TRUE.equals(c.getIsActive()))
            .collect(Collectors.toList());

    if (completedCycles.size() >= 3) {
      Cycle latest = completedCycles.get(0);
      Cycle prev1 = completedCycles.get(1);
      Cycle prev2 = completedCycles.get(2);

      long latestDone = countDone(latest);
      long prev1Done = countDone(prev1);
      long prev2Done = countDone(prev2);
      double avgPrior = (prev1Done + prev2Done) / 2.0;

      if (latestDone < avgPrior * 0.8) {
        insights.add(
            DashboardInsightDTO.builder()
                .type("VELOCITY_TREND")
                .severity("LOW")
                .title("Velocity Declining")
                .detail(
                    "Last cycle completed "
                        + latestDone
                        + " pitches vs. an average of "
                        + Math.round(avgPrior)
                        + " over the prior two cycles.")
                .count((int) latestDone)
                .build());
      } else if (latestDone > avgPrior * 1.2) {
        insights.add(
            DashboardInsightDTO.builder()
                .type("VELOCITY_TREND")
                .severity("INFO")
                .title("Velocity Improving")
                .detail(
                    "Last cycle completed "
                        + latestDone
                        + " pitches vs. an average of "
                        + Math.round(avgPrior)
                        + " over the prior two cycles.")
                .count((int) latestDone)
                .build());
      }
    }

    // ── 5. AI narrative summary ──────────────────────────────────────────────
    String aiSummary = null;
    if (chatLanguageModel != null && !insights.isEmpty()) {
      try {
        String insightText =
            insights.stream()
                .map(i -> i.getSeverity() + " – " + i.getTitle() + ": " + i.getDetail())
                .collect(Collectors.joining("; "));
        String prompt =
            "Briefly summarize these project alerts in 1-2 sentences for a project manager: "
                + insightText;
        aiSummary = chatLanguageModel.generate(prompt);
        log.debug("AI summary generated for project {}", projectId);
      } catch (Exception e) {
        log.warn("AI summary generation failed for project {}: {}", projectId, e.getMessage());
      }
    }

    return DashboardInsightsResponseDTO.builder()
        .insights(insights)
        .aiSummary(aiSummary)
        .computedAt(LocalDateTime.now())
        .aiEnabled(chatLanguageModel != null)
        .build();
  }

  /**
   * Evicts the cached insights for the given project and re-computes them immediately.
   *
   * <p>Note: {@code @CacheEvict} runs before the method body, so the subsequent {@code
   * getInsights} call hits the DB and re-populates the cache.
   *
   * @param projectId the project whose cache entry should be evicted
   * @return freshly computed insights
   */
  @CacheEvict(value = "dashboardInsights", key = "#projectId")
  public DashboardInsightsResponseDTO refreshInsights(Long projectId) {
    log.info("Cache evicted for dashboardInsights projectId={}, re-computing.", projectId);
    return getInsights(projectId);
  }

  // ── Private helpers ──────────────────────────────────────────────────────

  private long countDone(Cycle cycle) {
    return pitchRepository.findByCycleIdNotDeleted(cycle.getId()).stream()
        .filter(p -> p.getStatus() == PitchStatus.DONE || p.getStatus() == PitchStatus.COOLDOWN)
        .count();
  }
}
