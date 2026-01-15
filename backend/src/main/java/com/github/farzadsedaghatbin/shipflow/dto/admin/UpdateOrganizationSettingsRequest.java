package com.github.farzadsedaghatbin.shipflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating organization settings.
 */
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
    private String timeZone;
    private String dateFormat;
    private Boolean enableNotifications;
    private Boolean enableAIFeatures;
}
