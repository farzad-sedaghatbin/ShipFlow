import { Task } from '../types';

/**
 * Reorders a flat task list so each sub-task renders directly after its
 * parent, instead of wherever the current sort happens to scatter it. This
 * is the only structural grouping available for Kanban-mode projects (no
 * Pitch concept there) — used by both BacklogTaskTable (list view) and
 * KanbanBoard (within a status column).
 *
 * Only children already present in `tasks` are nested — a child excluded by
 * the caller's active filter/page/column simply doesn't appear, same as it
 * wouldn't if rendered standalone. Never adds or drops items, only reorders,
 * so callers that need `tasks.length` (pagination, drag-to-reorder maths)
 * stay consistent.
 */
export function groupTasksByParent(tasks: Task[]): Task[] {
  const byId = new Map(tasks.map((t) => [t.id, t]));
  const rendered = new Set<number>();
  const result: Task[] = [];

  for (const task of tasks) {
    if (rendered.has(task.id)) continue;
    if (task.parentTaskId != null && byId.has(task.parentTaskId)) {
      // Rendered under its parent below - skip its own top-level slot.
      continue;
    }
    result.push(task);
    rendered.add(task.id);
    if (task.children && task.children.length > 0) {
      for (const child of task.children) {
        const liveChild = byId.get(child.id);
        if (liveChild && !rendered.has(liveChild.id)) {
          result.push(liveChild);
          rendered.add(liveChild.id);
        }
      }
    }
  }

  return result;
}
