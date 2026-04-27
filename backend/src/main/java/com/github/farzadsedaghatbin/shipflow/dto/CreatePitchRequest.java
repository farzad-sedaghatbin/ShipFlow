package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BusinessValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for creating pitches.
 * 
 * Field requirements by status:
 * - IDEA: title only (lightweight idea capture)
 * - DRAFT: title (shaping in progress)
 * - SHAPED: title, appetiteDays (ready for betting)
 * - PENDING+: title, appetiteDays, cycleId (assigned to cycle)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePitchRequest {
  @NotBlank(message = "Title is required")
  private String title;

  private String description;

  /** Required for SHAPED status and beyond. NULL allowed for IDEA/DRAFT. */
  @Min(value = 1, message = "Appetite must be at least 1 day")
  private Integer appetiteDays;

  /** Required for PENDING status and beyond. NULL allowed for IDEA/DRAFT/SHAPED. */
  private Long cycleId;

  private Long teamId;

  // Epic and Release linking (roadmap)
  private Long epicId;
  private Long targetReleaseId;

  // Priority and ordering
  private BusinessValue priority;
  private Integer sortOrder;

  /** Default to IDEA for new pitches (lightweight capture). */
  @Builder.Default
  private PitchStatus status = PitchStatus.IDEA;

  // Shape Up Methodology Fields
  private String problemStatement;
  private String solution;
  private String rabbitHoles;
  private String risks;
  private String noGos;
  private String wireframeLinks;
}
