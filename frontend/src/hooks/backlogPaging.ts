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
 *
 * The same holds for the list while a browser-only filter (epic, dependencies) is on: the server
 * knows nothing about that filter, so a page of 10 would be filtered down to whichever matches
 * happen to sit on it. The whole set is loaded instead and paged here with {@link pageOfFiltered}.
 */
export function taskRequestPaging(
  viewMode: ViewMode,
  page: number,
  rowsPerPage: number,
  browserFiltered = false,
): { page: number; size: number } {
  return viewMode === 'list' && !browserFiltered
    ? { page, size: rowsPerPage }
    : { page: 0, size: KANBAN_PAGE_SIZE };
}

/**
 * The rows to show after a browser-only filter ran over the whole set: one page of it in the list
 * view (which has a pager), all of it in the views that draw everything at once.
 */
export function pageOfFiltered<T>(items: T[], viewMode: ViewMode, page: number, rowsPerPage: number): T[] {
  return viewMode === 'list' ? items.slice(page * rowsPerPage, (page + 1) * rowsPerPage) : items;
}
