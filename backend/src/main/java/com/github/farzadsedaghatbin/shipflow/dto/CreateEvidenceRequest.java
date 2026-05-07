package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEvidenceRequest {
  @NotNull(message = "Pitch ID is required")
  private Long pitchId;

  @NotNull(message = "Person ID is required")
  private Long personId;

  @NotNull(message = "Date is required")
  private LocalDate date;

  @NotBlank(message = "Description is required")
  private String description;

  private String fileUrl;
}
