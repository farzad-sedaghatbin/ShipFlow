import type { CreateTaskRequest, Task } from '../types';

/**
 * The edit form's starting values for a task.
 *
 * A task does not need a cycle: it may sit in the backlog without one, or be linked to a pitch
 * that is not bet yet. Such a task is located by its project instead, and the server needs one of
 * the two, so `projectId` is sent whenever there is no cycle.
 */
export function taskToEditForm(task: Task): CreateTaskRequest {
  return {
    title: task.title,
    description: task.description || '',
    cycleId: task.cycleId ?? undefined,
    projectId: !task.cycleId ? task.projectId : undefined,
    status: task.status,
    priority: task.priority,
    estimateHours: task.estimateHours,
    teamId: task.teamId,
    assigneeId: task.assigneeId,
    pairAssigneeId: task.pairAssigneeId,
    dueDate: task.dueDate,
    tags: task.tags || '',
    category: task.category,
    parentTaskId: task.parentTaskId,
    pitchId: task.pitchId,
  };
}

/** Field → i18n key of what is wrong with it. A cycle is optional, so it is never an error. */
export function taskEditFormErrors(form: CreateTaskRequest): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!form.title.trim()) errors.title = 'taskDetail.titleRequired';
  return errors;
}
