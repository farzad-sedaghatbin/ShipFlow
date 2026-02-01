package com.github.farzadsedaghatbin.shipflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity to track hill chart position history. Stores historical positions to track confidence vs
 * actual progress.
 */
@Entity
@Table(name = "hill_chart_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HillChartHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hill_chart_point_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private HillChartPoint hillChartPoint;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private Pitch pitch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private User movedBy;

  /** Position before the move (0-100) */
  @Column(name = "previous_position", nullable = false)
  private Integer previousPosition;

  /** Position after the move (0-100) */
  @Column(name = "new_position", nullable = false)
  private Integer newPosition;

  /** User's confidence level when moving (0-100) */
  @Column(name = "confidence_level")
  private Integer confidenceLevel;

  /** Optional note explaining the move */
  @Column(columnDefinition = "TEXT")
  private String note;

  /** Actual logged hours at time of move */
  @Column(name = "actual_hours_at_move")
  private Double actualHoursAtMove;

  /** Progress percentage at time of move */
  @Column(name = "progress_at_move")
  private Double progressAtMove;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
