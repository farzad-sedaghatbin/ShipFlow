import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SprintBoardPage from '../SprintBoardPage';
import { cycleService } from '../../services/cycleService';
import { taskService } from '../../services/taskService';

vi.mock('../../services/cycleService', () => ({
  cycleService: {
    getActiveByProject: vi.fn(),
  },
}));

vi.mock('../../services/taskService', () => ({
  taskService: {
    getByCycleId: vi.fn(),
    updateStatus: vi.fn(),
    delete: vi.fn(),
  },
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: any) =>
      opts?.name != null ? `${key}:${opts.name}` : key,
    i18n: { language: 'en' },
  }),
}));

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

// Default mock — SCRUM project
let mockProjectContext: any = {
  currentProject: { id: 1, name: 'Demo Project', projectKey: 'DEMO', projectType: 'SCRUM' },
  isScrumProject: true,
};

vi.mock('../../contexts/ProjectContext', () => ({
  useProject: () => mockProjectContext,
}));

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>{ui}</QueryClientProvider>
    </MemoryRouter>,
  );
}

const mockActiveCycle = {
  id: 20,
  name: 'Sprint 3',
  projectId: 1,
  startDate: '2026-09-01',
  endDate: '2026-09-14',
  phase: 'SHAPING_BUILDING' as const,
  isActive: true,
};

const mockBoardTasks = [
  { id: 1, title: 'Task A', status: 'TODO', priority: 'MEDIUM', cycleId: 20 },
  { id: 2, title: 'Task B', status: 'IN_PROGRESS', priority: 'HIGH', cycleId: 20 },
];

describe('SprintBoardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockProjectContext = {
      currentProject: { id: 1, name: 'Demo Project', projectKey: 'DEMO', projectType: 'SCRUM' },
      isScrumProject: true,
    };
    (cycleService.getActiveByProject as any).mockResolvedValue({ data: [mockActiveCycle] });
    (taskService.getByCycleId as any).mockResolvedValue({ data: mockBoardTasks });
  });

  it('redirects to /backlog when currentProject is not a SCRUM project', async () => {
    mockProjectContext = {
      currentProject: { id: 1, name: 'Shape Project', projectKey: 'SHP', projectType: 'SHAPE_UP' },
      isScrumProject: false,
    };

    renderWithClient(<SprintBoardPage />);

    await waitFor(() => {
      expect(screen.queryByText('sprintBoard.title')).not.toBeInTheDocument();
    });
    expect(cycleService.getActiveByProject).not.toHaveBeenCalled();
  });

  it('shows an empty state with a link to Sprint Planning when there is no active sprint', async () => {
    (cycleService.getActiveByProject as any).mockResolvedValue({ data: [] });

    renderWithClient(<SprintBoardPage />);

    await waitFor(() => {
      expect(screen.getByText('sprintBoard.noActiveSprint.title')).toBeInTheDocument();
    });
    expect(screen.getByText('sprintBoard.noActiveSprint.action')).toBeInTheDocument();
    expect(taskService.getByCycleId).not.toHaveBeenCalled();
  });

  it('fetches and renders the active cycle tasks on the board', async () => {
    renderWithClient(<SprintBoardPage />);

    await waitFor(() => {
      expect(taskService.getByCycleId).toHaveBeenCalledWith(20);
    });

    await waitFor(() => {
      expect(screen.getByText('Task A')).toBeInTheDocument();
      expect(screen.getByText('Task B')).toBeInTheDocument();
    });
    expect(screen.getByText('sprintBoard.activeSprint:Sprint 3')).toBeInTheDocument();
  });
});
