package com.github.farzadsedaghatbin.shipflow.dto.admin;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for updating organization settings. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrganizationSettingsRequest {

  private String organizationName;
  private Integer defaultCycleLengthWeeks;
  private Integer defaultCooldownWeeks;
  private OrganizationSettingsDTO.RiskThresholds riskThresholds;
  private OrganizationSettingsDTO.RiskWeights riskWeights;
  private List<OrganizationSettingsDTO.CategoryConfig> taskCategories;
  private List<OrganizationSettingsDTO.CategoryConfig> pitchCategories;
  private OrganizationSettingsDTO.ColorSettings colors;
  private List<OrganizationSettingsDTO.BugStatusConfig> bugStatuses;
  private List<OrganizationSettingsDTO.SeverityLevelConfig> severityLevels;
  private List<OrganizationSettingsDTO.MeetingTypeConfig> meetingTypes;
  private String timeZone;
  private String dateFormat;
  private Boolean enableNotifications;
  private Boolean enableAIFeatures;

  // Capacity Configuration
  private Double defaultHoursPerDay;
  private Integer defaultWorkingDaysPerWeek;

  // Email / SMTP settings
  private Boolean emailNotificationsEnabled;
  private String smtpHost;
  private Integer smtpPort;
  private String smtpUsername;
  private String smtpFrom;
  private Boolean smtpTlsEnabled;

  // Wise Architecture Feature Flag
  private Boolean enableWiseArchitecture;

  // Figma MCP Configuration (token managed via MCP settings API)
  private String figmaAccessToken;

  // GitHub MCP Configuration (token managed via MCP settings API)
  private String githubAccessToken;

  // MCP Server runtime toggle (null = leave unchanged)
  private Boolean mcpServerEnabled;
  private Boolean mcpServerWriteEnabled;

  // SCIM 2.0 toggle (null = leave unchanged)
  private Boolean scimEnabled;
}
