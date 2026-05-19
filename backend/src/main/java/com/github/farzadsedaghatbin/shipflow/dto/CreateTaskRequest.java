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
   * Direct project reference. Required when {@code cycleId} is null (SCRUM product backlog tasks).
   * Ignored when {@code cycleId} is supplied (project is derived from the cycle).
   */
  private Long projectId;

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
   * When true and pitchId is set (and no parentTaskId), auto-creates a hill chart scope.
   * Defaults to true for root tasks with pitch association.
   */
  @Builder.Default
  private Boolean createScopeAutomatically = true;

  /**
   * Initial position on the hill chart (0-100).
   * Only used when createScopeAutomatically is true.
   * Defaults to 0 (start of hill - figuring things out).
   */
  @Min(value = 0, message = "Initial hill position must be between 0 and 100")
  @Max(value = 100, message = "Initial hill position must be between 0 and 100")
  @Builder.Default
  private Integer initialHillPosition = 0;

  /**
   * Cross-field validation: at least one of {@code cycleId} or {@code projectId} must be provided.
   * This constraint is surfaced in OpenAPI and validated at the controller boundary via JSR-303,
   * so all consumers (including future API integrations and bulk imports) are covered.
   */
  @AssertTrue(message = "Task location required: provide cycleId (assign to a cycle/sprint) or projectId (SCRUM product backlog)")
  @SuppressWarnings("unused") // invoked by the Bean Validation framework
  private boolean isTaskLocationValid() {
    return cycleId != null || projectId != null;
  }
}
