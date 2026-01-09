package com.github.farzadsedaghatbin.shipflow.dto.risk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for asking questions to the AI Risk Advisor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskQuestionRequest {
    
    @NotBlank(message = "Question is required")
    @Size(min = 5, max = 1000, message = "Question must be between 5 and 1000 characters")
    private String question;
}
