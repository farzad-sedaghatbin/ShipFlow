import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BacklogViewDialog } from '../BacklogViewDialog';
import { Task } from '../../../types';

// Covers the "Kanban task view has no attachment/comment ability" fix — the
// Kanban board's task view dialog now includes TaskAttachments and Comments,
// matching what TaskDetailPage.tsx already renders for the full task page.

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('../../TaskAttachments', () => ({
  default: ({ taskId }: { taskId: number }) => <div data-testid="task-attachments">attachments:{taskId}</div>,
}));

vi.mock('../../Comments', () => ({
  default: ({ entityType, entityId }: { entityType: string; entityId: number }) => (
    <div data-testid="task-comments">comments:{entityType}:{entityId}</div>
  ),
}));

vi.mock('../../TaskDependencies', () => ({
  default: () => <div data-testid="task-dependencies" />,
}));

vi.mock('../../../services/taskService', () => ({
  taskService: { getById: vi.fn() },
}));

const task: Task = {
  id: 42,
  title: 'A kanban task',
  status: 'TODO',
  priority: 'MEDIUM',
  projectId: 1,
} as Task;

describe('BacklogViewDialog', () => {
  it('renders TaskAttachments and Comments for the viewed task', () => {
    render(
      <BacklogViewDialog
        open={true}
        task={task}
        subtasks={[]}
        viewHistory={[]}
        onClose={vi.fn()}
        onBack={vi.fn()}
        onViewTask={vi.fn()}
        onEditTask={vi.fn()}
        onReloadTasks={vi.fn()}
        setViewDialogTask={vi.fn()}
      />
    );

    expect(screen.getByTestId('task-attachments')).toHaveTextContent('attachments:42');
    expect(screen.getByTestId('task-comments')).toHaveTextContent('comments:task:42');
  });
});
