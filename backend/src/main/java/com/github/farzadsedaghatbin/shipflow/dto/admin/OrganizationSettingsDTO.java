package com.github.farzadsedaghatbin.shipflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for organization-wide settings.
 */
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
    
    // Categories
    private List<CategoryConfig> taskCategories;
    private List<CategoryConfig> pitchCategories;
    
    // Colors
    private ColorSettings colors;
    
    // Bug Configuration
    private List<BugStatusConfig> bugStatuses;
    private List<SeverityLevelConfig> severityLevels;
    
    // Other Settings
    private String timeZone;
    private String dateFormat;
    private Boolean enableNotifications;
    private Boolean enableAIFeatures;
    
    private LocalDateTime updatedAt;
    private String updatedBy;

    /**
     * Configurable risk detection thresholds for automated health assessment.
     * These values control how the system calculates pitch health status.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskThresholds {
        // Risk Score Boundaries (used to determine final risk level)
        private Integer lowMax = 24;           // 0-24 is LOW
        private Integer mediumMax = 49;        // 25-49 is MEDIUM
        private Integer highMax = 69;          // 50-69 is HIGH
        // 70+ is CRITICAL
        
        // Budget Thresholds (percentage of appetite used)
        private Integer budgetWarning = 80;         // Start warning at 80%
        private Integer budgetOverrun = 100;        // Over budget at 100%
        private Integer budgetCritical = 120;       // Critical overrun at 120%
        
        // Schedule Variance Thresholds (time progress vs work progress gap)
        private Integer scheduleModerateGap = 15;   // Moderately behind if gap > 15%
        private Integer scheduleSignificantGap = 30; // Significantly behind if gap > 30%
        
        // Bug Count Thresholds
        private Integer criticalBugsMinor = 1;      // Risk increases with 1+ critical bugs
        private Integer criticalBugsModerate = 3;   // Higher risk with 3+ critical bugs
        private Integer criticalBugsSevere = 5;     // Severe risk with 5+ critical bugs
        private Integer majorBugsThreshold = 3;     // Risk increases with 3+ major bugs
        private Integer majorBugsHigh = 5;          // Higher risk with 5+ major bugs
        private Integer openBugsModerate = 5;       // Concern with 5+ open bugs
        private Integer openBugsHigh = 10;          // Higher concern with 10+ open bugs
        private Integer openBugsCritical = 15;      // Critical concern with 15+ open bugs
        private Integer recentBugInflux = 5;        // Quality concern if 5+ bugs in 3 days
        
        // Bug Resolution Rate Threshold
        private Integer bugResolutionRateMin = 50;  // Concern if resolution rate < 50%
        
        // Scope Progress Thresholds (hill chart positions 0-100)
        private Integer scopeEarlyPhase = 25;       // Still in early understanding phase
        private Integer scopeUphillMax = 30;        // Maximum uphill position threshold
        private Integer scopeMidPhase = 40;         // Mid-understanding phase
        private Integer scopePeakMin = 45;          // Minimum peak position (decision point)
        private Integer scopePeakMax = 55;          // Maximum peak position (decision point)
        private Double scopeExpectedProgressRate = 0.8; // Should be at 80% of cycle progress
        private Integer scopeLagSignificant = 30;   // Significant lag if gap > 30%
        
        // Time-based Thresholds (days remaining)
        private Integer daysUrgent = 3;             // Urgent if ≤3 days left
        private Integer daysWarning = 7;            // Warning if ≤7 days left
        private Integer daysConcern = 14;           // Concern threshold at 14 days
        
        // Cycle Progress Thresholds (percentage of cycle elapsed)
        private Integer cycleMidpoint = 50;         // Midpoint of cycle
        private Integer cycleLatePhase = 60;        // Late phase begins
        private Integer cycleFinalQuarter = 75;     // Final quarter begins
        private Integer cycleMinForScopes = 30;     // Should have scopes defined by 30%
        
        // Stagnation Thresholds (days without movement)
        private Integer scopeStagnationDays = 7;    // Scope hasn't moved in 7 days
        private Integer peakStuckDays = 5;          // Stuck at peak for 5 days
        private Integer noProgressDays = 7;         // No overall progress in 7 days
        
        // Work Rate Thresholds
        private Integer recentWorkHighHours = 15;   // High work rate indicator (3 days)
        private Integer appetiteHighUsage = 90;     // High appetite usage threshold
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
        private String appetiteHours = "#3B82F6";
        private String actualHours = "#10B981";
        private String overBudget = "#EF4444";
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
}
