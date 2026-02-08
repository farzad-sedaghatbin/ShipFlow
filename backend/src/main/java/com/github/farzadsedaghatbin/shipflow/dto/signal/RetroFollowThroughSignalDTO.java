package com.github.farzadsedaghatbin.shipflow.dto.signal;

import java.util.List;
import lombok.*;

/**
 * Signal DTO for retrospective follow-through analysis.
 * Tracks "did we act on this?" for action items.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetroFollowThroughSignalDTO {

  /** Overall follow-through rate (acted on / total actions) */
  private Double followThroughRatePercent;

  /** Total action items across analyzed retrospectives */
  private Integer totalActionItems;

  /** Action items marked as acted upon */
  private Integer actedOnCount;

  /** Action items not yet acted upon */
  private Integer pendingCount;

  /** Number of retrospectives analyzed */
  private Integer retrospectivesAnalyzed;

  /** Trend in follow-through over time */
  private AppetiteAccuracySignalDTO.TrendDirection trend;

  /** Unacted items that are high priority */
  private List<UnactedActionItem> highPriorityUnacted;

  /** Recurring themes in unacted items */
  private List<RecurringTheme> recurringThemes;

  /** Per-retrospective breakdown */
  private List<RetroFollowThroughData> retroData;

  /** Plain-language interpretation */
  private String interpretation;

  /** Recommendations for improving follow-through */
  private List<String> recommendations;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class UnactedActionItem {
    private Long retroItemId;
    private String content;
    private String retrospectiveName;
    private String cycleName;
    private Integer voteCount;
    private Integer daysSinceCreated;
    private List<String> tags;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class RecurringTheme {
    private String theme;
    private Integer occurrenceCount;
    private Integer cyclesSpanned;
    private Boolean hasBeenAddressed;
    private List<String> relatedTags;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class RetroFollowThroughData {
    private Long retrospectiveId;
    private String retrospectiveName;
    private Long cycleId;
    private String cycleName;
    private Integer totalActions;
    private Integer actedOnCount;
    private Double followThroughRate;
  }
}
