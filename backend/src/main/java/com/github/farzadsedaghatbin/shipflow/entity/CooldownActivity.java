package com.github.farzadsedaghatbin.shipflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Represents an activity tracked during the cooldown phase of a Shape Up cycle.
 * Cooldown is the 2-week period between build cycles for cleanup, learning, and preparation.
 * 
 * This supports Shape Up methodology by:
 * - Tracking technical debt repayment
 * - Recording bug fixes addressed between cycles
 * - Documenting learning/training activities
 * - Planning kickoff preparation
 */
@Entity
@Table(name = "cooldown_activities", indexes = {
    @Index(name = "idx_cooldown_activity_cycle", columnList = "cycle_id"),
    @Index(name = "idx_cooldown_activity_type", columnList = "activity_type"),
    @Index(name = "idx_cooldown_activity_status", columnList = "status"),
    @Index(name = "idx_cooldown_activity_assignee", columnList = "assignee_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CooldownActivity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "pitches", "teams", "retrospectives"})
  private Cycle cycle;

  /** Type of cooldown activity */
  @Enumerated(EnumType.STRING)
  @Column(name = "activity_type", nullable = false)
  private CooldownActivityType activityType;

  /** Short title/name of the activity */
  @Column(nullable = false)
  private String title;

  /** Detailed description of what needs to be done */
  @Column(columnDefinition = "TEXT")
  private String description;

  /** Current status of the activity */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private CooldownActivityStatus status = CooldownActivityStatus.PLANNED;

  /** Person assigned to this activity */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "projects"})
  private User assignee;

  /** User who created/reported this activity */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "projects"})
  private User createdBy;

  /** Estimated effort in hours (optional) */
  @Column
  private Integer estimatedHours;

  /** Actual hours spent (updated on completion) */
  @Column
  private Integer actualHours;

  /** Priority level (1 = highest) */
  @Column
  @Builder.Default
  private Integer priority = 3;

  /** Optional link to related pitch (e.g., for bug fixes from a specific pitch) */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "related_pitch_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "workLogs", "meetings", "evidences"})
  private Pitch relatedPitch;

  /** Notes or comments about the activity */
  @Column(columnDefinition = "TEXT")
  private String notes;

  /** When the activity was started */
  @Column
  private LocalDateTime startedAt;

  /** When the activity was completed */
  @Column
  private LocalDateTime completedAt;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Helper methods
  public boolean isCompleted() {
    return status == CooldownActivityStatus.COMPLETED;
  }

  public boolean isInProgress() {
    return status == CooldownActivityStatus.IN_PROGRESS;
  }

  public void start() {
    if (status == CooldownActivityStatus.PLANNED) {
      status = CooldownActivityStatus.IN_PROGRESS;
      startedAt = LocalDateTime.now();
    }
  }

  public void complete(Integer hoursSpent) {
    status = CooldownActivityStatus.COMPLETED;
    completedAt = LocalDateTime.now();
    if (hoursSpent != null) {
      actualHours = hoursSpent;
    }
  }
}
