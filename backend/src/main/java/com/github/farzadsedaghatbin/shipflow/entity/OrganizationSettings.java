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
  @Builder.Default
  private String timeZone = "UTC";

  @Column(nullable = false, name = "date_format")
  @Builder.Default
  private String dateFormat = "MM/DD/YYYY";

  @Column(nullable = false, name = "enable_notifications")
  @Builder.Default
  private Boolean enableNotifications = true;

  @Column(nullable = false, name = "enable_ai_features")
  @Builder.Default
  private Boolean enableAIFeatures = true;

  // Email / SMTP settings (password is env-var only — never stored in DB)
  @Column(nullable = false, name = "email_notifications_enabled")
  @Builder.Default
  private Boolean emailNotificationsEnabled = true;

  @Column(name = "smtp_host")
  private String smtpHost;

  @Column(name = "smtp_port")
  @Builder.Default
  private Integer smtpPort = 587;

  @Column(name = "smtp_username")
  private String smtpUsername;

  @Column(name = "smtp_from")
  @Builder.Default
  private String smtpFrom = "noreply@shipflow.dev";

  @Column(nullable = false, name = "smtp_tls_enabled")
  @Builder.Default
  private Boolean smtpTlsEnabled = true;

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

  // Wise Architecture Feature Flag
  @Column(name = "enable_wise_architecture", nullable = false)
  @Builder.Default
  private Boolean enableWiseArchitecture = false;

  // MCP Server runtime toggle. NULL = fall back to the environment-variable default
  // (mcp.server.enabled / mcp.server.write-enabled). Set by an admin from the UI.
  @Column(name = "mcp_server_enabled")
  private Boolean mcpServerEnabled;

  @Column(name = "mcp_server_write_enabled")
  private Boolean mcpServerWriteEnabled;

  // Figma MCP Configuration
  @Column(name = "figma_access_token", columnDefinition = "TEXT")
  private String figmaAccessToken;

  @Column(name = "default_figma_file_key")
  private String defaultFigmaFileKey;

  // GitHub MCP Configuration
  @Column(name = "github_access_token", columnDefinition = "TEXT")
  private String githubAccessToken;

  @Column(name = "default_github_owner")
  private String defaultGithubOwner;

  @Column(name = "default_github_repo")
  private String defaultGithubRepo;

  @Column(name = "default_github_branch")
  @Builder.Default
  private String defaultGithubBranch = "main";

  // Linear OAuth Configuration (v1.2.0 S29)
  @Column(name = "linear_access_token", columnDefinition = "TEXT")
  private String linearAccessToken;

  @Column(name = "linear_team_id")
  private String linearTeamId;

  @Column(name = "linear_team_name")
  private String linearTeamName;

  // Jira OAuth Configuration (v1.2.0 S30)
  @Column(name = "jira_access_token", columnDefinition = "TEXT")
  private String jiraAccessToken;

  @Column(name = "jira_refresh_token", columnDefinition = "TEXT")
  private String jiraRefreshToken;

  @Column(name = "jira_cloud_id")
  private String jiraCloudId;

  @Column(name = "jira_cloud_name")
  private String jiraCloudName;

  // SCIM 2.0 provisioning settings (V2026_06_06_0001)
  @Column(name = "scim_enabled", nullable = false)
  @Builder.Default
  private boolean scimEnabled = false;

  @Column(name = "scim_bearer_token", length = 500)
  private String scimBearerToken;

  @Column(name = "scim_token_hash", length = 500)
  private String scimTokenHash;

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
