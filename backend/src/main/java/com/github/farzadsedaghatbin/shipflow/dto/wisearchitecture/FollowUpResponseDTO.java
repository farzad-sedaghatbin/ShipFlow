package com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a follow-up question answer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpResponseDTO {

    /**
     * The answer to the user's question.
     */
    private String answer;

    /**
     * Whether the question was asking for code implementation.
     */
    private Boolean isCodeRequest;

    /**
     * Ready-to-use prompt for Copilot or other AI assistants.
     * Only populated when isCodeRequest is true.
     */
    private String copilotPrompt;

    /**
     * Session ID for continued conversation.
     */
    private String sessionId;

    /**
     * Any clarifying questions the AI wants to ask back.
     */
    private String clarifyingQuestion;
}
