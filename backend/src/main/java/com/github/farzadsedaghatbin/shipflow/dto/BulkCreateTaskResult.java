package com.github.farzadsedaghatbin.shipflow.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/** Response returned by POST /api/tasks/bulk-create. */
@Data
@Builder
public class BulkCreateTaskResult {
  private int successCount;
  private int failureCount;
  private List<String> errors;
  private List<TaskDTO> createdTasks;
}
