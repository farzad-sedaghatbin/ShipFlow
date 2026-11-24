import { describe, it, expect } from 'vitest';
import { taskRequestPaging, pageOfFiltered, KANBAN_PAGE_SIZE } from '../backlogPaging';

describe('taskRequestPaging', () => {
  it('lets the list view page through results', () => {
    expect(taskRequestPaging('list', 3, 25)).toEqual({ page: 3, size: 25 });
  });

  it.each(['calendar', 'gantt', 'kanban'] as const)(
    'loads the whole set for the %s view, whatever the list page is',
    (view) => {
      expect(taskRequestPaging(view, 4, 10)).toEqual({ page: 0, size: KANBAN_PAGE_SIZE });
    },
  );

  it('loads the whole set for the list too while a browser-only filter is on', () => {
    // Otherwise the epic/dependency filter would only see the 10 tasks of the current page.
    expect(taskRequestPaging('list', 2, 10, true)).toEqual({ page: 0, size: KANBAN_PAGE_SIZE });
  });
});

describe('pageOfFiltered', () => {
  const items = Array.from({ length: 23 }, (_, i) => i);

  it('gives the list one page of the filtered set', () => {
    expect(pageOfFiltered(items, 'list', 0, 10)).toEqual(items.slice(0, 10));
    expect(pageOfFiltered(items, 'list', 2, 10)).toEqual([20, 21, 22]);
  });

  it('gives the views without a pager everything', () => {
    expect(pageOfFiltered(items, 'kanban', 2, 10)).toHaveLength(23);
  });
});
