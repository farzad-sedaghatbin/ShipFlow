package com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for WISE Architecture advice history entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdviceHistoryDTO {

    private Long id;
    private String conversationId;
    private Long pitchId;
    private String pitchTitle;
    private Long userId;
    private String messageType;
    private String userMessage;
    private String aiResponse;
    private List<String> techStacks;
    private List<Long> repositoryIds;
    private Boolean hasFigmaContext;
    private Boolean hasGitHubContext;
    private Boolean hasRoadmapContext;
    private Long processingTimeMs;
    private Boolean feedbackHelpful;
    private String feedbackText;
    private LocalDateTime feedbackAt;
    private LocalDateTime createdAt;

    /**
     * Summary DTO for listing conversations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationSummary {
        private String conversationId;
        private Long pitchId;
        private String pitchTitle;
        private String projectName;
        private List<String> techStacks;
        private int messageCount;
        private LocalDateTime createdAt;
        private LocalDateTime lastMessageAt;
        
        /** Preview of the AI response (first 200 chars). */
        private String responsePreview;
    }

    /**
     * DTO for feedback submission.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackRequest {
        private Boolean helpful;
        private String feedbackText;
    }
}
