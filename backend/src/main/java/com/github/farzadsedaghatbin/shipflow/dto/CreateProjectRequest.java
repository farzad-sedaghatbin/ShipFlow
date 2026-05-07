package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProjectRequest {

  @NotBlank(message = "Project name is required")
  @Size(max = 100, message = "Project name must be less than 100 characters")
  private String name;

  @NotBlank(message = "Project key is required")
  @Size(min = 2, max = 10, message = "Project key must be 2-10 characters")
  @Pattern(regexp = "^[A-Z0-9]+$", message = "Project key must be uppercase letters and numbers only")
  private String projectKey;

  private String description;

  @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color (e.g., #FF5733)")
  private String color;

  private String logoUrl;

  private Long ownerId;

  /**
   * Project methodology type. SHAPE_UP: 6-week cycles with betting, pitches, and
   * cooldown (default) KANBAN: Continuous flow with visual board, no cycles
   */
  @Builder.Default
  private ProjectType projectType = ProjectType.SHAPE_UP;
}
