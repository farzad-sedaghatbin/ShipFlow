package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity representing a user's dashboard widget configuration. Allows users to
 * customize which widgets appear on their dashboard and in what order.
 */
@Entity
@Table(name = "dashboard_widgets", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "widget_type"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardWidget {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /**
   * Type of widget (e.g., ACTIVE_CYCLES, RECENT_PITCHES, HILL_CHART, QUICK_LINKS,
   * RECENT_ACTIVITY, RISK_OVERVIEW, STATS_CARDS, NOTIFICATIONS)
   */
  @Column(name = "widget_type", nullable = false, length = 50)
  private String widgetType;

  /** Whether this widget is visible on the dashboard */
  @Column(name = "is_visible", nullable = false)
  private Boolean isVisible = true;

  /** Display order position (lower numbers appear first) */
  @Column(name = "display_order", nullable = false)
  private Integer displayOrder = 0;

  /**
   * Grid layout configuration (JSON string) Example: {"column": 1, "row": 1,
   * "width": 1, "height": 1}
   */
  @Column(name = "layout_config", columnDefinition = "TEXT")
  private String layoutConfig;

  /**
   * Widget-specific settings (JSON string) Example: {"maxItems": 5, "compact":
   * true}
   */
  @Column(name = "settings", columnDefinition = "TEXT")
  private String settings;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
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
