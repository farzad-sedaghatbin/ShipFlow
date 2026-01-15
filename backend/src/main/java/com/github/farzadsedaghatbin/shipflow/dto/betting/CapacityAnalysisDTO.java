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
    private Integer usedCapacityWeeks;
    private Integer availableCapacityWeeks;
    private Double utilizationRate; // Percentage
    
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
        private String type; // "OVER_ALLOCATED", "UNDER_UTILIZED", "TIGHT_SCHEDULE"
    }
}
