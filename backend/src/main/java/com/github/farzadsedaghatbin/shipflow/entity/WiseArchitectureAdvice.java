package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity for storing WISE Architecture advice history.
 * Tracks AI-generated technical solutions for pitches, 
 * including follow-up conversations.
 */
@Entity
@Table(name = "wise_architecture_advice", indexes = {
    @Index(name = "idx_advice_pitch_id", columnList = "pitch_id"),
    @Index(name = "idx_advice_user_id", columnList = "user_id"),
    @Index(name = "idx_advice_created_at", columnList = "created_at"),
    @Index(name = "idx_advice_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_advice_user_type_created", columnList = "user_id, message_type, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WiseArchitectureAdvice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique conversation ID to group related messages. */
    @Column(name = "conversation_id", length = 100, nullable = false)
    private String conversationId;

    /** The pitch this advice is for. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitch_id", nullable = false)
    private Pitch pitch;

    /** ID of the user who requested the advice. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Type of message: INITIAL_SOLUTION, FOLLOW_UP_QUESTION, FOLLOW_UP_ANSWER. */
    @Column(name = "message_type", length = 30, nullable = false)
    private String messageType;

    /** The user's question (for follow-ups) or summary of request (for initial). */
    @Column(name = "user_message", columnDefinition = "TEXT")
    private String userMessage;

    /** The AI-generated advice/solution. */
    @Column(name = "ai_response", columnDefinition = "TEXT")
    private String aiResponse;

    /** Comma-separated list of tech stacks used in the advice. */
    @Column(name = "tech_stacks", length = 500)
    private String techStacks;

    /** Comma-separated list of repository IDs analyzed. */
    @Column(name = "repository_ids", length = 500)
    private String repositoryIds;

    /** Whether Figma context was included. */
    @Builder.Default
    @Column(name = "has_figma_context", nullable = false)
    private Boolean hasFigmaContext = false;

    /** Whether GitHub code context was included. */
    @Builder.Default
    @Column(name = "has_github_context", nullable = false)
    private Boolean hasGitHubContext = false;

    /** Whether roadmap context was included. */
    @Builder.Default
    @Column(name = "has_roadmap_context", nullable = false)
    private Boolean hasRoadmapContext = false;

    /** Processing time in milliseconds. */
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    /** User feedback: true = helpful, false = not helpful, null = no feedback. */
    @Column(name = "feedback_helpful")
    private Boolean feedbackHelpful;

    /** User feedback text. */
    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

    /** Timestamp when feedback was given. */
    @Column(name = "feedback_at")
    private LocalDateTime feedbackAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
