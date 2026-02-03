package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity representing a custom dashboard configuration. Users can create
 * multiple dashboards with different widget arrangements.
 */
@Entity
@Table(name = "custom_dashboards", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "name"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomDashboard {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** Optional: Scope this dashboard to a specific cycle */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id")
  private Cycle cycle;

  /** Optional: Scope this dashboard to a specific pitch */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id")
  private Pitch pitch;

  /** Optional: Scope this dashboard to a specific team */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  private Team team;

  /** Dashboard name (e.g., "My Dashboard", "Executive View") */
  @Column(name = "name", nullable = false, length = 100)
  private String name;

  /** Optional description of the dashboard's purpose */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /** Whether this is the user's default dashboard */
  @Column(name = "is_default", nullable = false)
  private Boolean isDefault;

  /**
   * Grid layout configuration (JSON) Example: {"columns": 12, "rowHeight": 60,
   * "breakpoints": {"lg": 1200, "md": 996, "sm": 768}}
   */
  @Column(name = "layout_config", columnDefinition = "TEXT")
  private String layoutConfig;

  /** Whether this is a system template (cannot be deleted by users) */
  @Column(name = "is_template", nullable = false)
  private Boolean isTemplate;

  /**
   * When enabled, widgets filter data to user's context (their cycles, teams,
   * tasks) When disabled, widgets show organization-wide data
   */
  @Column(name = "user_context_filter")
  private Boolean userContextFilter = false;

  /** Template category (for predefined templates) */
  @Column(name = "template_category", length = 50)
  @Enumerated(EnumType.STRING)
  private TemplateCategory templateCategory;

  /** User who created this template (for admin-created templates) */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (isDefault == null) {
      isDefault = false;
    }
    if (isTemplate == null) {
      isTemplate = false;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum TemplateCategory {
    EXECUTIVE, DEVELOPER, MANAGER, QA, CUSTOM
  }
}
