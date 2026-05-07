package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TeamMemberRole;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTeamAssignmentRequest {
  @NotNull(message = "Person ID is required")
  private Long personId;

  @NotNull(message = "Team ID is required")
  private Long teamId;

  @NotNull(message = "Role is required")
  private TeamMemberRole role;

  private LocalDate startDate;
  private LocalDate endDate;
  private String notes;
}
