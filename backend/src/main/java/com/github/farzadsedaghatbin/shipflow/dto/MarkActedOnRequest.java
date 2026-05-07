package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for marking a retro action item as acted upon.
 * Part of v0.5 follow-through tracking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkActedOnRequest {

  @NotNull(message = "actedOn status is required")
  private Boolean actedOn;

  private String notes;
}
