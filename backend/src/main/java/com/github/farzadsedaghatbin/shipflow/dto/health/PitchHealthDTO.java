package com.github.farzadsedaghatbin.shipflow.dto.health;

import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import lombok.*;

import java.time.LocalDate;

/**
 * Lightweight pitch health summary DTO for non-technical stakeholders.
 * Shows a simplified snapshot of pitch status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchHealthDTO {
    
    private Long pitchId;
    private String pitchName;
    private String projectName;
    private String projectKey;
    private String cycleName;
    
    /**
     * Risk level with color indicator (LOW=green, MEDIUM=yellow, HIGH=red)
     */
    private PitchRiskDTO.RiskLevel riskLevel;
    private String riskColor;
    
    /**
     * Percentage of appetite hours used (0-100+)
     */
    private Double appetiteUsedPercent;
    
    /**
     * Days remaining in the cycle
     */
    private Integer daysLeft;
    
    /**
     * Human-readable status summary
     */
    private String statusSummary;
    
    /**
     * Current pitch status
     */
    private String status;
    
    /**
     * Team assigned to this pitch
     */
    private String teamName;
    
    /**
     * Hours budgeted (appetite)
     */
    private Double appetiteHours;
    
    /**
     * Hours actually spent
     */
    private Double actualHours;
    
    /**
     * QA status: NOT_STARTED, IN_PROGRESS, COMPLETED
     */
    private String qaStatus;
    
    /**
     * Cycle end date for reference
     */
    private LocalDate cycleEndDate;
}
