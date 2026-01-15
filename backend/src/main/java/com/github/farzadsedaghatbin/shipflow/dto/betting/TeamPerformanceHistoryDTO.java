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
    
    // Average metrics
    private Double avgBetsPerCycle;
    private Double avgWeeksPerBet;
    private Double avgTimeOverrun; // Percentage (e.g., 10% means tasks took 10% longer than appetite)
    
    // Recent trends (last 3 cycles)
    private String trend; // "IMPROVING", "STABLE", "DECLINING"
    private String performanceRating; // "EXCELLENT", "GOOD", "FAIR", "NEEDS_IMPROVEMENT"
}
