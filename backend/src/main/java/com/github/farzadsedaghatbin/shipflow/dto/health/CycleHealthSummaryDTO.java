package com.github.farzadsedaghatbin.shipflow.dto.health;

import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import java.time.LocalDate;
import java.util.List;
import lombok.*;

/**
 * Cycle-level health summary for stakeholders. Aggregates pitch health data for
 * quick overview.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleHealthSummaryDTO {

  private Long cycleId;
  private String cycleName;
  private String projectName;
  private String projectKey;

  /** Overall cycle health status */
  private RiskLevel overallHealth;

  private String healthColor;

  /** Cycle timeline */
  private LocalDate startDate;

  private LocalDate endDate;
  private Integer daysLeft;
  private Double cycleProgressPercent;

  /** Pitch statistics */
  private Integer totalPitches;

  private Integer healthyPitches; // Low risk
  private Integer atRiskPitches; // Medium risk
  private Integer criticalPitches; // High/Critical risk

  /** Budget overview */
  private Double totalAppetiteHours;

  private Double totalActualHours;
  private Double budgetUsedPercent;

  /** Status breakdown */
  private Integer pendingCount;

  private Integer inProgressCount;
  private Integer testingCount;
  private Integer doneCount;

  /** Individual pitch health summaries */
  private List<PitchHealthDTO> pitchHealthList;
}
