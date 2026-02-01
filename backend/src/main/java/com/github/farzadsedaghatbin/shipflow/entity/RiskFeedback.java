package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.dto.feedback.RiskFeedbackDTO.FeedbackRating;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity to store user feedback on AI risk assessments. Used for improving AI accuracy over time.
 */
@Entity
@Table(name = "risk_feedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskFeedback {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id", nullable = false)
  private Pitch pitch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** Risk score at the time of feedback */
  @Column(name = "original_risk_score", nullable = false)
  private Integer originalRiskScore;

  /** User's assessment of AI accuracy */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FeedbackRating rating;

  /** User's suggested risk score (if they disagree) */
  @Column(name = "suggested_risk_score")
  private Integer suggestedRiskScore;

  /** User's notes explaining their feedback */
  @Column(columnDefinition = "TEXT")
  private String notes;

  /** What the AI missed or got wrong */
  @Column(name = "missed_factors", columnDefinition = "TEXT")
  private String missedFactors;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
