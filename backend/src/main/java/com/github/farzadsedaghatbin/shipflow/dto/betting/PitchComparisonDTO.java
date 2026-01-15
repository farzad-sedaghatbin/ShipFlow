package com.github.farzadsedaghatbin.shipflow.dto.betting;

import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import lombok.*;

import java.util.List;

/**
 * DTO for pitch comparison view with risk and capacity analysis
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchComparisonDTO {
    private PitchDTO pitch;
    
    // Risk indicators
    private Integer appetiteDays;
    private String complexityLevel; // "LOW", "MEDIUM", "HIGH"
    private List<String> risks;
    private List<String> rabbitHoles;
    
    // Team fit analysis
    private List<TeamFitAnalysis> teamFitScores;
    
    // Estimated impact
    private String estimatedBusinessValue; // "HIGH", "MEDIUM", "LOW"
    private String urgency; // "CRITICAL", "HIGH", "MEDIUM", "LOW"
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamFitAnalysis {
        private Long teamId;
        private String teamName;
        private Integer availableCapacityWeeks;
        private Boolean canFit;
        private String capacityStatus; // "AVAILABLE", "TIGHT", "OVER_ALLOCATED"
        private String recommendation; // "GOOD_FIT", "POSSIBLE_WITH_TRADEOFFS", "NOT_RECOMMENDED"
        private List<String> warnings;
    }
}
