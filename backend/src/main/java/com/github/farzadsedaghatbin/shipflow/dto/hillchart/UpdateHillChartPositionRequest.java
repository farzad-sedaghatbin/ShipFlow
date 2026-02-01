package com.github.farzadsedaghatbin.shipflow.dto.hillchart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** Request DTO for updating hill chart point position with confidence tracking. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateHillChartPositionRequest {

  @NotNull(message = "New position is required")
  @Min(value = 0, message = "Position must be between 0 and 100")
  @Max(value = 100, message = "Position must be between 0 and 100")
  private Integer newPosition;

  /** User's confidence level (0-100) - how confident they are about this position */
  @Min(value = 0, message = "Confidence must be between 0 and 100")
  @Max(value = 100, message = "Confidence must be between 0 and 100")
  private Integer confidenceLevel;

  /** Optional note explaining the move */
  private String note;
}
