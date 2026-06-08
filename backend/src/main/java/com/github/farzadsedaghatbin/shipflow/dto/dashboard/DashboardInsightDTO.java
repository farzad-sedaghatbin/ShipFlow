package com.github.farzadsedaghatbin.shipflow.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardInsightDTO {
  /** Insight category: OVERDUE_PITCHES, AT_RISK_CYCLE, SCOPE_CREEP, VELOCITY_TREND */
  private String type;

  /** Alert severity: HIGH, MEDIUM, LOW, INFO */
  private String severity;

  private String title;
  private String detail;

  /** Count of affected items (e.g. number of overdue pitches). Nullable. */
  private Integer count;

  /** Cycle or pitch id as string, used for deep-link navigation. Nullable. */
  private String targetId;

  /** Frontend route to navigate on click (e.g. "/cycles/5"). Nullable. */
  private String targetRoute;
}
