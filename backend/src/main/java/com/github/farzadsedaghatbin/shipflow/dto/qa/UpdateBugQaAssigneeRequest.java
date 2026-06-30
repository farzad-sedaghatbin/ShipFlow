package com.github.farzadsedaghatbin.shipflow.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for the PATCH /api/qa/bug-reports/{id}/qa-assignee endpoint. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBugQaAssigneeRequest {
  /** The person to assign as QA tester. Null means "unassign (no QA tester)". */
  private Long qaAssigneeId;
}
