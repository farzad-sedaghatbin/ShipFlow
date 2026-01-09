package com.github.farzadsedaghatbin.shipflow.dto.feedback;

import lombok.*;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating risk feedback.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRiskFeedbackRequest {
    
    @NotNull(message = "Pitch ID is required")
    private Long pitchId;
    
    @NotNull(message = "Original risk score is required")
    private Integer originalRiskScore;
    
    @NotNull(message = "Rating is required")
    private RiskFeedbackDTO.FeedbackRating rating;
    
    /**
     * User's suggested risk score (optional)
     */
    private Integer suggestedRiskScore;
    
    /**
     * User's notes explaining their feedback
     */
    private String notes;
    
    /**
     * What the AI missed or got wrong
     */
    private String missedFactors;
}
