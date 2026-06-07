package com.github.farzadsedaghatbin.shipflow.dto.pitch;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for the AI Pitch Writer generate endpoint. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PitchWriterRequestDTO {

  @NotBlank(message = "Problem description is required")
  private String problemDescription;

  /** Optional appetite hint: 14 (small batch) or 28 (big batch). */
  private Integer appetiteHint;

  /** Optional project context, e.g. "B2B SaaS, 5-person team". */
  private String projectContext;
}
