import { describe, it, expect } from 'vitest';
import { taskEditFormErrors, taskToEditForm } from '../taskEditForm';
import type { Task } from '../../types';

const task = (over: Partial<Task>): Task => ({
  id: 7, title: 'Tidy the importer', status: 'BACKLOG', priority: 'MEDIUM', projectId: 1, ...over,
} as Task);

describe('editing a task', () => {
  it('does not ask for a cycle when the task has none', () => {
    const form = taskToEditForm(task({ cycleId: undefined }));
    expect(taskEditFormErrors(form)).toEqual({});
  });

  it('locates a task without a cycle by its project, as the server requires', () => {
    // The API sends null for a missing cycle; it must not reach the form as null.
    const form = taskToEditForm(task({ cycleId: null as unknown as undefined }));
    expect(form.cycleId).toBeUndefined();
    expect(form.projectId).toBe(1);
  });

  it('keeps the cycle of a task that has one and leaves the project to the cycle', () => {
    const form = taskToEditForm(task({ cycleId: 18 }));
    expect(form.cycleId).toBe(18);
    expect(form.projectId).toBeUndefined();
  });

  it('still requires a title', () => {
    expect(taskEditFormErrors(taskToEditForm(task({ title: '  ' })))).toEqual({ title: 'taskDetail.titleRequired' });
  });
});
