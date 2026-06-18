package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.snapshot.ProjectSnapshotDTO;
import com.github.farzadsedaghatbin.shipflow.dto.snapshot.ProjectSnapshotDTO.ShippedCycleDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.CycleNarrative;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.ProjectSnapshot;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BusinessValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.InitiativeStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.NarrativeType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.CycleNarrativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectSnapshotRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds, caches, and persists pre-computed project snapshots.
 *
 * <p>Every AI service (Pitch Writer, Dashboard Insights, Retro Summarizer) calls {@link
 * #buildPromptBlock(Long)} once per LLM invocation to inject a compact (~500-token) project
 * context block without querying raw tables each time.
 *
 * <p>The snapshot is cached in Redis under {@code projectSnapshot} with a 90-minute TTL and also
 * written to the {@code project_snapshots} table for audit/debug purposes.
 */
@Service
@Slf4j
public class ProjectSnapshotService {

  private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy");
  private static final int SAMPLE_CYCLES = 6;
  private static final int RECENT_SHIPPED = 3;
  private static final int MAX_THEMES = 8;
  private static final int MAX_PATTERNS = 4;
  private static final int NARRATIVE_SNIPPET_CHARS = 200;
  private static final int SUMMARY_TRUNCATE_CHARS = 120;

  private final ProjectRepository projectRepository;
  private final CycleRepository cycleRepository;
  private final CycleNarrativeRepository cycleNarrativeRepository;
  private final InitiativeRepository initiativeRepository;
  private final EpicRepository epicRepository;
  private final ProjectSnapshotRepository snapshotRepository;
  private final ObjectMapper objectMapper;

  public ProjectSnapshotService(
      ProjectRepository projectRepository,
      CycleRepository cycleRepository,
      CycleNarrativeRepository cycleNarrativeRepository,
      InitiativeRepository initiativeRepository,
      EpicRepository epicRepository,
      ProjectSnapshotRepository snapshotRepository,
      ObjectMapper objectMapper) {
    this.projectRepository = projectRepository;
    this.cycleRepository = cycleRepository;
    this.cycleNarrativeRepository = cycleNarrativeRepository;
    this.initiativeRepository = initiativeRepository;
    this.epicRepository = epicRepository;
    this.snapshotRepository = snapshotRepository;
    this.objectMapper = objectMapper;
  }

  // ── Public API ────────────────────────────────────────────────────────────

  /**
   * Returns the cached snapshot for the project, computing and storing a fresh one if no cache
   * entry exists yet.
   */
  @Cacheable(value = "projectSnapshot", key = "#projectId")
  @Transactional(readOnly = true)
  public ProjectSnapshotDTO getOrCompute(Long projectId) {
    return computeAndStore(projectId);
  }

  /**
   * Forces a cache eviction then re-computes a fresh snapshot. Call after significant project
   * mutations (cycle closed, initiative added, etc.).
   */
  @CacheEvict(value = "projectSnapshot", key = "#projectId")
  @Transactional
  public ProjectSnapshotDTO refresh(Long projectId) {
    log.info("Cache evicted for projectSnapshot projectId={}, re-computing.", projectId);
    return computeAndStore(projectId);
  }

  /** Evicts the cache entry without re-computing. */
  @CacheEvict(value = "projectSnapshot", key = "#projectId")
  public void evict(Long projectId) {
    log.debug("projectSnapshot cache evicted for projectId={}", projectId);
  }

  /**
   * Returns a compact, ~500-token text block for direct injection into LLM prompts.
   *
   * <p>This method never throws — any failure returns an empty string so that the calling AI
   * service can proceed unaffected.
   */
  @Transactional(readOnly = true)
  public String buildPromptBlock(Long projectId) {
    try {
      ProjectSnapshotDTO snap = getOrCompute(projectId);
      return renderPromptBlock(snap);
    } catch (Exception e) {
      log.warn(
          "Could not build project snapshot for projectId={}: {}", projectId, e.getMessage());
      return "";
    }
  }

  // ── Core computation ──────────────────────────────────────────────────────

  @Transactional
  ProjectSnapshotDTO computeAndStore(Long projectId) {
    // 1. Load project
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

    // 2. Velocity — last SAMPLE_CYCLES completed cycles
    List<Cycle> allCycles = cycleRepository.findByProjectIdOrderByStartDateDesc(projectId);
    List<Cycle> completedCycles =
        allCycles.stream()
            .filter(c -> c.getPhase() != CyclePhase.SHAPING_BUILDING || !Boolean.TRUE.equals(c.getIsActive()))
            .filter(c -> !Boolean.TRUE.equals(c.getIsActive()))
            .limit(SAMPLE_CYCLES)
            .collect(Collectors.toList());

    int cyclesCompleted = completedCycles.size();
    double avgPitchesPerCycle = 0.0;
    double avgCompletionRate = 0.0;
    int typicalAppetiteDays = 14;

    if (!completedCycles.isEmpty()) {
      List<Integer> pitchCounts = new ArrayList<>();
      List<Double> completionRates = new ArrayList<>();
      Map<Integer, Integer> appetiteFrequency = new HashMap<>();

      for (Cycle cycle : completedCycles) {
        List<Pitch> pitches =
            cycle.getPitches().stream()
                .filter(p -> p.getDeletedAt() == null)
                .collect(Collectors.toList());
        pitchCounts.add(pitches.size());

        if (!pitches.isEmpty()) {
          long shipped =
              pitches.stream()
                  .filter(p -> p.getStatus() == PitchStatus.DONE || p.getStatus() == PitchStatus.COOLDOWN)
                  .count();
          completionRates.add((double) shipped / pitches.size());
        }

        for (Pitch p : pitches) {
          if (p.getAppetiteDays() != null) {
            appetiteFrequency.merge(p.getAppetiteDays(), 1, Integer::sum);
          }
        }
      }

      avgPitchesPerCycle =
          pitchCounts.stream().mapToInt(Integer::intValue).average().orElse(0.0);
      avgCompletionRate =
          completionRates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

      // Mode of appetite days
      typicalAppetiteDays =
          appetiteFrequency.entrySet().stream()
              .max(Map.Entry.comparingByValue())
              .map(Map.Entry::getKey)
              .orElse(14);
    }

    // 3. Recent shipped (last RECENT_SHIPPED completed cycles)
    List<ShippedCycleDTO> recentShipped = new ArrayList<>();
    List<Cycle> recentCycles = completedCycles.stream().limit(RECENT_SHIPPED).collect(Collectors.toList());
    for (Cycle cycle : recentCycles) {
      List<Pitch> pitches =
          cycle.getPitches().stream()
              .filter(p -> p.getDeletedAt() == null)
              .collect(Collectors.toList());

      String summary = buildCycleSummary(cycle, pitches);
      String endDateStr =
          cycle.getEndDate() != null ? cycle.getEndDate().format(MONTH_YEAR) : "";

      recentShipped.add(
          ShippedCycleDTO.builder()
              .cycleName(cycle.getName())
              .endDate(endDateStr)
              .pitchCount(pitches.size())
              .summary(summary)
              .build());
    }

    // 4. Active cycle
    List<Cycle> activeCycles = cycleRepository.findByProjectIdAndIsActiveTrue(projectId);
    String activeCycleName = null;
    Integer activeCycleDaysRemaining = null;
    Integer activeCyclePitchCount = null;
    Integer activeCycleCompletionPercent = null;

    if (!activeCycles.isEmpty()) {
      Cycle active = activeCycles.get(0);
      activeCycleName = active.getName();

      if (active.getEndDate() != null) {
        activeCycleDaysRemaining =
            (int) ChronoUnit.DAYS.between(LocalDate.now(), active.getEndDate());
      }

      List<Pitch> activePitches =
          active.getPitches().stream()
              .filter(p -> p.getDeletedAt() == null)
              .collect(Collectors.toList());
      activeCyclePitchCount = activePitches.size();

      if (!activePitches.isEmpty()) {
        long shipped =
            activePitches.stream()
                .filter(p -> p.getStatus() == PitchStatus.DONE || p.getStatus() == PitchStatus.COOLDOWN)
                .count();
        activeCycleCompletionPercent = (int) (shipped * 100 / activePitches.size());
      } else {
        activeCycleCompletionPercent = 0;
      }
    }

    // 5. Roadmap themes
    List<Initiative> initiatives = initiativeRepository.findByProjectIdNotDeleted(projectId);
    List<Epic> epics = epicRepository.findByProjectIdNotDeleted(projectId);

    Set<String> themes = new LinkedHashSet<>();
    initiatives.stream()
        .filter(
            i ->
                i.getStatus() != InitiativeStatus.COMPLETED
                    && i.getStatus() != InitiativeStatus.CANCELLED)
        .map(Initiative::getName)
        .forEach(themes::add);
    epics.stream()
        .filter(
            e ->
                e.getStatus() != EpicStatus.COMPLETED && e.getStatus() != EpicStatus.CANCELLED)
        .map(Epic::getName)
        .forEach(themes::add);

    List<String> roadmapThemes =
        themes.stream().limit(MAX_THEMES).collect(Collectors.toList());

    // 6. Known patterns from retro summaries
    List<CycleNarrative> narratives =
        cycleNarrativeRepository.findByProjectIdOrderByCycleStartDateDesc(
            projectId, PageRequest.of(0, MAX_PATTERNS));
    List<String> knownPatterns =
        narratives.stream()
            .filter(n -> n.getNarrativeType() == NarrativeType.RETROSPECTIVE_SUMMARY)
            .limit(MAX_PATTERNS)
            .map(
                n -> {
                  String content = n.getContent();
                  if (content == null) return "";
                  return content.length() > NARRATIVE_SNIPPET_CHARS
                      ? content.substring(0, NARRATIVE_SNIPPET_CHARS)
                      : content;
                })
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());

    // 7. Risk posture from pitch priorities across completed cycles
    String riskPosture = computeRiskPosture(completedCycles);

    // 8. Build DTO
    ProjectSnapshotDTO dto =
        ProjectSnapshotDTO.builder()
            .projectId(projectId)
            .projectName(project.getName())
            .projectType(project.getProjectType() != null ? project.getProjectType().name() : "SHAPE_UP")
            .computedAt(LocalDateTime.now())
            .cyclesCompleted(cyclesCompleted)
            .avgPitchesPerCycle(avgPitchesPerCycle)
            .avgCompletionRate(avgCompletionRate)
            .typicalAppetiteDays(typicalAppetiteDays)
            .recentShipped(recentShipped)
            .activeCycleName(activeCycleName)
            .activeCycleDaysRemaining(activeCycleDaysRemaining)
            .activeCyclePitchCount(activeCyclePitchCount)
            .activeCycleCompletionPercent(activeCycleCompletionPercent)
            .roadmapThemes(roadmapThemes)
            .knownPatterns(knownPatterns)
            .riskPosture(riskPosture)
            .build();

    // 9. Persist snapshot
    persistSnapshot(project, dto);

    return dto;
  }

  // ── Rendering ─────────────────────────────────────────────────────────────

  /**
   * Renders the DTO as a compact text block suitable for prepending to an LLM prompt.
   * Omits optional lines when there is no data (no blank lines inside the block).
   */
  String renderPromptBlock(ProjectSnapshotDTO snap) {
    StringBuilder sb = new StringBuilder();
    sb.append("=== Project Context ===\n");
    sb.append("Project: ").append(snap.getProjectName()).append(" (").append(snap.getProjectType()).append(")\n");

    sb.append("Velocity: ")
        .append(snap.getCyclesCompleted())
        .append(" cycles · avg ")
        .append(String.format("%.1f", snap.getAvgPitchesPerCycle()))
        .append(" pitches/cycle · ")
        .append(Math.round(snap.getAvgCompletionRate() * 100))
        .append("% completion · typical appetite ")
        .append(snap.getTypicalAppetiteDays())
        .append(" days\n");

    if (snap.getActiveCycleName() != null) {
      sb.append("Active cycle: \"")
          .append(snap.getActiveCycleName())
          .append("\" — ")
          .append(snap.getActiveCyclePitchCount() != null ? snap.getActiveCyclePitchCount() : 0)
          .append(" pitches, ")
          .append(snap.getActiveCycleCompletionPercent() != null ? snap.getActiveCycleCompletionPercent() : 0)
          .append("% done, ")
          .append(snap.getActiveCycleDaysRemaining() != null ? snap.getActiveCycleDaysRemaining() : 0)
          .append(" days remaining\n");
    }

    if (snap.getRecentShipped() != null && !snap.getRecentShipped().isEmpty()) {
      String shipped =
          snap.getRecentShipped().stream()
              .map(
                  c ->
                      c.getCycleName()
                          + " ("
                          + c.getEndDate()
                          + ", "
                          + c.getPitchCount()
                          + " pitches)")
              .collect(Collectors.joining(" · "));
      sb.append("Recently shipped: ").append(shipped).append("\n");
    }

    if (snap.getRoadmapThemes() != null && !snap.getRoadmapThemes().isEmpty()) {
      sb.append("Roadmap themes: ")
          .append(String.join(", ", snap.getRoadmapThemes()))
          .append("\n");
    }

    if (snap.getKnownPatterns() != null && !snap.getKnownPatterns().isEmpty()) {
      sb.append("Known patterns: ")
          .append(String.join(" · ", snap.getKnownPatterns()))
          .append("\n");
    }

    sb.append("Risk posture: ").append(snap.getRiskPosture()).append("\n");
    sb.append("========================\n");
    return sb.toString();
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private String buildCycleSummary(Cycle cycle, List<Pitch> pitches) {
    // Try WHAT_SHIPPED narrative first
    Optional<CycleNarrative> narrative =
        cycleNarrativeRepository
            .findByCycleIdAndNarrativeType(cycle.getId(), NarrativeType.WHAT_SHIPPED);
    if (narrative.isPresent() && narrative.get().getContent() != null) {
      String content = narrative.get().getContent();
      return content.length() > SUMMARY_TRUNCATE_CHARS
          ? content.substring(0, SUMMARY_TRUNCATE_CHARS)
          : content;
    }
    // Fallback: pitch titles joined
    return pitches.stream().map(Pitch::getTitle).collect(Collectors.joining(", "));
  }

  private String computeRiskPosture(List<Cycle> completedCycles) {
    int highCount = 0;
    int mediumCount = 0;
    int lowCount = 0;

    for (Cycle cycle : completedCycles) {
      for (Pitch pitch : cycle.getPitches()) {
        if (pitch.getDeletedAt() != null) continue;
        BusinessValue priority = pitch.getPriority();
        if (priority == BusinessValue.HIGH) {
          highCount++;
        } else if (priority == BusinessValue.MEDIUM) {
          mediumCount++;
        } else if (priority == BusinessValue.LOW) {
          lowCount++;
        }
      }
    }

    int total = highCount + mediumCount + lowCount;
    if (total == 0) {
      return "MEDIUM";
    }
    if (highCount >= mediumCount && highCount >= lowCount) {
      return "HIGH";
    }
    if (lowCount >= mediumCount && lowCount >= highCount) {
      return "LOW";
    }
    return "MEDIUM";
  }

  private void persistSnapshot(Project project, ProjectSnapshotDTO dto) {
    try {
      String json = objectMapper.writeValueAsString(dto);
      snapshotRepository.deleteByProjectId(project.getId());
      ProjectSnapshot snapshot = new ProjectSnapshot();
      snapshot.setProject(project);
      snapshot.setContent(json);
      snapshot.setComputedAt(dto.getComputedAt());
      snapshotRepository.save(snapshot);
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize snapshot DTO for projectId={}: {}", project.getId(), e.getMessage());
    } catch (Exception e) {
      log.warn("Failed to persist snapshot for projectId={}: {}", project.getId(), e.getMessage());
    }
  }
}
