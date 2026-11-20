import { describe, it, expect } from 'vitest';
import { taskRequestPaging, KANBAN_PAGE_SIZE } from '../backlogPaging';

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
});
