package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for the PATCH /api/tasks/{id}/assignee endpoint. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskAssigneeRequest {
  /** The person to assign the task to. Null means "unassign (no assignee)". */
  private Long assigneeId;
}
