package com.github.farzadsedaghatbin.shipflow.dto.cooldown;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for creating a cooldown activity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCooldownActivityRequest {

  @NotNull(message = "Cycle ID is required")
  private Long cycleId;

  @NotNull(message = "Activity type is required")
  private CooldownActivityType activityType;

  @NotBlank(message = "Title is required")
  private String title;

  private String description;

  /** Optional assignee ID */
  private Long assigneeId;

  /** Estimated hours (optional) */
  private Integer estimatedHours;

  /** Priority (1 = highest, default 3) */
  @Builder.Default
  private Integer priority = 3;

  /** Optional related pitch ID */
  private Long relatedPitchId;

  /** Notes */
  private String notes;
}
