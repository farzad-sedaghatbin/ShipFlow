import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const showError = vi.fn();
const showSuccess = vi.fn();
vi.mock('../../contexts', () => ({ useToast: () => ({ showError, showSuccess }) }));
vi.mock('../../services/taskService', () => ({
  taskService: { getByProjectId: vi.fn().mockResolvedValue({ data: [] }), create: vi.fn(), update: vi.fn() },
}));
vi.mock('../../services/cycleService', () => ({ cycleService: { getActiveByProject: vi.fn() } }));
vi.mock('../../services/epicService', () => ({ epicService: { linkTask: vi.fn(), unlinkTask: vi.fn() } }));

import { taskService } from '../../services/taskService';
import EpicTasksSection from '../EpicTasksSection';

const renderIt = (onChanged = vi.fn()) =>
  render(
    <MemoryRouter>
      <EpicTasksSection epicId={1} projectId={2} projectType="SCRUM" tasks={[]} onChanged={onChanged} />
    </MemoryRouter>,
  );

describe('EpicTasksSection — creating a task', () => {
  beforeEach(() => {
    showError.mockReset();
    showSuccess.mockReset();
    vi.mocked(taskService.create).mockReset();
  });

  it('keeps the New Task button clickable even with an empty title', () => {
    renderIt();
    expect(screen.getByRole('button', { name: /epics\.newTask/ })).toBeEnabled();
  });

  it('an empty title explains what is missing, focuses the field and creates nothing', () => {
    renderIt();

    fireEvent.click(screen.getByRole('button', { name: /epics\.newTask/ }));

    expect(showError).toHaveBeenCalledWith('epics.taskTitleRequired');
    expect(screen.getByLabelText('epics.newTask')).toHaveFocus();
    expect(taskService.create).not.toHaveBeenCalled();
  });

  it('a whitespace-only title counts as empty', () => {
    renderIt();
    fireEvent.change(screen.getByLabelText('epics.newTask'), { target: { value: '   ' } });

    fireEvent.click(screen.getByRole('button', { name: /epics\.newTask/ }));

    expect(showError).toHaveBeenCalledWith('epics.taskTitleRequired');
    expect(taskService.create).not.toHaveBeenCalled();
  });

  it('with a title it creates the task in the epic and tells the parent', async () => {
    vi.mocked(taskService.create).mockResolvedValue({ data: {} } as never);
    const onChanged = vi.fn();
    renderIt(onChanged);
    fireEvent.change(screen.getByLabelText('epics.newTask'), { target: { value: 'Write spec' } });

    fireEvent.click(screen.getByRole('button', { name: /epics\.newTask/ }));

    await waitFor(() => expect(onChanged).toHaveBeenCalled());
    expect(taskService.create).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'Write spec', epicId: 1, projectId: 2 }),
    );
    expect(showError).not.toHaveBeenCalled();
  });
});
