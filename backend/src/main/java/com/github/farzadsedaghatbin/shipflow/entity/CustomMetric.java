package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity representing a user-defined custom metric. Supports formula-based
 * calculations for deriving insights from various data sources.
 */
@Entity
@Table(name = "custom_metrics", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "name"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomMetric {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** Optional: Scope this metric to a specific cycle */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id")
  private Cycle cycle;

  /** Optional: Scope this metric to a specific pitch */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id")
  private Pitch pitch;

  /** Optional: Scope this metric to a specific team */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  private Team team;

  /** User-friendly name for the metric */
  @Column(name = "name", nullable = false, length = 100)
  private String name;

  /** Detailed description of what the metric measures */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /**
   * Formula for calculating the metric value. Examples: - "COUNT(pitch WHERE
   * status=DONE)" - "(SUM(workLog.hours WHERE pitch.status=DONE) /
   * SUM(pitch.appetiteDays * 8)) * 100" - "AVG(pitch.actualHours -
   * pitch.appetiteHours)"
   */
  @Column(name = "formula", nullable = false, columnDefinition = "TEXT")
  private String formula;

  /** Primary data source for the metric */
  @Column(name = "data_source", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private DataSource dataSource;

  /** Type of aggregation to apply */
  @Column(name = "aggregation_type", length = 20)
  @Enumerated(EnumType.STRING)
  private AggregationType aggregationType;

  /**
   * JSON filters to apply when calculating the metric Example: {"cycleId": 1,
   * "teamIds": [1,2], "dateRange": {"start": "2026-01-01", "end": "2026-01-31"}}
   */
  @Column(name = "filters", columnDefinition = "TEXT")
  private String filters;

  /** How to display the calculated value */
  @Column(name = "display_format", length = 20)
  @Enumerated(EnumType.STRING)
  private DisplayFormat displayFormat;

  /** Whether this metric is currently active and should be calculated */
  @Column(name = "is_active", nullable = false)
  private Boolean isActive;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (isActive == null) {
      isActive = true;
    }
    if (displayFormat == null) {
      displayFormat = DisplayFormat.NUMBER;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum DataSource {
    PITCH, CYCLE, TASK, WORK_LOG, TEAM, CUSTOM
  }

  public enum AggregationType {
    SUM, AVG, COUNT, MIN, MAX, RATIO, PERCENTAGE
  }

  public enum DisplayFormat {
    NUMBER, PERCENTAGE, CURRENCY, DURATION, DECIMAL
  }
}
