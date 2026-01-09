package com.github.farzadsedaghatbin.shipflow.dto.qa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for asking a question.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AskQuestionRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 2000, message = "Question must be at most 2000 characters")
    private String question;

    /**
     * Type of context (pitch, meeting, team, cycle).
     * Optional - if not provided, searches across all knowledge.
     */
    private String contextType;

    /**
     * ID of the context entity.
     * Optional - if not provided with contextType, searches across all of that type.
     */
    private Long contextId;

    /**
     * Optional cycle ID to scope the search.
     */
    private Long cycleId;

    /**
     * Optional team ID to scope the search.
     */
    private Long teamId;

    /**
     * Whether to include sources/citations in the response.
     */
    @Builder.Default
    private Boolean includeSources = true;
    
    /**
     * Conversation ID for multi-turn conversations.
     * Optional - if not provided, starts a new conversation.
     */
    private String conversationId;
}
