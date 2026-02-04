package com.github.farzadsedaghatbin.shipflow.dto.health;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

/**
 * DTO representing a stagnating pitch alert.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StagnationAlertDTO {

  private Long pitchId;
  private String pitchTitle;
  private Long cycleId;
  private String cycleName;
  private StagnationType stagnationType;
  private String description;
  private int daysSinceActivity;
  private double hillChartPosition;
  private LocalDateTime lastActivityAt;
  private List<String> stagnantScopes;
  private List<String> recommendations;
  private AlertSeverity severity;

  public enum StagnationType {
    /** No hill chart movement for extended period */
    HILL_CHART_STALLED,
    /** Pitch stuck at the peak of hill chart (decision point) */
    STUCK_AT_PEAK,
    /** No work logs recorded for extended period */
    NO_RECENT_WORK,
    /** Status hasn't changed despite time passing */
    STATUS_UNCHANGED,
    /** Budget nearly exhausted but progress is low */
    BUDGET_EXHAUSTED_LOW_PROGRESS,
    /** Multiple stagnation indicators present */
    COMPOUND_STAGNATION
  }

  public enum AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
  }

  /**
   * Calculate severity based on stagnation factors.
   */
  public static AlertSeverity calculateSeverity(int daysSinceActivity, double hillChartPosition, 
      double cycleProgressPercent) {
    // Critical if late in cycle with minimal progress
    if (cycleProgressPercent > 75 && hillChartPosition < 50 && daysSinceActivity > 5) {
      return AlertSeverity.CRITICAL;
    }
    // Warning if moderate stagnation
    if (daysSinceActivity > 7 || (cycleProgressPercent > 50 && hillChartPosition < 30)) {
      return AlertSeverity.WARNING;
    }
    return AlertSeverity.INFO;
  }
}
