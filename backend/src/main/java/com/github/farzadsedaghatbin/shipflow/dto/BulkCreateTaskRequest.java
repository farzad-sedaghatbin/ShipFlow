package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.dto.pitch.TaskSuggestionDTO;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * Request body for POST /api/tasks/bulk-create — creates multiple tasks under a single pitch and
 * cycle in one transaction, e.g. from a batch of accepted AI task suggestions.
 */
@Data
public class BulkCreateTaskRequest {

  @NotNull(message = "Pitch ID is required")
  private Long pitchId;

  @NotNull(message = "Cycle ID is required")
  private Long cycleId;

  @NotEmpty(message = "At least one task is required")
  private List<TaskSuggestionDTO> tasks;
}
