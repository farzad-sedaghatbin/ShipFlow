package com.github.farzadsedaghatbin.shipflow.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for the PATCH /api/qa/bug-reports/{id}/assignee endpoint. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBugAssigneeRequest {
  /** The person to assign to fix the bug. Null means "unassign". */
  private Long assigneeId;
}
