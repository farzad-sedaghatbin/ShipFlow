package com.github.farzadsedaghatbin.shipflow.dto.health;

import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import java.time.LocalDate;
import java.util.List;
import lombok.*;

/**
 * Lightweight pitch health summary DTO for non-technical stakeholders. Shows a
 * simplified snapshot of pitch status.
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

  /** Risk level with color indicator (LOW=green, MEDIUM=yellow, HIGH=red) */
  private RiskLevel riskLevel;

  private String riskColor;

  /**
   * Risk trend indicator: IMPROVING, STABLE, WORSENING Based on recent changes in
   * risk factors
   */
  private String riskTrend;

  /** Percentage of appetite hours used (0-100+) */
  private Double appetiteUsedPercent;

  /** Days remaining in the cycle */
  private Integer daysLeft;

  /** Human-readable status summary */
  private String statusSummary;

  /** Current pitch status */
  private String status;

  /** Team assigned to this pitch */
  private String teamName;

  /** Hours budgeted (appetite) */
  private Double appetiteHours;

  /** Hours actually spent */
  private Double actualHours;

  /** QA status: NOT_STARTED, IN_PROGRESS, COMPLETED */
  private String qaStatus;

  /** Cycle end date for reference */
  private LocalDate cycleEndDate;

  // --- Team Capacity / Budget Fields ---
  
  /** Number of active team members */
  private Integer teamMemberCount;
  
  /** Appetite in calendar days */
  private Integer appetiteDays;
  
  /** Total budget in person-days (sum of all members' budgets) */
  private Double totalBudgetPersonDays;
  
  /** Total hours spent by the team */
  private Double totalHoursSpent;
  
  /** Total person-days spent (totalHoursSpent / avgHoursPerDay) */
  private Double totalPersonDaysSpent;
  
  /** Budget utilization percentage based on person-days */
  private Double budgetUtilizationPercent;
  
  /** Member who is closest to exhausting their individual budget */
  private MemberBudgetSummary bottleneckMember;
  
  /** Individual member budget breakdowns */
  private List<MemberBudgetSummary> memberBudgets;
  
  /** Whether any team member has exceeded their individual budget */
  private Boolean hasOverBudgetMember;
  
  /**
   * Summary of a team member's budget status.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class MemberBudgetSummary {
    private Long personId;
    private String personName;
    private String role;
    private Double hoursPerDay;
    private String capacitySource; // organization, team, person, assignment
    private Double totalBudgetHours;
    private Double hoursSpent;
    private Double hoursRemaining;
    private Double utilizationPercent;
    private Boolean isOverBudget;
  }
}
