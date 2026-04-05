package com.github.farzadsedaghatbin.shipflow.dto.savedview;

import jakarta.validation.constraints.Size;
import lombok.*;

/** Request DTO for updating an existing saved filter view. All fields are optional. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSavedViewRequest {

  @Size(max = 100, message = "View name must not exceed 100 characters")
  private String name;

  /** Raw JSON string representing the updated filter state. */
  private String filters;
}
