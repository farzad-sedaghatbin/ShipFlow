package com.github.farzadsedaghatbin.shipflow.dto.narrative;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

/**
 * Complete cycle summary with all narrative sections.
 * Answers: What we bet, What shipped, What we cut, What surprised us.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleSummaryDTO {

  /** Cycle information */
  private Long cycleId;
  private String cycleName;
  private String projectName;
  private LocalDate startDate;
  private LocalDate endDate;
  private String phase;

  /** Narrative sections */
  private CycleNarrativeDTO whatWeBet;
  private CycleNarrativeDTO whatShipped;
  private CycleNarrativeDTO whatWeCut;
  private CycleNarrativeDTO surprises;
  private CycleNarrativeDTO fullSummary;

  /** Quick stats for context */
  private Integer totalPitchesCommitted;
  private Integer pitchesShipped;
  private Integer pitchesCut;
  private Integer pitchesInProgress;
  private Double avergeAppetiteAccuracy;

  /** Generation metadata */
  private LocalDateTime generatedAt;
  private Boolean aiGenerated;

  /** Available export formats */
  private List<String> exportFormats;
}
