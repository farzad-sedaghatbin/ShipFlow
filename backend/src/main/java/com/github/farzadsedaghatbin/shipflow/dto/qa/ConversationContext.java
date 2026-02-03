package com.github.farzadsedaghatbin.shipflow.dto.qa;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/** Holds conversation history for multi-turn Q&A. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {

  /** Unique conversation ID. */
  private String conversationId;

  /** User ID. */
  private Long userId;

  /** Conversation history (question-answer pairs). */
  @Builder.Default
  private List<Turn> history = new ArrayList<>();

  /** Context type (pitch, meeting, cycle, etc.). */
  private String contextType;

  /** Context entity ID. */
  private Long contextId;

  /** When conversation started. */
  private LocalDateTime startedAt;

  /** When last interaction occurred. */
  private LocalDateTime lastInteractionAt;

  /** Whether conversation is still active. */
  @Builder.Default
  private Boolean isActive = true;

  /** Add a new turn to the conversation. */
  public void addTurn(String question, String answer) {
    this.history.add(new Turn(question, answer, LocalDateTime.now()));
    this.lastInteractionAt = LocalDateTime.now();
  }

  /** Get recent turns (for context building). */
  public List<Turn> getRecentTurns(int count) {
    int size = history.size();
    if (size <= count) {
      return new ArrayList<>(history);
    }
    return new ArrayList<>(history.subList(size - count, size));
  }

  /** Check if conversation has expired (inactive for > 30 minutes). */
  public boolean isExpired() {
    if (lastInteractionAt == null) {
      return true;
    }
    return lastInteractionAt.plusMinutes(30).isBefore(LocalDateTime.now());
  }

  /** A single turn in the conversation. */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Turn {
    private String question;
    private String answer;
    private LocalDateTime timestamp;
  }

  /** Lightweight context info for inferring context in follow-up questions. */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ContextInfo {
    private String contextType;
    private Long contextId;
  }
}
