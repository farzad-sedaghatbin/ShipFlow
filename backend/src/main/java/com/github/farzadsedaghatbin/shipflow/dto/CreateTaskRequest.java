package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {

  @NotBlank(message = "Title is required")
  private String title;

  private String description;

  /**
   * ID of the sprint/cycle to assign the task to. Optional for SCRUM projects — when null and
   * {@code projectId} is supplied the task is placed in the product backlog (cycle = null).
   */
  private Long cycleId;

  /**
   * Direct project reference. Used when {@code cycleId} is null and the task isn't pitch-linked
   * (SCRUM product backlog / Shape Up Debt-Improvement tasks). Ignored when {@code cycleId} is
   * supplied (project is derived from the cycle).
   */
  private Long projectId;

  /**
   * Pitch to link this task to. When neither {@code cycleId} nor {@code projectId} is supplied,
   * the task's cycle and project are derived from this pitch (cycle follows the pitch's current
   * bet, which may be {@code null}; project follows the same cycle→pitch→epic fallback chain
   * used by {@code PitchService#toDTO}).
   */
  private Long pitchId;

  private Long scopeId;

  private TaskStatus status;
  private TaskPriority priority;
  private TaskCategory category;

  private BigDecimal estimateHours;
  private BigDecimal actualHours;

  @Min(value = 0, message = "Story points must be 0 or greater")
  @Max(value = 999, message = "Story points must not exceed 999")
  private Integer storyPoints;

  private Long teamId;

  private Long assigneeId;
  private Long pairAssigneeId;

  private Long parentTaskId;

  private LocalDate dueDate;

  private String tags;

  /**
   * Initial position on the hill chart (0-100) for the auto-created scope.
   * When null (the usual case), the position is derived from the task's status
   * so the scope reflects real progress immediately instead of being pinned at 0.
   */
  @Min(value = 0, message = "Initial hill position must be between 0 and 100")
  @Max(value = 100, message = "Initial hill position must be between 0 and 100")
  private Integer initialHillPosition;

  /**
   * Cross-field validation: at least one of {@code cycleId}, {@code projectId}, or {@code
   * pitchId} must be provided. A pitch-linked task with neither an explicit cycle nor an
   * explicit project derives both from the pitch (its current bet's cycle/project, or the
   * epic's project chain when not yet bet) — see {@code TaskService#createTask}. This constraint
   * is surfaced in OpenAPI and validated at the controller boundary via JSR-303, so all consumers
   * (including future API integrations and bulk imports) are covered.
   */
  @AssertTrue(message = "Task location required: provide cycleId (assign to a cycle/sprint), projectId (SCRUM product backlog / Debt-Improvement task), or pitchId (cycle/project derived from the pitch)")
  @SuppressWarnings("unused") // invoked by the Bean Validation framework
  private boolean isTaskLocationValid() {
    return cycleId != null || projectId != null || pitchId != null;
  }
}
