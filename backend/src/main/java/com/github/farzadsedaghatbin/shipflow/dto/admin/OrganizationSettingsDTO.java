package com.github.farzadsedaghatbin.shipflow.dto.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for organization-wide settings. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSettingsDTO {

  private Long id;
  private String organizationName;

  // Cycle Configuration
  private Integer defaultCycleLengthWeeks;
  private Integer defaultCooldownWeeks;

  // Risk Thresholds
  private RiskThresholds riskThresholds;

  // Risk Factor Weights
  private RiskWeights riskWeights;

  // Categories
  private List<CategoryConfig> taskCategories;
  private List<CategoryConfig> pitchCategories;

  // Colors
  private ColorSettings colors;

  // Bug Configuration
  private List<BugStatusConfig> bugStatuses;
  private List<SeverityLevelConfig> severityLevels;

  // Meeting Types Configuration
  private List<MeetingTypeConfig> meetingTypes;

  // Other Settings
  private String timeZone;
  private String dateFormat;
  private Boolean enableNotifications;
  private Boolean enableAIFeatures;
  private Boolean enableWiseArchitecture;
  
  /**
   * Figma personal access token for Wise Architecture design context.
   * When set, allows AI to analyze Figma designs linked in pitches.
   * Note: Token is stored but not returned in responses for security.
   */
  private String figmaAccessToken;
  
  /**
   * Indicates if a Figma access token is configured (without exposing the token).
   */
  private Boolean hasFigmaAccessToken;
  
  /**
   * Default Figma file key for design context.
   */
  private String defaultFigmaFileKey;
  
  /**
   * GitHub personal access token for Wise Architecture code context.
   * When set, allows AI to analyze repository files.
   * Note: Token is stored but not returned in responses for security.
   */
  private String githubAccessToken;
  
  /**
   * Indicates if a GitHub access token is configured (without exposing the token).
   */
  private Boolean hasGithubAccessToken;
  
  /**
   * Default GitHub repository owner (organization or username).
   */
  private String defaultGithubOwner;
  
  /**
   * Default GitHub repository name.
   */
  private String defaultGithubRepo;
  
  /**
   * Default GitHub branch name.
   */
  private String defaultGithubBranch;
  private Boolean hasFigmaAccessToken;

  // Capacity Configuration
  private Double defaultHoursPerDay;
  private Integer defaultWorkingDaysPerWeek;

  private LocalDateTime updatedAt;
  private String updatedBy;

  /**
   * Configurable risk detection thresholds for automated health assessment. These
   * values control how the system calculates pitch health status.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class RiskThresholds {
    // Risk Score Boundaries (used to determine final risk level)
    @Builder.Default
    private Integer lowMax = 24; // 0-24 is LOW
    @Builder.Default
    private Integer mediumMax = 49; // 25-49 is MEDIUM
    @Builder.Default
    private Integer highMax = 69; // 50-69 is HIGH
    // 70+ is CRITICAL

    // Budget Thresholds (percentage of appetite used)
    @Builder.Default
    private Integer budgetWarning = 80; // Start warning at 80%
    @Builder.Default
    private Integer budgetOverrun = 100; // Over budget at 100%
    @Builder.Default
    private Integer budgetCritical = 120; // Critical overrun at 120%

    // Schedule Variance Thresholds (time progress vs work progress gap)
    @Builder.Default
    private Integer scheduleModerateGap = 15; // Moderately behind if gap > 15%

    @Builder.Default
    private Integer scheduleSignificantGap = 30; // Significantly behind if gap > 30%

    // Bug Count Thresholds
    @Builder.Default
    private Integer criticalBugsMinor = 1; // Risk increases with 1+ critical bugs
    @Builder.Default
    private Integer criticalBugsModerate = 3; // Higher risk with 3+ critical bugs
    @Builder.Default
    private Integer criticalBugsSevere = 5; // Severe risk with 5+ critical bugs
    @Builder.Default
    private Integer majorBugsThreshold = 3; // Risk increases with 3+ major bugs
    @Builder.Default
    private Integer majorBugsHigh = 5; // Higher risk with 5+ major bugs
    @Builder.Default
    private Integer openBugsModerate = 5; // Concern with 5+ open bugs
    @Builder.Default
    private Integer openBugsHigh = 10; // Higher concern with 10+ open bugs
    @Builder.Default
    private Integer openBugsCritical = 15; // Critical concern with 15+ open bugs
    @Builder.Default
    private Integer recentBugInflux = 5; // Quality concern if 5+ bugs in 3 days

    // Bug Resolution Rate Threshold
    @Builder.Default
    private Integer bugResolutionRateMin = 50; // Concern if resolution rate < 50%

    // Scope Progress Thresholds (hill chart positions 0-100)
    @Builder.Default
    private Integer scopeEarlyPhase = 25; // Still in early understanding phase
    @Builder.Default
    private Integer scopeUphillMax = 30; // Maximum uphill position threshold
    @Builder.Default
    private Integer scopeMidPhase = 40; // Mid-understanding phase
    @Builder.Default
    private Integer scopePeakMin = 45; // Minimum peak position (decision point)
    @Builder.Default
    private Integer scopePeakMax = 55; // Maximum peak position (decision point)

    @Builder.Default
    private Double scopeExpectedProgressRate = 0.8; // Should be at 80% of cycle progress

    @Builder.Default
    private Integer scopeLagSignificant = 30; // Significant lag if gap > 30%

    // Time-based Thresholds (days remaining)
    @Builder.Default
    private Integer daysUrgent = 3; // Urgent if ≤3 days left
    @Builder.Default
    private Integer daysWarning = 7; // Warning if ≤7 days left
    @Builder.Default
    private Integer daysConcern = 14; // Concern threshold at 14 days

    // Cycle Progress Thresholds (percentage of cycle elapsed)
    @Builder.Default
    private Integer cycleMidpoint = 50; // Midpoint of cycle
    @Builder.Default
    private Integer cycleLatePhase = 60; // Late phase begins
    @Builder.Default
    private Integer cycleFinalQuarter = 75; // Final quarter begins
    @Builder.Default
    private Integer cycleMinForScopes = 30; // Should have scopes defined by 30%

    // Stagnation Thresholds (days without movement)
    @Builder.Default
    private Integer scopeStagnationDays = 7; // Scope hasn't moved in 7 days
    @Builder.Default
    private Integer peakStuckDays = 5; // Stuck at peak for 5 days
    @Builder.Default
    private Integer noProgressDays = 7; // No overall progress in 7 days

    // Work Rate Thresholds
    @Builder.Default
    private Integer recentWorkHighHours = 15; // High work rate indicator (3 days)
    @Builder.Default
    private Integer appetiteHighUsage = 90; // High appetite usage threshold
  }

  /**
   * Configurable risk factor weights for calculating overall risk score. All
   * weights should sum to 100 (percentage-based). These control how much each
   * factor contributes to the final risk assessment.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class RiskWeights {
    // Default risk weight constants
    public static final int DEFAULT_BUDGET_WEIGHT = 25;
    public static final int DEFAULT_BUGS_WEIGHT = 30;
    public static final int DEFAULT_SCOPE_WEIGHT = 25;
    public static final int DEFAULT_TIME_WEIGHT = 20;

    @Builder.Default
    private Integer budgetWeight = DEFAULT_BUDGET_WEIGHT; // Budget utilization contribution (default: 25%)

    @Builder.Default
    private Integer bugsWeight = DEFAULT_BUGS_WEIGHT; // Bug severity contribution (default: 30%)

    @Builder.Default
    private Integer scopeWeight = DEFAULT_SCOPE_WEIGHT; // Scope progress contribution (default: 25%)

    @Builder.Default
    private Integer timeWeight = DEFAULT_TIME_WEIGHT; // Time pressure contribution (default: 20%)

    /** Validate that weights sum to 100 */
    public boolean isValid() {
      return budgetWeight + bugsWeight + scopeWeight + timeWeight == 100;
    }

    /**
     * Normalize weights to sum to 100 if they don't already.
     *
     * <p>
     * Integer division truncates fractional parts, so timeWeight absorbs the
     * remainder to ensure the sum equals exactly 100. If total is zero, resets to
     * defaults.
     *
     * <p>
     * Example: [33, 33, 33, 33] (sum=132) → [25, 25, 25, 25] (sum=100)
     */
    public void normalize() {
      int total = budgetWeight + bugsWeight + scopeWeight + timeWeight;
      if (total == 0) {
        // Reset to defaults when all weights are zero
        budgetWeight = DEFAULT_BUDGET_WEIGHT;
        bugsWeight = DEFAULT_BUGS_WEIGHT;
        scopeWeight = DEFAULT_SCOPE_WEIGHT;
        timeWeight = DEFAULT_TIME_WEIGHT;
      } else if (total != 100) {
        budgetWeight = (budgetWeight * 100) / total;
        bugsWeight = (bugsWeight * 100) / total;
        scopeWeight = (scopeWeight * 100) / total;
        // Assign remainder to timeWeight to ensure sum equals exactly 100
        // (compensates for precision loss from integer division truncation)
        // NOTE: timeWeight is chosen arbitrarily to receive the remainder
        timeWeight = 100 - (budgetWeight + bugsWeight + scopeWeight);
      }
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CategoryConfig {
    private Long id;
    private String name;
    private String description;
    private String color;
    private Boolean isActive;
    private Integer order;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ColorSettings {
    @Builder.Default
    private String appetiteHours = "#3B82F6";
    @Builder.Default
    private String actualHours = "#10B981";
    @Builder.Default
    private String overBudget = "#EF4444";
    @Builder.Default
    private String underBudget = "#22C55E";
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class BugStatusConfig {
    private Long id;
    private String name;
    private String description;
    private String color;
    private Boolean isActive;
    private Integer order;
    private Boolean isClosed;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class SeverityLevelConfig {
    private Long id;
    private String name;
    private String description;
    private String color;
    private Boolean isActive;
    private Integer order;
    private Integer priority;
  }

  /**
   * Configuration for a meeting type with DOR and DOD checklist items.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class MeetingTypeConfig {
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String color;
    private Boolean isActive;
    private Integer order;
    @Builder.Default
    private List<DorDodItem> dorItems = new ArrayList<>();
    @Builder.Default
    private List<DorDodItem> dodItems = new ArrayList<>();
  }

  /**
   * A single checklist item for DOR (Definition of Ready) or DOD (Definition of Done).
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class DorDodItem {
    private Long id;
    private String name;
    private String description;
    private Boolean isRequired;
    private Integer order;
    @Builder.Default
    private Boolean isDeleted = false;
  }
}
