package com.github.farzadsedaghatbin.shipflow.plugin.builtin;

import com.github.farzadsedaghatbin.shipflow.plugin.RiskCalculatorPlugin;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Built-in risk calculator: scores pitches by deadline pressure and task completion ratio.
 * Always active — can be disabled via the admin Plugins page.
 */
@Component
public class DeadlinePressureRiskPlugin implements RiskCalculatorPlugin {

  @Override
  public String getPluginId() {
    return "builtin-deadline-pressure";
  }

  @Override
  public String getDisplayName() {
    return "Deadline Pressure Risk";
  }

  @Override
  public Double calculateRisk(Map<String, Object> context) {
    Integer taskCount = (Integer) context.getOrDefault("taskCount", 0);
    Integer completedCount = (Integer) context.getOrDefault("completedTaskCount", 0);
    Integer daysRemaining = (Integer) context.getOrDefault("daysRemaining", 30);
    Integer appetiteDays = (Integer) context.getOrDefault("appetiteDays", 14);

    if (taskCount == null || taskCount == 0 || appetiteDays == null || appetiteDays == 0) {
      return null;
    }

    double completionRatio = (double) completedCount / taskCount;
    double timeRatio = daysRemaining == null ? 0.5
        : (double) daysRemaining / appetiteDays;

    // If completion is lagging well behind the time consumed, risk is high
    double timeConsumed = 1.0 - timeRatio;
    double gap = timeConsumed - completionRatio;

    if (gap > 0.5) return 0.9;
    if (gap > 0.3) return 0.7;
    if (gap > 0.1) return 0.4;
    return 0.15;
  }
}
