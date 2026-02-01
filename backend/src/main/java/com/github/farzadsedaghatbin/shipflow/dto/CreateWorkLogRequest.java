package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkLogRequest {
  @NotNull(message = "Person ID is required")
  private Long personId;

  // Either pitchId or taskId must be provided
  private Long pitchId;

  private Long taskId;

  @NotNull(message = "Date is required")
  private LocalDate date;

  @NotNull(message = "Hours spent is required")
  @DecimalMin(value = "0.25", message = "Minimum time is 15 minutes (0.25 hours)")
  private BigDecimal hoursSpent;

  private String note;
}
