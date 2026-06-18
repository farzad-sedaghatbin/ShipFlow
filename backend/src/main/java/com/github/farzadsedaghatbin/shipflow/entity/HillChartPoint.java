package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "hill_chart_points")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HillChartPoint {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id", nullable = false)
  private Pitch pitch;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String scope;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  // Position on hill: 0-50 is uphill (figuring things out), 50-100 is downhill
  // (executing)
  @Column(nullable = false)
  private Integer position;

  /**
   * The task linked to this scope for unified Scope-Task tracking.
   * When a task is created with a pitch link, this binding is auto-created.
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "linked_task_id")
  private Task linkedTask;

  /**
   * When true, the position auto-updates based on linked task's subtask completion.
   * Defaults to true. Set to false when user manually drags the scope on hill chart.
   */
  @Column(name = "auto_progress_enabled", nullable = false)
  @Builder.Default
  private Boolean autoProgressEnabled = true;

  /**
   * The suggested position based on subtask completion (0-100).
   * Calculated but not applied unless autoProgressEnabled is true or user accepts.
   */
  @Transient
  private Integer suggestedPosition;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (autoProgressEnabled == null) {
      autoProgressEnabled = true;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
