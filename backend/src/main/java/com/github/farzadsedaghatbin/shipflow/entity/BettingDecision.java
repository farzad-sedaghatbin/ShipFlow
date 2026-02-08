package com.github.farzadsedaghatbin.shipflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BettingDecisionType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Represents a betting decision made during the betting meeting for a pitch.
 * Tracks commit/reject decisions with reasoning for historical reference.
 * 
 * This supports Shape Up methodology by:
 * - Recording WHY pitches were chosen or rejected
 * - Comparing appetite vs available capacity
 * - Building institutional knowledge about betting decisions
 */
@Entity
@Table(name = "betting_decisions", 
    indexes = {
        @Index(name = "idx_betting_decision_pitch", columnList = "pitch_id"),
        @Index(name = "idx_betting_decision_cycle", columnList = "cycle_id"),
        @Index(name = "idx_betting_decision_decided_at", columnList = "decided_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_betting_decision_pitch_cycle", columnNames = {"pitch_id", "cycle_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BettingDecision {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "workLogs", "meetings", "evidences"})
  private Pitch pitch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "pitches", "teams", "retrospectives"})
  private Cycle cycle;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BettingDecisionType decision;

  /** The reasoning behind this decision - why commit or why reject */
  @Column(columnDefinition = "TEXT")
  private String reason;

  /** User who made/recorded the decision */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "decided_by_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "projects"})
  private User decidedBy;

  /** When the decision was made */
  @Column(nullable = false)
  private LocalDateTime decidedAt;

  // Appetite comparison fields - for explicit capacity analysis

  /** The appetite requested by the pitch (in days) */
  @Column(nullable = false)
  private Integer requestedAppetiteDays;

  /** Available capacity in the cycle for this team (in days) */
  @Column
  private Integer availableCapacityDays;

  /** Notes about appetite fit - e.g., "Fits well", "Too large for remaining capacity" */
  @Column(columnDefinition = "TEXT")
  private String appetiteComparisonNotes;

  /** Team that was considered for this pitch (may differ from final assignment) */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "considered_team_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "cycle", "members", "pitches"})
  private Team consideredTeam;

  /** Priority score given during betting (for comparing multiple pitches) */
  @Column
  private Integer priorityScore;

  /** Strategic alignment notes - how this pitch fits company/product goals */
  @Column(columnDefinition = "TEXT")
  private String strategicAlignmentNotes;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (decidedAt == null) {
      decidedAt = LocalDateTime.now();
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  /**
   * Check if this was a positive decision (pitch will be worked on).
   */
  public boolean isCommitted() {
    return decision == BettingDecisionType.COMMITTED;
  }

  /**
   * Check if the appetite fits within available capacity.
   */
  public boolean appetiteFitsCapacity() {
    if (availableCapacityDays == null || requestedAppetiteDays == null) {
      return true; // No capacity data, assume it fits
    }
    return requestedAppetiteDays <= availableCapacityDays;
  }
}
