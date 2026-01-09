package com.github.farzadsedaghatbin.shipflow.dto.risk;

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
     * 0-30: Low risk
     * 31-60: Medium risk
     * 61-100: High risk
     */
    private Integer riskScore;
    
    /**
     * Risk level category derived from score.
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
    
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
