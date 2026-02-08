package com.github.farzadsedaghatbin.shipflow.dto.signal;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

/**
 * Aggregated signals DTO for a single cycle or across multiple cycles.
 * Provides the complete "Insight, Not Metrics" view.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleSignalsDTO {

  /** Project context */
  private Long projectId;
  private String projectName;

  /** Optional cycle context (null for cross-cycle analysis) */
  private Long cycleId;
  private String cycleName;

  /** Number of cycles included in analysis */
  private Integer cyclesAnalyzed;

  /** Individual signal components */
  private AppetiteAccuracySignalDTO appetiteAccuracy;
  private ShapingPatternSignalDTO shapingPattern;
  private RiskCorrelationSignalDTO riskCorrelation;
  private RetroFollowThroughSignalDTO retroFollowThrough;

  /** Top-level insights (human-readable highlights) */
  private List<String> keyInsights;

  /** Top recommendations across all signals */
  private List<String> topRecommendations;

  /** Overall health score (0-100) based on signal aggregation */
  private Integer overallHealthScore;

  /** When this analysis was generated */
  private LocalDateTime analyzedAt;

  /** Whether AI was used in generating insights */
  private Boolean aiEnhanced;
}
