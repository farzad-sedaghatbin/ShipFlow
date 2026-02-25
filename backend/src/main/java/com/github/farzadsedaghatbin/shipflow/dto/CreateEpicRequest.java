package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BusinessValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.*;

/**
 * Request DTO for creating or updating an Epic.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEpicRequest {

  @NotBlank(message = "Epic name is required")
  @Size(max = 255, message = "Name must be less than 255 characters")
  private String name;

  private String description;

  @Builder.Default
  private EpicStatus status = EpicStatus.DRAFT;

  @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex color (e.g., #3B82F6)")
  private String color;

  private LocalDate targetStartDate;

  private LocalDate targetEndDate;

  @NotNull(message = "Project ID is required")
  private Long projectId;

  /** Optional parent initiative */
  private Long initiativeId;

  private Long ownerId;

  private Integer sortOrder;

  private BusinessValue priority;
}
