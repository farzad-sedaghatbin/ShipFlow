package com.example.plugin;

import com.github.farzadsedaghatbin.shipflow.plugin.RiskCalculatorPlugin;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Example risk calculator that elevates risk when fewer than 7 days remain
 * and less than half the tasks are complete.
 */
@Component
public class SampleDeadlineRiskPlugin implements RiskCalculatorPlugin {

  @Override
  public String getPluginId() {
    return "sample-deadline-risk";
  }

  @Override
  public String getDisplayName() {
    return "Deadline Crunch Risk";
  }

  @Override
  public Double calculateRisk(Map<String, Object> context) {
    Integer daysRemaining = (Integer) context.get("daysRemaining");
    Integer taskCount = (Integer) context.get("taskCount");
    Integer completed = (Integer) context.get("completedTaskCount");

    if (taskCount == null || taskCount == 0 || daysRemaining == null) {
      return null; // abstain
    }

    double progress = completed == null ? 0.0 : (double) completed / taskCount;
    if (daysRemaining < 7 && progress < 0.5) {
      return 0.85; // high risk
    }
    return 0.15; // low risk
  }
}
