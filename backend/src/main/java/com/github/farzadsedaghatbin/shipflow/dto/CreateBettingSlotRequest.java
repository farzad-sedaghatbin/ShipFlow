package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBettingSlotRequest {

  @NotNull(message = "Cycle ID is required")
  private Long cycleId;

  @NotNull(message = "Team ID is required")
  private Long teamId;

  private Long pitchId; // Optional - slot can be created empty

  @NotNull(message = "Position is required")
  private Integer position;

  @NotNull(message = "Start date is required")
  private LocalDate startDate;

  @NotNull(message = "End date is required")
  private LocalDate endDate;

  private String notes;
}
