package com.github.farzadsedaghatbin.shipflow.dto.publicapi;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTaskStatusRequest {

  @NotBlank(message = "Status is required")
  private String status;

  /** Optional comment describing the status change. */
  private String comment;
}
