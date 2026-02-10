package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHillChartPointRequest {
  @NotNull(message = "Pitch ID is required")
  private Long pitchId;

  @NotBlank(message = "Scope is required")
  @Size(max = 100, message = "Scope must not exceed 100 characters")
  private String scope;

  @NotBlank(message = "Description is required")
  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;

  @NotNull(message = "Position is required")
  @Min(value = 0, message = "Position must be between 0 and 100")
  @Max(value = 100, message = "Position must be between 0 and 100")
  private Integer position;

  /**
   * When true, auto-creates a Task linked to this scope.
   * Defaults to true for unified Scope-Task workflow.
   */
  @Builder.Default
  private Boolean createTaskAutomatically = true;

  /**
   * Optional assignee ID for the auto-created task.
   */
  private Long assigneeId;
}
