package com.github.farzadsedaghatbin.shipflow.dto.betting;

import lombok.*;

/** DTO containing historical performance metrics for a team */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamPerformanceHistoryDTO {
  private Long teamId;
  private String teamName;

  // Historical performance (last completed cycle)
  private Integer lastCycleTotalBets;
  private Integer lastCycleCompletedBets;
  private Double lastCycleCompletionRate; // Percentage

  // All-time performance
  private Integer totalCycles;
  private Integer totalBets;
  private Integer totalCompletedBets;
  private Double overallCompletionRate; // Percentage

  // Average metrics
  private Double avgBetsPerCycle;
  private Double
      avgWeeksPerBet; // Calculated from actual cycle durations divided by bet counts (nullable if
  // no completed cycles)
  private Double
      avgTimeOverrun; // Percentage: (actual hours - appetite hours) / appetite hours * 100,
  // averaged across completed bets (nullable if no completed bets)

  // Recent trends (last 3 cycles)
  private String trend; // "IMPROVING", "STABLE", "DECLINING"
  private String performanceRating; // "EXCELLENT", "GOOD", "FAIR", "POOR"
}
