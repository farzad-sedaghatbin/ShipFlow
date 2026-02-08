package com.github.farzadsedaghatbin.shipflow.dto.report;

import java.time.LocalDate;
import java.util.List;
import lombok.*;

/**
 * Enhanced cycle report with comprehensive metrics including risk analysis,
 * efficiency ratios, and detailed breakdowns.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnhancedCycleReportDTO {

  // Basic info
  private Long cycleId;
  private String cycleName;
  private String projectName;
  private LocalDate startDate;
  private LocalDate endDate;

  // Pitch metrics
  private Integer totalPitches;
  private Integer completedPitches;
  private Integer inProgressPitches;
  private Integer notStartedPitches;

  // Hours and efficiency
  private Double totalAppetiteHours;
  private Double totalActualHours;
  private Double varianceHours;
  private Double variancePercentage;
  private Double efficiencyPercentage; // (actual/appetite) * 100

  // Out-of-scope work (Tasks)
  private Integer totalTasks;
  private Integer completedTasks;
  private Double totalTaskEstimateHours;
  private Double totalTaskActualHours;

  // Risk distribution
  private RiskDistributionDTO riskDistribution;

  // Team member statistics
  private Integer totalTeamMembers;
  private Double averageHoursPerMember;
  private Double maxHoursPerMember;
  private Double minHoursPerMember;

  // Detailed breakdowns
  private List<PitchReportDTO> pitchReports;
  private List<MemberWorkReportDTO> memberReports;

  // Top performers and risks
  private List<String> topPerformers; // Members who worked most efficiently
  private List<String> overBudgetPitches; // Pitches exceeding appetite
}
