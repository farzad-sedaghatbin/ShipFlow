package com.github.farzadsedaghatbin.shipflow.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Q&A answers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QAResponse {

    /**
     * ID of the Q&A interaction (for feedback).
     */
    private Long interactionId;

    /**
     * The original question.
     */
    private String question;

    /**
     * The AI-generated answer.
     */
    private String answer;

    /**
     * List of sources/citations used to generate the answer.
     */
    private List<SourceCitation> sources;

    /**
     * Confidence score (0-100).
     */
    private Integer confidenceScore;

    /**
     * Timestamp of when the answer was generated.
     */
    private LocalDateTime answeredAt;

    /**
     * Processing time in milliseconds.
     */
    private Long processingTimeMs;

    /**
     * Whether AI is enabled for Q&A.
     */
    private Boolean aiEnabled;

    /**
     * Error message if something went wrong.
     */
    private String errorMessage;

    /**
     * Suggested follow-up questions.
     */
    private List<String> suggestedFollowUps;

    /**
     * Whether this response was served from cache.
     * Helps frontend understand response source.
     */
    private Boolean cached;
    
    /**
     * Conversation ID for multi-turn conversations.
     */
    private String conversationId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceCitation {
        private Long knowledgeItemId;
        private String entityType;
        private Long entityId;
        private String title;
        private String snippet;
        private Double relevanceScore;
    }
}
