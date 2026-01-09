package com.github.farzadsedaghatbin.shipflow.dto.risk;

import lombok.*;

/**
 * Represents a specific risk factor identified in a pitch.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskFactor {
    
    /**
     * Category of the risk factor.
     */
    private RiskCategory category;
    
    /**
     * Description of the specific risk.
     */
    private String description;
    
    /**
     * Impact level of this factor (1-10).
     */
    private Integer impactLevel;
    
    /**
     * Probability of this risk materializing (1-10).
     */
    private Integer probability;
    
    public enum RiskCategory {
        SCOPE_CREEP,
        TIME_OVERRUN,
        RESOURCE_CONSTRAINT,
        TECHNICAL_COMPLEXITY,
        DEPENDENCY_RISK,
        TEAM_CAPACITY,
        UNCLEAR_REQUIREMENTS,
        PROGRESS_STAGNATION,
        APPETITE_MISMATCH,
        OTHER
    }
}
