package com.github.farzadsedaghatbin.shipflow.dto;

import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamDTO {
  private Long id;
  private String name;
  private List<TeamAssignmentDTO> assignments;

  // Capacity Configuration Overrides (null = inherit from organization)
  private Double hoursPerDayOverride;
  private Integer workingDaysPerWeekOverride;

  // Computed effective values (resolved from hierarchy: team → org)
  private Double effectiveHoursPerDay;
  private Integer effectiveWorkingDaysPerWeek;

  // Computed team capacity summary
  private TeamCapacitySummary capacitySummary;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TeamCapacitySummary {
    private Integer memberCount;
    private Double totalDailyCapacityHours; // Sum of all member hours/day
    private String capacitySource; // "organization", "team", or "mixed" (some members have overrides)
  }
}
