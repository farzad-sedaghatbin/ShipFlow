package com.github.farzadsedaghatbin.shipflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Lightweight request for creating an idea.
 * Ideas are raw concepts that don't require shaping details yet.
 */
@Data
@Schema(description = "Request to create a lightweight idea (Shape Up pre-shaping)")
public class CreateIdeaRequest {

  @NotBlank(message = "Title is required")
  @Schema(description = "Brief title for the idea", example = "Add bulk export feature")
  private String title;

  @Schema(description = "Optional description or initial thoughts", example = "Users need to export multiple items at once")
  private String description;

  @Schema(description = "Optional epic ID to associate this idea with")
  private Long epicId;
}
