package com.github.farzadsedaghatbin.shipflow.dto.savedview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Request DTO for creating a new saved filter view. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSavedViewRequest {

  @NotBlank(message = "View name is required")
  @Size(max = 100, message = "View name must not exceed 100 characters")
  private String name;

  /** Raw JSON string representing the filter state to persist. */
  @NotNull(message = "Filters are required")
  private String filters;
}
