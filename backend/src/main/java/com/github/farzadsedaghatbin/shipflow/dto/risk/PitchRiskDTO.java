package com.github.farzadsedaghatbin.shipflow.dto.risk;

import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for pitch risk analysis result.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchRiskDTO {
    
    private Long pitchId;
    private String pitchTitle;
    
    /**
     * Overall risk score (0-100).
     * Thresholds are configurable via OrganizationSettings:
     * - LOW: score <= lowMax (default: 30)
     * - MEDIUM: score > lowMax && <= mediumMax (default: 31-60)
     * - HIGH: score > mediumMax && <= highMax (default: 61-85)
     * - CRITICAL: score > highMax (default: > 85)
     */
    private Integer riskScore;
    
    /**
     * Risk level category derived from score using configurable thresholds.
     */
    private RiskLevel riskLevel;
    
    /**
     * List of specific risk factors identified.
     */
    private List<RiskFactor> riskFactors;
    
    /**
     * AI-generated insights and recommendations.
     */
    private List<String> insights;
    
    /**
     * AI-generated recommendations to mitigate risks.
     */
    private List<String> recommendations;
    
    /**
     * Confidence score of the analysis (0-100).
     */
    private Integer confidenceScore;
    
    /**
     * Timestamp when the analysis was performed.
     */
    private LocalDateTime analyzedAt;
    
    /**
     * Whether the AI feature is enabled.
     */
    private boolean aiEnabled;
    
    /**
     * Error message if analysis failed.
     */
    private String errorMessage;
}
