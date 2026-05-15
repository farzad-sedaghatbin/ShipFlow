import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SprintPlanningPage from '../SprintPlanningPage';
import { cycleService } from '../../services/cycleService';
import { taskService } from '../../services/taskService';

vi.mock('../../services/cycleService', () => ({
  cycleService: {
    getByProject: vi.fn(),
    getBurndown: vi.fn().mockResolvedValue({ data: [] }),
    getVelocity: vi.fn().mockResolvedValue({ data: [] }),
  },
}));

vi.mock('../../services/taskService', () => ({
  taskService: {
    getByProjectId: vi.fn(),
    getByCycleId: vi.fn(),
    update: vi.fn(),
  },
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: any) => (opts?.count != null ? `${key}:${opts.count}` : key),
    i18n: { language: 'en' },
  }),
}));

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

vi.mock('../../contexts/ProjectContext', () => ({
  useProject: () => ({
    currentProject: { id: 1, name: 'Demo Project', projectKey: 'DEMO', projectType: 'SCRUM' },
  }),
}));

vi.mock('recharts', async () => {
  const actual: any = await vi.importActual('recharts');
  return {
    ...actual,
    ResponsiveContainer: ({ children }: any) => <div>{children}</div>,
  };
});

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

const mockCycles = [
  {
    id: 10,
    name: 'Sprint 1',
    projectId: 1,
    startDate: '2026-05-01',
    endDate: '2026-05-14',
    phase: 'SHAPING_BUILDING' as const,
    isActive: true,
    sprintGoal: 'Ship the new dashboard',
  },
  {
    id: 11,
    name: 'Sprint 2',
    projectId: 1,
    startDate: '2026-05-15',
    endDate: '2026-05-28',
    phase: 'SHAPING_BUILDING' as const,
    isActive: false,
  },
];

const mockBacklogTasks = [
  {
    id: 1, title: 'Task A', status: 'BACKLOG', priority: 'MEDIUM',
    storyPoints: 5, cycleId: null,
  },
  {
    id: 2, title: 'Task B', status: 'TODO', priority: 'HIGH',
    storyPoints: 3, cycleId: null,
  },
  {
    id: 3, title: 'Task C (already in sprint)', status: 'TODO', priority: 'LOW',
    storyPoints: 8, cycleId: 10,
  },
];

const mockSprintTasks = [
  {
    id: 3, title: 'Task C', status: 'TODO', priority: 'LOW',
    storyPoints: 8, cycleId: 10,
  },
  {
    id: 4, title: 'Task D', status: 'IN_PROGRESS', priority: 'HIGH',
    storyPoints: 13, cycleId: 10,
  },
];

describe('SprintPlanningPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (cycleService.getByProject as any).mockResolvedValue({ data: mockCycles });
    (taskService.getByProjectId as any).mockResolvedValue({ data: mockBacklogTasks });
    (taskService.getByCycleId as any).mockResolvedValue({ data: mockSprintTasks });
  });

  it('renders the two-column planning board with backlog and sprint headers', async () => {
    renderWithClient(<SprintPlanningPage />);

    await waitFor(() => {
      expect(screen.getByText('sprintPlanning.productBacklog')).toBeInTheDocument();
    });
    expect(screen.getByText('sprintPlanning.sprintBacklog')).toBeInTheDocument();
  });

  it('calculates total story points for the product backlog', async () => {
    renderWithClient(<SprintPlanningPage />);

    // 5 + 3 = 8 (Task C is filtered out — has cycleId 10)
    await waitFor(() => {
      expect(screen.getByText('sprintPlanning.totalPoints:8')).toBeInTheDocument();
    });
  });

  it('shows the sprint selector with cycles', async () => {
    renderWithClient(<SprintPlanningPage />);

    await waitFor(() => {
      expect(screen.getByText('sprintPlanning.selectSprint:')).toBeInTheDocument();
    });

    // The Select trigger placeholder and burndown empty state both render this text
    expect(screen.getAllByText('sprintPlanning.noSprint').length).toBeGreaterThan(0);
  });

  it('renders backlog tasks', async () => {
    renderWithClient(<SprintPlanningPage />);

    await waitFor(() => {
      expect(screen.getByText('Task A')).toBeInTheDocument();
      expect(screen.getByText('Task B')).toBeInTheDocument();
    });
  });
});
