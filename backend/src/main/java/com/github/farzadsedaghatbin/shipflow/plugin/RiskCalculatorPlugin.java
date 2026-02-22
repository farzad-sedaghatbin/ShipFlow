package com.github.farzadsedaghatbin.shipflow.plugin;

import java.util.Map;

/**
 * Extension point for custom risk calculation logic.
 * Implementations can augment the built-in AI-based risk analysis.
 */
public interface RiskCalculatorPlugin {

  /** Unique identifier for this plugin. */
  String getPluginId();

  /** Human-readable name shown in the UI / admin panel. */
  String getDisplayName();

  /**
   * Calculate a risk score for a pitch.
   *
   * @param context a map containing pitch metadata:
   *   - "pitchId" (Long)
   *   - "pitchTitle" (String)
   *   - "appetiteDays" (Integer)
   *   - "cycleId" (Long)
   *   - "taskCount" (Integer)
   *   - "completedTaskCount" (Integer)
   *   - "daysRemaining" (Integer)
   * @return a score in the range [0.0, 1.0], or null to skip
   */
  Double calculateRisk(Map<String, Object> context);
}
