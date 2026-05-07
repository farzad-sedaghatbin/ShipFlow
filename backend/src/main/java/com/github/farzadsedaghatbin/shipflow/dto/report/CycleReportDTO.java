package com.github.farzadsedaghatbin.shipflow.dto.report;

import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleReportDTO {
  private Long cycleId;
  private String cycleName;
  private Integer totalPitches;
  private Integer completedPitches;
  private Integer inProgressPitches;
  private Double totalAppetiteHours;
  private Double totalActualHours;
  private Double efficiencyPercentage;

  // Out-of-scope work (Tasks not associated with pitches)
  private Integer totalTasks;
  private Integer completedTasks;
  private Double totalTaskEstimateHours;
  private Double totalTaskActualHours;

  private List<PitchReportDTO> pitchReports;
  private List<MemberWorkReportDTO> memberReports;
}
