package com.github.farzadsedaghatbin.shipflow.dto.qa;

import com.github.farzadsedaghatbin.shipflow.entity.enums.QAFeedbackType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for submitting feedback on a Q&A response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QAFeedbackRequest {

    @NotNull(message = "Interaction ID is required")
    private Long interactionId;

    @NotNull(message = "Feedback type is required")
    private QAFeedbackType feedbackType;

    /**
     * User's correction or additional notes.
     * Required when feedbackType is CORRECTED.
     */
    @Size(max = 5000, message = "Correction must be at most 5000 characters")
    private String correction;
}
