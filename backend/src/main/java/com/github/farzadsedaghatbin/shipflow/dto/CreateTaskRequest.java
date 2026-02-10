package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

  @NotNull(message = "Cycle ID is required")
  private Long cycleId;

  private Long pitchId;

  private Long scopeId;

  private TaskStatus status;
  private TaskPriority priority;
  private TaskCategory category;

  private BigDecimal estimateHours;
  private BigDecimal actualHours;

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
}
