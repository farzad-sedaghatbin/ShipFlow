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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskThresholds {
        private Integer lowMax = 30;      // 0-30 is LOW
        private Integer mediumMax = 60;   // 31-60 is MEDIUM
        private Integer highMax = 85;     // 61-85 is HIGH
        // 86-100 is CRITICAL
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
