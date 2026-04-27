package com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for asking a follow-up question about a generated solution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpQuestionDTO {

    /**
     * Session ID from the original solution generation.
     */
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    /**
     * The follow-up question from the user.
     */
    @NotBlank(message = "Question is required")
    private String question;
}
