package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCycleRequest {
  @NotNull(message = "Project ID is required")
  private Long projectId;

  @NotBlank(message = "Name is required")
  private String name;

  @NotNull(message = "Start date is required")
  private LocalDate startDate;

  // End date is optional:
  // - If null, the end date will be auto-calculated from OrganizationSettings.
  // - If provided, it is treated as a custom end date and must only be supplied
  // by users with ADMIN or PROJECT_MANAGER roles (role check applies only in this
  // case).
  private LocalDate endDate;

  @Builder.Default
  private CyclePhase phase = CyclePhase.BUILD;
}
