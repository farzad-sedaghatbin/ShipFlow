package com.github.farzadsedaghatbin.shipflow.dto.hillchart;

import java.util.List;
import lombok.*;

/** DTO for hill chart confidence analysis. Compares user confidence vs actual progress. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfidenceAnalysisDTO {

  private Long pitchId;
  private String pitchTitle;

  /** Average confidence level reported by users */
  private Double averageConfidence;

  /** Actual progress based on logged hours */
  private Double actualProgress;

  /**
   * Difference between confidence and reality Positive = overconfident, Negative = underconfident
   */
  private Double confidenceDelta;

  /** Accuracy assessment */
  private String accuracyAssessment;

  /** History of position changes */
  private List<HillChartHistoryDTO> history;

  /** Analysis insights */
  private List<String> insights;
}
