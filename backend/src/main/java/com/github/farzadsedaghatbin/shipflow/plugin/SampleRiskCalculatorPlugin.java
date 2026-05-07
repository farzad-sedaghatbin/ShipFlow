package com.github.farzadsedaghatbin.shipflow.plugin;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * A sample risk calculator plugin active only in the "dev" profile.
 * Demonstrates how to implement the {@link RiskCalculatorPlugin} SPI.
 *
 * <p>This plugin uses a simple heuristic: if the ratio of completed tasks
 * to total tasks is below 50% and fewer than 5 days remain, it signals
 * high risk. It is intentionally simplistic for demonstration purposes.</p>
 *
 * <p><strong>NOT loaded in production.</strong></p>
 */
@Component
@Profile("dev")
@Slf4j
public class SampleRiskCalculatorPlugin implements RiskCalculatorPlugin {

  @Override
  public String getPluginId() {
    return "sample-risk-heuristic";
  }

  @Override
  public String getDisplayName() {
    return "Sample Heuristic Risk Calculator (DEV only)";
  }

  @Override
  public Double calculateRisk(Map<String, Object> context) {
    Integer taskCount = (Integer) context.getOrDefault("taskCount", 0);
    Integer completedTaskCount = (Integer) context.getOrDefault("completedTaskCount", 0);
    Integer daysRemaining = (Integer) context.getOrDefault("daysRemaining", 30);

    if (taskCount == null || taskCount == 0) {
      return null; // no data – skip
    }

    double completionRatio = (double) completedTaskCount / taskCount;

    // Simple heuristic: high risk if not half done with <5 days left
    if (completionRatio < 0.5 && daysRemaining != null && daysRemaining < 5) {
      log.debug("[SampleRiskPlugin] High risk detected: {}% complete, {} days left",
          Math.round(completionRatio * 100), daysRemaining);
      return 0.85;
    }

    // Moderate risk if <30% complete with <10 days
    if (completionRatio < 0.3 && daysRemaining != null && daysRemaining < 10) {
      return 0.6;
    }

    // Low risk
    return 0.2;
  }
}
