package com.github.farzadsedaghatbin.shipflow.entity.enums;

/** Actions that can be applied to a batch of tasks in a single bulk-update call. */
public enum BulkAction {
  /** Assign all tasks to a person. value = personId (numeric string), or blank to unassign. */
  ASSIGN,
  /** Change status of all tasks. value = TaskStatus name. */
  CHANGE_STATUS,
  /** Change priority of all tasks. value = TaskPriority name. */
  CHANGE_PRIORITY,
  /** Append a tag to each task's tag list. value = tag string. */
  ADD_TAG,
  /** Soft-delete all tasks. value is ignored. */
  DELETE
}
