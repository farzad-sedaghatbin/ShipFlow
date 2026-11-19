import type { ViewMode } from '../components/backlog';

// Large page size for Kanban and the other whole-set views — fetches effectively all tasks.
export const KANBAN_PAGE_SIZE = 500;

/**
 * Which page of tasks to request for the current view.
 *
 * Only the list view has a pager, so only it may ask for "page N of `rowsPerPage`". The board,
 * Gantt and calendar views draw every task at once and have no pager: if they inherited the
 * list's page of 10 they would show 10 tasks and silently drop the rest (an empty calendar when
 * the newest 10 tasks have no due date).
 */
export function taskRequestPaging(
  viewMode: ViewMode,
  page: number,
  rowsPerPage: number,
): { page: number; size: number } {
  return viewMode === 'list'
    ? { page, size: rowsPerPage }
    : { page: 0, size: KANBAN_PAGE_SIZE };
}
