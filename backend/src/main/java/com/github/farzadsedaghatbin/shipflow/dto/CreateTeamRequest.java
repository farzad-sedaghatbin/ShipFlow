package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTeamRequest {
  @NotBlank(message = "Team name is required")
  private String name;

  // Capacity Configuration Overrides (null = inherit from organization)
  private Double hoursPerDayOverride;
  private Integer workingDaysPerWeekOverride;
}
