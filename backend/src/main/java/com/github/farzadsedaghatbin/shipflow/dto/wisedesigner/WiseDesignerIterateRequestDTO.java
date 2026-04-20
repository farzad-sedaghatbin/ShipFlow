package com.github.farzadsedaghatbin.shipflow.dto.wisedesigner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to iterate on an existing design artifact.
 *
 * <p>The service retrieves the current HTML via {@code artifactId}, prepends it as context to
 * the LLM prompt, and applies the {@code instruction}. The result is saved as a new artifact row
 * (history is preserved; the old artifact is never overwritten).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WiseDesignerIterateRequestDTO {

  @NotNull(message = "artifactId is required")
  private Long artifactId;

  /**
   * Natural-language instruction describing the change. Examples:
   * "Make the navigation sticky", "Add a dark mode toggle in the header",
   * "Replace the table with a card grid", "Add form validation error states".
   */
  @NotBlank(message = "instruction is required")
  private String instruction;
}
