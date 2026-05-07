package com.github.farzadsedaghatbin.shipflow.dto.signal;

import java.time.LocalDate;
import java.util.List;
import lombok.*;

/**
 * Signal DTO for appetite accuracy trend analysis.
 * Shows how well teams estimate their work over multiple cycles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppetiteAccuracySignalDTO {

  /** Overall trend direction: IMPROVING, DECLINING, STABLE */
  private TrendDirection trend;

  /** Average variance percentage across analyzed cycles (positive = over budget) */
  private Double averageVariancePercent;

  /** Standard deviation of variance - lower means more consistent */
  private Double varianceStdDev;

  /** Number of cycles analyzed */
  private Integer cyclesAnalyzed;

  /** Per-cycle breakdown */
  private List<CycleAccuracyData> cycleData;

  /** Plain-language interpretation */
  private String interpretation;

  /** Actionable recommendations */
  private List<String> recommendations;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CycleAccuracyData {
    private Long cycleId;
    private String cycleName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double totalAppetiteHours;
    private Double totalActualHours;
    private Double varianceHours;
    private Double variancePercent;
    private Integer pitchCount;
    private Integer overBudgetCount;
    private Integer underBudgetCount;
  }

  public enum TrendDirection {
    IMPROVING,   // Getting more accurate
    DECLINING,   // Getting less accurate
    STABLE,      // Staying consistent
    INSUFFICIENT_DATA
  }
}
