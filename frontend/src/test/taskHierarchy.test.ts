import { describe, it, expect } from 'vitest';
import { groupTasksByParent } from '../utils/taskHierarchy';
import type { Task } from '../types';

function mockTask(overrides: Partial<Task> & { id: number }): Task {
  return {
    title: `Task ${overrides.id}`,
    status: 'BACKLOG',
    priority: 'MEDIUM',
    cycleId: 1,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('groupTasksByParent', () => {
  it('moves a sub-task to render directly after its parent, regardless of input order', () => {
    const parent = mockTask({ id: 1, title: 'Parent', children: [{ id: 2 } as Task] });
    const child = mockTask({ id: 2, title: 'Child', parentTaskId: 1 });
    const unrelated = mockTask({ id: 3, title: 'Unrelated' });

    // Input order deliberately puts the child before its parent (e.g. sorted
    // alphabetically by title) - it should still end up right after it.
    // Top-level relative order (parent, then unrelated) is otherwise preserved.
    const result = groupTasksByParent([child, parent, unrelated]);

    expect(result.map((t) => t.id)).toEqual([1, 2, 3]);
  });

  it('keeps a child at its own position when its parent is not in the list', () => {
    const orphan = mockTask({ id: 5, title: 'Orphan child', parentTaskId: 99 });
    const other = mockTask({ id: 6, title: 'Other' });

    const result = groupTasksByParent([orphan, other]);

    expect(result.map((t) => t.id)).toEqual([5, 6]);
  });

  it('never renders a child twice, even if listed in both the flat array and parent.children', () => {
    const parent = mockTask({ id: 1, children: [{ id: 2 } as Task, { id: 3 } as Task] });
    const child1 = mockTask({ id: 2, parentTaskId: 1 });
    const child2 = mockTask({ id: 3, parentTaskId: 1 });

    const result = groupTasksByParent([parent, child1, child2]);

    expect(result.map((t) => t.id)).toEqual([1, 2, 3]);
    expect(result).toHaveLength(3);
  });

  it('preserves the live (fuller) task object for a child rather than the summary from parent.children', () => {
    const parent = mockTask({
      id: 1,
      children: [{ id: 2, title: 'Stale summary title' } as Task],
    });
    const liveChild = mockTask({ id: 2, title: 'Live child title', status: 'IN_PROGRESS' });

    const result = groupTasksByParent([parent, liveChild]);

    expect(result[1].title).toBe('Live child title');
    expect(result[1].status).toBe('IN_PROGRESS');
  });

  it('leaves tasks with no parent/child relationships in their original relative order', () => {
    const a = mockTask({ id: 1 });
    const b = mockTask({ id: 2 });
    const c = mockTask({ id: 3 });

    const result = groupTasksByParent([a, b, c]);

    expect(result.map((t) => t.id)).toEqual([1, 2, 3]);
  });

  it('never adds or drops items - output length always matches input length', () => {
    const parent = mockTask({ id: 1, children: [{ id: 2 } as Task] });
    const child = mockTask({ id: 2, parentTaskId: 1 });
    const orphan = mockTask({ id: 3, parentTaskId: 99 });
    const standalone = mockTask({ id: 4 });

    const input = [orphan, child, standalone, parent];
    const result = groupTasksByParent(input);

    expect(result).toHaveLength(input.length);
  });

  it('returns an empty array for empty input', () => {
    expect(groupTasksByParent([])).toEqual([]);
  });
});
