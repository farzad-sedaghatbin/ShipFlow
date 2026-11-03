package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body for {@code PATCH /api/tasks/{id}/epic}. A null {@code epicId} unlinks the task. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskEpicRequest {
  private Long epicId;
}
