package com.github.farzadsedaghatbin.shipflow.dto.cooldown;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityType;
import lombok.*;

/**
 * Request DTO for updating a cooldown activity.
 * All fields are optional to support partial updates.
 * Cycle cannot be changed after creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCooldownActivityRequest {

  /** Activity type (optional update) */
  private CooldownActivityType activityType;

  /** Title (optional update) */
  private String title;

  /** Description (optional update) */
  private String description;

  /** Status (optional update) */
  private CooldownActivityStatus status;

  /** Optional assignee ID (null to unassign) */
  private Long assigneeId;

  /** Flag to indicate explicit unassignment */
  private Boolean clearAssignee;

  /** Estimated hours (optional update) */
  private Integer estimatedHours;

  /** Actual hours spent (optional update) */
  private Integer actualHours;

  /** Priority (1 = highest, optional update) */
  private Integer priority;

  /** Optional related pitch ID (null to unlink) */
  private Long relatedPitchId;

  /** Flag to indicate explicit pitch unlinking */
  private Boolean clearRelatedPitch;

  /** Notes (optional update) */
  private String notes;
}
