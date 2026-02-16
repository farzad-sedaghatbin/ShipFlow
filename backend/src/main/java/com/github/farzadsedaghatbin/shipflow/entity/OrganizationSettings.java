package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity representing organization-wide configuration settings. Only one record
 * should exist in this table.
 */
@Entity
@Table(name = "organization_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSettings {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String organizationName;

  // Cycle Configuration
  @Column(nullable = false)
  @Builder.Default
  private Integer defaultCycleLengthWeeks = 6;

  @Column(nullable = false)
  @Builder.Default
  private Integer defaultCooldownWeeks = 2;

  // Risk Thresholds (stored as JSON in a single column for flexibility)
  @Column(columnDefinition = "TEXT")
  private String riskThresholdsJson;

  // Risk Factor Weights (Budget, Bugs, Scope, Time percentages)
  @Column(columnDefinition = "TEXT")
  private String riskWeightsJson;

  // Categories (stored as JSON arrays)
  @Column(columnDefinition = "TEXT")
  private String taskCategoriesJson;

  @Column(columnDefinition = "TEXT")
  private String pitchCategoriesJson;

  // Colors (stored as JSON)
  @Column(columnDefinition = "TEXT")
  private String colorsJson;

  // Bug Configuration (stored as JSON arrays)
  @Column(columnDefinition = "TEXT")
  private String bugStatusesJson;

  @Column(columnDefinition = "TEXT")
  private String severityLevelsJson;

  // Meeting Types Configuration (stored as JSON array)
  @Column(columnDefinition = "TEXT")
  private String meetingTypesJson;

  // Other Settings
  @Column(nullable = false, name = "time_zone")
  private String timeZone = "UTC";

  @Column(nullable = false, name = "date_format")
  private String dateFormat = "MM/DD/YYYY";

  @Column(nullable = false, name = "enable_notifications")
  @Builder.Default
  private Boolean enableNotifications = true;

  @Column(nullable = false, name = "enable_ai_features")
  @Builder.Default
  private Boolean enableAIFeatures = true;

  @Column(nullable = false, name = "enable_wise_architecture")
  @Builder.Default
  private Boolean enableWiseArchitecture = false;

  /**
   * Figma personal access token for reading design files via MCP.
   * Used by Wise Architecture to analyze Figma designs linked in pitches.
   */
  @Column(name = "figma_access_token", columnDefinition = "TEXT")
  private String figmaAccessToken;

  /**
   * Default Figma file key for design context.
   * Used as fallback when a pitch doesn't have wireframe links.
   */
  @Column(name = "default_figma_file_key")
  private String defaultFigmaFileKey;

  /**
   * GitHub personal access token for reading repository files via MCP.
   * Used by Wise Architecture to analyze code context.
   */
  @Column(name = "github_access_token", columnDefinition = "TEXT")
  private String githubAccessToken;

  /**
   * Default GitHub repository owner (organization or username).
   */
  @Column(name = "default_github_owner")
  private String defaultGithubOwner;

  /**
   * Default GitHub repository name.
   */
  @Column(name = "default_github_repo")
  private String defaultGithubRepo;

  /**
   * Default GitHub branch name.
   */
  @Column(name = "default_github_branch")
  @Builder.Default
  private String defaultGithubBranch = "main";

  // Capacity Configuration
  @Column(nullable = false, name = "default_hours_per_day", columnDefinition = "NUMERIC")
  @Builder.Default
  private Double defaultHoursPerDay = 8.0;

  @Column(nullable = false, name = "default_working_days_per_week")
  @Builder.Default
  private Integer defaultWorkingDaysPerWeek = 5;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Column(nullable = false)
  private String updatedBy;

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
