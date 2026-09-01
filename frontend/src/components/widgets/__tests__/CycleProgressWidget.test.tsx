import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CycleProgressWidget } from '../CycleProgressWidget';
import { cycleService } from '../../../services/cycleService';
import { pitchService } from '../../../services/pitchService';
import { taskService } from '../../../services/taskService';

vi.mock('../../../services/cycleService', () => ({
  cycleService: { getMyActiveCycles: vi.fn() },
}));
vi.mock('../../../services/pitchService', () => ({
  pitchService: { getMyPitches: vi.fn() },
}));
vi.mock('../../../services/taskService', () => ({
  taskService: { getMy: vi.fn() },
}));

function renderWithProviders(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>
  );
}

const baseCycle = {
  id: 1,
  name: 'Cycle One',
  startDate: '2026-01-01',
  endDate: '2026-02-01',
  phase: 'SHAPING_BUILDING',
  isActive: true,
};

describe('CycleProgressWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows pitch-based progress for a Shape Up cycle', async () => {
    (cycleService.getMyActiveCycles as any).mockResolvedValue({
      data: [{ ...baseCycle, id: 1, projectType: 'SHAPE_UP' }],
    });
    (pitchService.getMyPitches as any).mockResolvedValue({
      data: [
        { id: 1, cycleId: 1, status: 'DONE' },
        { id: 2, cycleId: 1, status: 'STARTED' },
      ],
    });
    (taskService.getMy as any).mockResolvedValue({ data: { content: [] } });

    renderWithProviders(<CycleProgressWidget />);

    await waitFor(() => {
      expect(screen.getByText(/1\/2/)).toBeInTheDocument();
    });
    expect(screen.getByText(/pitches/i)).toBeInTheDocument();
  });

  it('shows task-based ("stories") progress for a Scrum sprint instead of pitches', async () => {
    (cycleService.getMyActiveCycles as any).mockResolvedValue({
      data: [{ ...baseCycle, id: 2, name: 'Sprint One', projectType: 'SCRUM' }],
    });
    (pitchService.getMyPitches as any).mockResolvedValue({ data: [] });
    (taskService.getMy as any).mockResolvedValue({
      data: {
        content: [
          { id: 1, cycleId: 2, status: 'DONE' },
          { id: 2, cycleId: 2, status: 'DONE' },
          { id: 3, cycleId: 2, status: 'IN_PROGRESS' },
        ],
      },
    });

    renderWithProviders(<CycleProgressWidget />);

    await waitFor(() => {
      expect(screen.getByText(/2\/3/)).toBeInTheDocument();
    });
    expect(screen.getByText(/stories/i)).toBeInTheDocument();
  });

  it('excludes Kanban cycles (no cycle/pitch concept) even if one is somehow returned', async () => {
    (cycleService.getMyActiveCycles as any).mockResolvedValue({
      data: [{ ...baseCycle, id: 3, projectType: 'KANBAN' }],
    });
    (pitchService.getMyPitches as any).mockResolvedValue({ data: [] });
    (taskService.getMy as any).mockResolvedValue({ data: { content: [] } });

    renderWithProviders(<CycleProgressWidget />);

    await waitFor(() => {
      expect(screen.getByText(/noActiveCycles|No active cycles/i)).toBeInTheDocument();
    });
  });
});
