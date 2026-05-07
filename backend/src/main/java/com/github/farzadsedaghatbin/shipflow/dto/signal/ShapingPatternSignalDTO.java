package com.github.farzadsedaghatbin.shipflow.dto.signal;

import java.util.List;
import lombok.*;

/**
 * Signal DTO for shaping pattern analysis.
 * Identifies consistent over-shaping or under-shaping across pitches.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShapingPatternSignalDTO {

  /** Overall shaping quality assessment */
  private ShapingQuality overallQuality;

  /** Count of pitches that were over-shaped (finished well under appetite) */
  private Integer overShapedCount;

  /** Count of pitches that were under-shaped (significantly over appetite) */
  private Integer underShapedCount;

  /** Count of pitches shaped accurately (within tolerance) */
  private Integer accuratelyShapedCount;

  /** Total pitches analyzed */
  private Integer totalPitchesAnalyzed;

  /** Tolerance percentage used for classification (default 15%) */
  private Double tolerancePercent;

  /** Pitches with concerning patterns */
  private List<ShapingPatternPitch> overShapedPitches;
  private List<ShapingPatternPitch> underShapedPitches;

  /** Recurring themes in under/over shaped pitches */
  private List<String> recurringOverShapingThemes;
  private List<String> recurringUnderShapingThemes;

  /** Plain-language interpretation */
  private String interpretation;

  /** Actionable recommendations */
  private List<String> recommendations;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ShapingPatternPitch {
    private Long pitchId;
    private String pitchTitle;
    private Long cycleId;
    private String cycleName;
    private Integer appetiteDays;
    private Double actualHours;
    private Double variancePercent;
    private String status;
  }

  public enum ShapingQuality {
    EXCELLENT,      // Most pitches accurately shaped
    GOOD,           // Majority accurate, few outliers
    NEEDS_ATTENTION, // Significant over or under shaping
    POOR,           // Consistent pattern problems
    INSUFFICIENT_DATA
  }
}
