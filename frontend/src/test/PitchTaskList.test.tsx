import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { PitchTaskList } from '../components/pitchDetail/PitchTaskList';
import { Task } from '../types';

function makeTask(overrides: Partial<Task> & { id: number; title: string }): Task {
  return {
    status: 'TODO',
    priority: 'MEDIUM',
    cycleId: 1,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  } as Task;
}

const noop = async () => {};

describe('PitchTaskList', () => {
  it('shows only top-level tasks, collapsing sub-tasks by default', () => {
    const tasks = [
      makeTask({ id: 1, title: 'Main task' }),
      makeTask({ id: 2, title: 'Hidden subtask', parentTaskId: 1 }),
    ];

    render(
      <PitchTaskList
        tasks={tasks}
        onStatusChange={noop}
        onViewTask={vi.fn()}
        onEditTask={vi.fn()}
        onDeleteTask={vi.fn()}
        onAddSubtask={vi.fn()}
      />
    );

    expect(screen.getByText('Main task')).toBeInTheDocument();
    expect(screen.queryByText('Hidden subtask')).not.toBeInTheDocument();
  });

  it('reveals a task\'s sub-tasks after expanding it', () => {
    const tasks = [
      makeTask({ id: 1, title: 'Main task' }),
      makeTask({ id: 2, title: 'Sub task A', parentTaskId: 1 }),
    ];

    render(
      <PitchTaskList
        tasks={tasks}
        onStatusChange={noop}
        onViewTask={vi.fn()}
        onEditTask={vi.fn()}
        onDeleteTask={vi.fn()}
        onAddSubtask={vi.fn()}
      />
    );

    expect(screen.queryByText('Sub task A')).not.toBeInTheDocument();

    const [expandButton] = screen.getAllByRole('button');
    fireEvent.click(expandButton);

    expect(screen.getByText('Sub task A')).toBeInTheDocument();
  });

  it('treats a sub-task whose parent is missing from the list as top-level', () => {
    const tasks = [
      makeTask({ id: 2, title: 'Orphan subtask', parentTaskId: 999 }),
    ];

    render(
      <PitchTaskList
        tasks={tasks}
        onStatusChange={noop}
        onViewTask={vi.fn()}
        onEditTask={vi.fn()}
        onDeleteTask={vi.fn()}
        onAddSubtask={vi.fn()}
      />
    );

    expect(screen.getByText('Orphan subtask')).toBeInTheDocument();
  });

  it('does not render a stray "0" next to a task with zero blocked/blocking counts', () => {
    // Regression: `count && count > 0` short-circuits to the number 0 (not false) when count is
    // exactly 0, and `{0 && <Badge/>}` renders a bare "0" text node in JSX — React only suppresses
    // false/null/undefined children, not 0. Every task defaults to blockedByCount: 0 and an empty
    // blockingTasks array, so this used to show a stray "0" next to every single task's title.
    const tasks = [makeTask({ id: 1, title: 'Clean task', blockedByCount: 0, blockingTasks: [] })];

    const { container } = render(
      <PitchTaskList
        tasks={tasks}
        onStatusChange={noop}
        onViewTask={vi.fn()}
        onEditTask={vi.fn()}
        onDeleteTask={vi.fn()}
        onAddSubtask={vi.fn()}
      />
    );

    expect(screen.getByText('Clean task')).toBeInTheDocument();
    expect(container.textContent).not.toContain('0');
  });

  it('calls onViewTask when a task title is clicked', () => {
    const onViewTask = vi.fn();
    const tasks = [makeTask({ id: 1, title: 'Main task' })];

    render(
      <PitchTaskList
        tasks={tasks}
        onStatusChange={noop}
        onViewTask={onViewTask}
        onEditTask={vi.fn()}
        onDeleteTask={vi.fn()}
        onAddSubtask={vi.fn()}
      />
    );

    fireEvent.click(screen.getByText('Main task'));
    expect(onViewTask).toHaveBeenCalledWith(tasks[0]);
  });
});
