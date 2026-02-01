package com.github.farzadsedaghatbin.shipflow.dto.feedback;

import java.time.LocalDateTime;
import lombok.*;

/**
 * DTO for AI risk assessment feedback. Allows team leads to mark AI assessments as
 * accurate/inaccurate.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskFeedbackDTO {

  private Long id;
  private Long pitchId;
  private String pitchTitle;
  private Long userId;
  private String userName;

  /** Risk score at the time of feedback */
  private Integer originalRiskScore;

  /** User's assessment of AI accuracy */
  private FeedbackRating rating;

  /** User's suggested risk score (if they disagree) */
  private Integer suggestedRiskScore;

  /** User's notes explaining their feedback */
  private String notes;

  /** What the AI missed or got wrong */
  private String missedFactors;

  /** Timestamp when feedback was submitted */
  private LocalDateTime createdAt;

  public enum FeedbackRating {
    ACCURATE, // AI was correct
    PARTIALLY_ACCURATE, // AI was mostly correct
    INACCURATE, // AI was wrong
    OVERLY_PESSIMISTIC, // AI overestimated risk
    OVERLY_OPTIMISTIC // AI underestimated risk
  }
}
