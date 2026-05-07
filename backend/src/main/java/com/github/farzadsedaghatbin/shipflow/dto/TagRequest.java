package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for creating/updating tags.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagRequest {

  @NotBlank(message = "Tag name is required")
  @Size(max = 100, message = "Tag name must be at most 100 characters")
  private String name;

  @NotNull(message = "Project ID is required")
  private Long projectId;

  @Size(max = 20, message = "Color must be at most 20 characters")
  private String color;

  @Size(max = 500, message = "Description must be at most 500 characters")
  private String description;
}
