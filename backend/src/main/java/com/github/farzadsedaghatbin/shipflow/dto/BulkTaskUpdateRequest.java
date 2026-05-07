package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BulkAction;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * Request body for POST /api/tasks/bulk-update.
 *
 * <p>All task IDs must belong to the same project. The {@code value} field carries the
 * action-specific payload:
 *
 * <ul>
 *   <li>ASSIGN — person ID as a numeric string, or blank to unassign
 *   <li>CHANGE_STATUS — {@link com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus} name
 *   <li>CHANGE_PRIORITY — {@link
 *       com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority} name
 *   <li>ADD_TAG — tag string to append
 *   <li>DELETE — ignored
 * </ul>
 */
@Data
public class BulkTaskUpdateRequest {

  @NotEmpty(message = "Task IDs must not be empty")
  private List<Long> taskIds;

  @NotNull(message = "Action must not be null")
  private BulkAction action;

  /** Action-specific value. May be null/blank for DELETE and ASSIGN (unassign). */
  private String value;
}
