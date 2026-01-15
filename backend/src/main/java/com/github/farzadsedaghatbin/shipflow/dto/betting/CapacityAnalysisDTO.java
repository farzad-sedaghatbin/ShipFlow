package com.github.farzadsedaghatbin.shipflow.dto.betting;

import lombok.*;

import java.util.List;

/**
 * DTO for overall capacity analysis of a betting table
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapacityAnalysisDTO {
    private Long cycleId;
    private String cycleName;
    
    // Current state
    private Integer totalTeams;
    private Integer totalCapacityWeeks;
    private Integer allocatedWeeks;  // Changed from usedCapacityWeeks to match frontend
    private Integer remainingWeeks;  // Changed from availableCapacityWeeks to match frontend
    private Double utilizationRate; // Percentage (0.0 to 1.0)
    
    // Warnings
    private List<CapacityWarning> warnings;
    private Boolean isOverAllocated;
    private Boolean hasConflicts;
    
    // Recommendations
    private List<String> recommendations;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CapacityWarning {
        private String severity; // "CRITICAL", "WARNING", "INFO"
        private Long teamId;
        private String teamName;
        private String message;
        private Double utilizationRate; // Team utilization percentage
        private String type; // "OVER_ALLOCATED", "UNDER_UTILIZED", "TIGHT_SCHEDULE"
    }
}
