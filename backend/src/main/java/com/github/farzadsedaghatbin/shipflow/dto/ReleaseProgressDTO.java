package com.github.farzadsedaghatbin.shipflow.dto;

import java.util.Map;
import lombok.*;

/**
 * DTO for release progress metrics.
 * Contains aggregated statistics about items linked to a release.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseProgressDTO {
  // Overall completion
  private Double completionPercentage;

  // Pitch metrics
  private Integer totalPitches;
  private Integer completedPitches;
  private Integer inProgressPitches;

  // Task metrics
  private Integer totalTasks;
  private Integer completedTasks;
  private Integer inProgressTasks;
  private Integer blockedTasks;

  // Bug metrics
  private Integer totalBugs;
  private Integer resolvedBugs;
  private Integer openBugs;
  private Integer criticalBugs;

  // Bug fix expectation tracking
  private Integer bugsOnTarget; // Fixed in target release
  private Integer bugsSlipped;  // Fixed in later release than target
  private Integer bugsPulledForward; // Fixed earlier than target

  // Bugs by status breakdown
  private Map<String, Integer> bugsByStatus;

  // Scope change tracking
  private Integer scopeChangeCount;
  private Integer itemsAdded;
  private Integer itemsRemoved;

  // Risk indicator
  private String riskLevel;
  private String riskReason;
}
