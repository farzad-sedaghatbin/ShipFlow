import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

const showError = vi.fn();
const showSuccess = vi.fn();
// Stable references: a fresh object per render would re-trigger the page's load effect and remount the dialog.
const projectContext = { currentProject: { id: 1, name: 'Kixy' }, isAllProjectsSelected: false, isScrumProject: false };
const toastContext = { showError, showSuccess };
vi.mock('../../contexts', () => ({
  useProject: () => projectContext,
  useToast: () => toastContext,
}));
vi.mock('../../services/cycleService', () => ({
  cycleService: {
    getByProject: vi.fn(),
    getMyCycles: vi.fn(),
    getDeleteImpact: vi.fn(),
    delete: vi.fn(),
    toggleActive: vi.fn(),
  },
}));

import { cycleService } from '../../services/cycleService';
import CycleList from '../CycleList';

const svc = vi.mocked(cycleService);

const cycle = {
  id: 22,
  name: 'Cycle 10',
  startDate: '2026-10-05',
  endDate: '2026-11-02',
  phase: 'SHAPING_BUILDING',
  isActive: false,
  pitchCount: 0,
  taskCount: 15,
  teamCount: 0,
  projectId: 1,
};

function renderList() {
  svc.getByProject.mockResolvedValue({ data: [cycle] } as never);
  return render(
    <MemoryRouter>
      <CycleList />
    </MemoryRouter>,
  );
}

async function openDeleteDialog() {
  const triggers = await screen.findAllByLabelText('cycles.deleteCycleTooltip');
  fireEvent.click(triggers[0]);
}

describe('CycleList — delete shows what the cascade will remove', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lists the tasks, pitches and retros that will be deleted and blocks Delete until the name is typed', async () => {
    svc.getDeleteImpact.mockResolvedValue({
      data: { cycleId: 22, cycleName: 'Cycle 10', taskCount: 15, doneTaskCount: 4, pitchCount: 2, retrospectiveCount: 1 },
    } as never);
    renderList();
    await openDeleteDialog();

    const impact = await screen.findByTestId('cycle-delete-impact');
    await waitFor(() => expect(impact).toHaveTextContent('cycles.deleteImpactTasks'));
    expect(impact).toHaveTextContent('cycles.deleteImpactPitches');
    expect(impact).toHaveTextContent('cycles.deleteImpactRetros');
    expect(svc.getDeleteImpact).toHaveBeenCalledWith(22);

    const deleteButton = screen.getByRole('button', { name: 'common.delete' });
    expect(deleteButton).toBeDisabled();

    const confirm = screen.getByPlaceholderText('Cycle 10');
    fireEvent.change(confirm, { target: { value: 'Cycle 1' } });
    expect(deleteButton).toBeDisabled();

    fireEvent.change(confirm, { target: { value: 'Cycle 10' } });
    expect(deleteButton).toBeEnabled();

    svc.delete.mockResolvedValue({} as never);
    fireEvent.click(deleteButton);
    await waitFor(() => expect(svc.delete).toHaveBeenCalledWith(22));
  });

  it('lets an empty cycle be deleted without typing anything', async () => {
    svc.getDeleteImpact.mockResolvedValue({
      data: { cycleId: 22, cycleName: 'Cycle 10', taskCount: 0, doneTaskCount: 0, pitchCount: 0, retrospectiveCount: 0 },
    } as never);
    renderList();
    await openDeleteDialog();

    await screen.findByText('cycles.deleteImpactEmpty');
    expect(screen.getByRole('button', { name: 'common.delete' })).toBeEnabled();
    expect(screen.queryByPlaceholderText('Cycle 10')).not.toBeInTheDocument();
  });

  it('assumes the cycle is not empty when the impact cannot be loaded', async () => {
    svc.getDeleteImpact.mockRejectedValue(new Error('boom'));
    renderList();
    await openDeleteDialog();

    await screen.findByText('cycles.deleteImpactFailed');
    expect(screen.getByRole('button', { name: 'common.delete' })).toBeDisabled();
    expect(screen.getByPlaceholderText('Cycle 10')).toBeInTheDocument();
  });

  it('does not allow deleting while the impact is still loading', async () => {
    svc.getDeleteImpact.mockReturnValue(new Promise(() => {}) as never);
    renderList();
    await openDeleteDialog();

    await screen.findByText('cycles.deleteImpactLoading');
    expect(screen.getByRole('button', { name: 'common.delete' })).toBeDisabled();
    expect(svc.delete).not.toHaveBeenCalled();
  });
});
