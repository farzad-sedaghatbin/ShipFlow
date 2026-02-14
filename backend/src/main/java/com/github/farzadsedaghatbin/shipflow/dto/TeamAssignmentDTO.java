package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TeamMemberRole;
import java.time.LocalDate;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamAssignmentDTO {
  private Long id;
  private Long personId;
  private String personName;
  private Long teamId;
  private String teamName;
  private Long cycleId;
  private String cycleName;
  private TeamMemberRole role;
  private LocalDate startDate;
  private LocalDate endDate;
  private Boolean isActive;
  private String notes;

  // Capacity Configuration Override (per team assignment)
  private Double hoursPerDayOverride;

  // Computed effective hours per day (resolved from hierarchy: assignment → person → team → org)
  private Double effectiveHoursPerDay;

  // Source of the effective value for UI display
  private String capacitySource; // "organization", "team", "person", or "assignment"
}
