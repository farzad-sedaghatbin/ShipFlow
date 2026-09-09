package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.dto.pitch.TaskSuggestionDTO;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * Request body for POST /api/tasks/bulk-create — creates multiple tasks under a single cycle (and
 * optionally a pitch) in one transaction, e.g. from a batch of accepted AI task suggestions.
 *
 * <p>{@code pitchId} is optional: when present, the created tasks are pitch-scoped ({@code
 * PITCH_SCOPE}); when absent (e.g. Scrum sprint-suggested tasks with no pitch), they're created as
 * {@code DEBT_IMPROVEMENT} tasks scoped directly to the cycle. See {@link
 * com.github.farzadsedaghatbin.shipflow.service.TaskService#bulkCreate}.
 */
@Data
public class BulkCreateTaskRequest {

  private Long pitchId;

  @NotNull(message = "Cycle ID is required")
  private Long cycleId;

  @NotEmpty(message = "At least one task is required")
  private List<TaskSuggestionDTO> tasks;
}
