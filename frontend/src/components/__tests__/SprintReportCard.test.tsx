import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SprintReportCard } from '../SprintReportCard';
import { cycleService } from '../../services/cycleService';

vi.mock('../../services/cycleService', () => ({
  cycleService: {
    getSprintReport: vi.fn(),
  },
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: any) =>
      typeof opts === 'object' ? `${key} ${JSON.stringify(opts)}` : key,
  }),
}));

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

describe('SprintReportCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows loading skeleton while fetching', () => {
    (cycleService.getSprintReport as any).mockReturnValue(new Promise(() => {}));
    const { container } = renderWithClient(<SprintReportCard cycleId={1} cycleName="Sprint 1" />);
    expect(container.querySelector('.animate-pulse')).toBeTruthy();
  });

  // Regression test: completionRate from the backend is a 0-1 fraction
  // (completedPoints / plannedPoints), not a percentage — the component must
  // multiply by 100 before rendering, or a 50% sprint would show as "1%".
  it('renders completion rate as a percentage, not the raw 0-1 fraction', async () => {
    (cycleService.getSprintReport as any).mockResolvedValue({
      data: {
        cycleId: 1,
        cycleName: 'Sprint 1',
        startDate: '2026-01-01',
        endDate: '2026-01-14',
        plannedPoints: 40,
        completedPoints: 20,
        completionRate: 0.5,
        taskCountByStatus: {
          BACKLOG: 0,
          TODO: 1,
          IN_PROGRESS: 2,
          BLOCKED: 0,
          IN_REVIEW: 0,
          DONE: 3,
          CANCELLED: 0,
        },
        scopeAddedTaskCount: 0,
        scopeAddedPoints: 0,
      },
    });

    renderWithClient(<SprintReportCard cycleId={1} cycleName="Sprint 1" />);

    await waitFor(() => {
      expect(screen.getByText('50%')).toBeInTheDocument();
    });
    expect(screen.queryByText('1%')).not.toBeInTheDocument();
    expect(screen.queryByText('0%')).not.toBeInTheDocument();
  });

  it('shows the scope-added callout only when tasks were added mid-sprint', async () => {
    (cycleService.getSprintReport as any).mockResolvedValue({
      data: {
        cycleId: 1,
        cycleName: 'Sprint 1',
        startDate: '2026-01-01',
        endDate: '2026-01-14',
        plannedPoints: 10,
        completedPoints: 0,
        completionRate: 0,
        taskCountByStatus: {
          BACKLOG: 1,
          TODO: 0,
          IN_PROGRESS: 0,
          BLOCKED: 0,
          IN_REVIEW: 0,
          DONE: 0,
          CANCELLED: 0,
        },
        scopeAddedTaskCount: 2,
        scopeAddedPoints: 5,
      },
    });

    renderWithClient(<SprintReportCard cycleId={1} cycleName="Sprint 1" />);

    await waitFor(() => {
      expect(screen.getByText(/scrumReports.sprintReport.scopeAdded/)).toBeInTheDocument();
    });
  });
});
