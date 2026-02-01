package com.github.farzadsedaghatbin.shipflow.dto.feedback;

import java.util.List;
import java.util.Map;
import lombok.*;

/** Aggregated statistics for risk feedback. Useful for analyzing AI accuracy over time. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskFeedbackStatsDTO {

  /** Total feedback entries */
  private Long totalFeedback;

  /** Count by rating type */
  private Long accurateCount;

  private Long partiallyAccurateCount;
  private Long inaccurateCount;
  private Long overlyPessimisticCount;
  private Long overlyOptimisticCount;

  /** Accuracy percentage (accurate / total * 100) */
  private Double accuracyPercent;

  /** Average difference between AI score and suggested score */
  private Double averageScoreDelta;

  /** Common missed factors reported by users */
  private List<String> commonMissedFactors;

  /** Feedback breakdown by time period */
  private Map<String, Long> feedbackByMonth;
}
