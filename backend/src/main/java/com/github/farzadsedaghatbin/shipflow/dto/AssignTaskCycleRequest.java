package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for the PATCH /api/tasks/{id}/cycle endpoint. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignTaskCycleRequest {
  /** The cycle (sprint) to assign the task to. Null means "move to product backlog (no cycle)". */
  private Long cycleId;
}
