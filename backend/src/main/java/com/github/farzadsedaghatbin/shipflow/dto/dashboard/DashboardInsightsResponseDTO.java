package com.github.farzadsedaghatbin.shipflow.dto.dashboard;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardInsightsResponseDTO {
  /** List of proactive alerts computed from DB metrics. */
  private List<DashboardInsightDTO> insights;

  /** 1-2 sentence AI narrative summary. Null when no LLM is configured. */
  private String aiSummary;

  /** Timestamp when the insights were computed (before Redis caching). */
  private LocalDateTime computedAt;

  /** Whether an LLM is configured for this instance. */
  private boolean aiEnabled;
}
