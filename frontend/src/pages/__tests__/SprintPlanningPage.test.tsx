import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SprintPlanningPage from '../SprintPlanningPage';
import { cycleService } from '../../services/cycleService';
import { taskService } from '../../services/taskService';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual: any = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../../services/cycleService', () => ({
  cycleService: {
    getByProject: vi.fn(),
    getBurndown: vi.fn().mockResolvedValue({ data: [] }),
    getVelocity: vi.fn().mockResolvedValue({ data: [] }),
  },
}));

vi.mock('../../services/taskService', () => ({
  taskService: {
    getProductBacklogTasks: vi.fn(),
    getByCycleId: vi.fn(),
    assignCycle: vi.fn(),
  },
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: any) => (opts?.points != null ? `${key}:${opts.points}` : key),
    i18n: { language: 'en' },
  }),
}));

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

// Mock Radix Select with a plain <select> to avoid jsdom hasPointerCapture issues
vi.mock('@/components/ui/select', () => ({
  Select: ({ value, onValueChange, children }: any) => (
    <select
      data-testid="cycle-select"
      value={value ?? ''}
      onChange={(e) => onValueChange(e.target.value)}
    >
      {children}
    </select>
  ),
  SelectTrigger: ({ children }: any) => <>{children}</>,
  SelectValue: ({ placeholder }: any) => <span>{placeholder}</span>,
  SelectContent: ({ children }: any) => <>{children}</>,
  SelectItem: ({ value, children }: any) => <option value={value}>{children}</option>,
}));

// Default mock — SCRUM project
let mockProjectContext: any = {
  currentProject: { id: 1, name: 'Demo Project', projectKey: 'DEMO', projectType: 'SCRUM' },
  isScrumProject: true,
};

vi.mock('../../contexts/ProjectContext', () => ({
  useProject: () => mockProjectContext,
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
  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>{ui}</QueryClientProvider>
    </MemoryRouter>,
  );
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
    mockProjectContext = {
      currentProject: { id: 1, name: 'Demo Project', projectKey: 'DEMO', projectType: 'SCRUM' },
      isScrumProject: true,
    };
    (cycleService.getByProject as any).mockResolvedValue({ data: mockCycles });
    // getProductBacklogTasks returns only tasks with no sprint assignment (ids 1 and 2)
    (taskService.getProductBacklogTasks as any).mockResolvedValue({
      data: mockBacklogTasks.filter((t) => !t.cycleId),
    });
    (taskService.getByCycleId as any).mockResolvedValue({ data: mockSprintTasks });
    (taskService.assignCycle as any).mockResolvedValue({ data: {} });
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

  // ── Fix 7a: move to sprint calls assignCycle(taskId, cycleId) ──────────────
  it('calls taskService.assignCycle with taskId and cycleId when moving a task to a sprint', async () => {
    const user = userEvent.setup();
    renderWithClient(<SprintPlanningPage />);

    // Wait for backlog tasks to render
    await waitFor(() => expect(screen.getByText('Task A')).toBeInTheDocument());

    // Select Sprint 1 (id=10) using the native select mock — avoids Radix pointer-capture
    const cycleSelect = screen.getByTestId('cycle-select');
    fireEvent.change(cycleSelect, { target: { value: '10' } });

    // Click the ArrowRight (move to sprint) button for Task A
    const moveToSprintButtons = await screen.findAllByLabelText('sprintPlanning.moveToSprint');
    await user.click(moveToSprintButtons[0]);

    await waitFor(() => {
      expect(taskService.assignCycle).toHaveBeenCalledWith(1, 10);
    });
  });

  // ── Fix 7b: move to backlog calls assignCycle(taskId, null) ───────────────
  it('calls taskService.assignCycle with taskId and null when moving a task to backlog', async () => {
    const user = userEvent.setup();
    renderWithClient(<SprintPlanningPage />);

    // Wait for data to load
    await waitFor(() => expect(screen.getByText('Task A')).toBeInTheDocument());

    // Select Sprint 1 so sprint tasks are fetched and shown
    const cycleSelect = screen.getByTestId('cycle-select');
    fireEvent.change(cycleSelect, { target: { value: '10' } });

    // Sprint tasks should now be visible (Task C, id=3)
    await waitFor(() => expect(screen.getByText('Task C')).toBeInTheDocument());

    // Click the ArrowLeft (move to backlog) button for the first sprint task
    const moveToBacklogButtons = await screen.findAllByLabelText('sprintPlanning.moveToBacklog');
    await user.click(moveToBacklogButtons[0]);

    await waitFor(() => {
      expect(taskService.assignCycle).toHaveBeenCalledWith(3, null);
    });
  });

  // ── Fix 7c: non-SCRUM project redirects to /backlog ───────────────────────
  it('navigates to /backlog when currentProject is not a SCRUM project', async () => {
    mockProjectContext = {
      currentProject: { id: 1, name: 'Shape Project', projectKey: 'SHP', projectType: 'SHAPE_UP' },
      isScrumProject: false,
    };

    renderWithClient(<SprintPlanningPage />);

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/backlog', { replace: true });
    });
  });

  // ── Fix 7d: changing cycle selector re-queries sprint tasks ───────────────
  it('re-queries sprint tasks when a different cycle is selected', async () => {
    renderWithClient(<SprintPlanningPage />);

    await waitFor(() => expect(screen.getByText('Task A')).toBeInTheDocument());

    const cycleSelect = screen.getByTestId('cycle-select');

    // Select Sprint 1
    fireEvent.change(cycleSelect, { target: { value: '10' } });
    await waitFor(() => {
      expect(taskService.getByCycleId).toHaveBeenCalledWith(10);
    });

    // Select Sprint 2
    fireEvent.change(cycleSelect, { target: { value: '11' } });
    await waitFor(() => {
      expect(taskService.getByCycleId).toHaveBeenCalledWith(11);
    });
  });
});
