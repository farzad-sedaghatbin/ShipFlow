package com.github.farzadsedaghatbin.shipflow.dto.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for AI Risk Advisor Q&A.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskQuestionResponse {
    
    private Long pitchId;
    private String pitchTitle;
    private String question;
    private String answer;
    private int confidenceScore;
    private boolean aiEnabled;
    private LocalDateTime answeredAt;
    private String errorMessage;
}
