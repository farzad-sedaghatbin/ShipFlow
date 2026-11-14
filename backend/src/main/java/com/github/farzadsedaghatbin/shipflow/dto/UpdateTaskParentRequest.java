package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for {@code PATCH /api/tasks/{id}/parent}. A non-null {@code parentTaskId} turns the task
 * into a subtask of that parent; null turns a subtask back into a top-level task.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskParentRequest {
  private Long parentTaskId;
}
