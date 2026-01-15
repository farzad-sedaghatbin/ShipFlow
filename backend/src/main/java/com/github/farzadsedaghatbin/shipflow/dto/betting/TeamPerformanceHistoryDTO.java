package com.github.farzadsedaghatbin.shipflow.dto.betting;

import lombok.*;

/**
 * DTO containing historical performance metrics for a team
 */
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
    
    // Average metrics (some may be null if calculation not yet implemented)
    private Double avgBetsPerCycle;
    private Double avgWeeksPerBet; // TODO: Calculate from actual cycle durations and bet counts (nullable until implemented)
    private Double avgTimeOverrun; // TODO: Implement work log analysis to calculate actual vs appetite - Percentage (nullable until implemented)
    
    // Recent trends (last 3 cycles)
    private String trend; // "IMPROVING", "STABLE", "DECLINING"
    private String performanceRating; // "EXCELLENT", "GOOD", "FAIR", "NEEDS_IMPROVEMENT"
}
