package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.*;

/**
 * Statistics for action tracking within a retrospective.
 * Part of v0.5 "Insight, Not Metrics".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetroActionStatsDTO {
  private Long retrospectiveId;
  private long totalActionItems;
  private long actedOnCount;
  private long pendingCount;
  private double followThroughRate; // Percentage 0-100
}
