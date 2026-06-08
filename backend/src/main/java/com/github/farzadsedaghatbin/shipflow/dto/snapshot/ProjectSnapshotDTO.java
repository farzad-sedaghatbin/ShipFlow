package com.github.farzadsedaghatbin.shipflow.dto.snapshot;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO representing a pre-computed project snapshot for LLM prompt injection. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSnapshotDTO {

  private Long projectId;
  private String projectName;
  private String projectType;
  private LocalDateTime computedAt;

  // ── Velocity ────────────────────────────────────────────────────────────────
  private int cyclesCompleted;
  private double avgPitchesPerCycle;

  /** 0.0–1.0 ratio of shipped pitches to total pitches across sampled cycles. */
  private double avgCompletionRate;

  /** Most common pitch appetite in days across sampled cycles (default 14). */
  private int typicalAppetiteDays;

  // ── Recently shipped cycles (last 3 completed) ───────────────────────────
  private List<ShippedCycleDTO> recentShipped;

  // ── Active cycle ─────────────────────────────────────────────────────────
  private String activeCycleName;
  private Integer activeCycleDaysRemaining;
  private Integer activeCyclePitchCount;
  private Integer activeCycleCompletionPercent;

  // ── Roadmap ───────────────────────────────────────────────────────────────
  /** Initiative and epic names combined, deduped, limited to 8 items. */
  private List<String> roadmapThemes;

  // ── Patterns ──────────────────────────────────────────────────────────────
  /** Short snippets extracted from retro RETROSPECTIVE_SUMMARY narratives (max 4). */
  private List<String> knownPatterns;

  // ── Risk ─────────────────────────────────────────────────────────────────
  /** Majority risk level across last 6 cycles: "LOW", "MEDIUM", or "HIGH". */
  private String riskPosture;

  // ── Nested ───────────────────────────────────────────────────────────────

  /** Summary of a recently completed cycle for the prompt block. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ShippedCycleDTO {

    private String cycleName;

    /** Formatted as "MMM yyyy", e.g. "May 2026". */
    private String endDate;

    private int pitchCount;

    /** First 120 chars of the WHAT_SHIPPED narrative, or pitch titles joined with ", ". */
    private String summary;
  }
}
