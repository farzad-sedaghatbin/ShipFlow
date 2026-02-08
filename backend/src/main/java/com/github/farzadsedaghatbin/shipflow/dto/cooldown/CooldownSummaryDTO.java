package com.github.farzadsedaghatbin.shipflow.dto.cooldown;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityType;
import java.util.List;
import java.util.Map;
import lombok.*;

/**
 * Summary DTO for cooldown activities in a cycle.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CooldownSummaryDTO {

  private Long cycleId;
  private String cycleName;

  /** All activities for the cycle */
  private List<CooldownActivityDTO> activities;

  /** Count by activity type */
  private Map<CooldownActivityType, Long> countByType;

  /** Count by status */
  private Map<CooldownActivityStatus, Long> countByStatus;

  /** Total statistics */
  private int totalActivities;
  private int completedCount;
  private int inProgressCount;
  private int plannedCount;
  private int blockedCount;
  private int skippedCount;

  /** Hours tracking */
  private int totalEstimatedHours;
  private int totalActualHours;

  /** Progress percentage (completed / total) */
  private double completionPercentage;

  public static double calculateCompletionPercentage(int completed, int total) {
    if (total == 0) return 0.0;
    return Math.round((completed * 100.0 / total) * 10) / 10.0;
  }
}
