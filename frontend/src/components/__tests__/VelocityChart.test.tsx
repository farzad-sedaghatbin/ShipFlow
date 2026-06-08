import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { VelocityChart } from '../VelocityChart';
import { cycleService } from '../../services/cycleService';

vi.mock('../../services/cycleService', () => ({
  cycleService: {
    getVelocity: vi.fn(),
  },
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: 'en' },
  }),
}));

vi.mock('recharts', async () => {
  const actual: any = await vi.importActual('recharts');
  return {
    ...actual,
    ResponsiveContainer: ({ children }: any) => (
      <div data-testid="responsive-container" style={{ width: 600, height: 260 }}>
        {children}
      </div>
    ),
  };
});

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

describe('VelocityChart', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows loading skeleton while fetching', () => {
    (cycleService.getVelocity as any).mockReturnValue(new Promise(() => {}));
    const { container } = renderWithClient(<VelocityChart projectId={1} />);
    expect(container.querySelector('.animate-pulse')).toBeTruthy();
  });

  it('renders empty state when no sprints have velocity data', async () => {
    (cycleService.getVelocity as any).mockResolvedValue({ data: [] });
    renderWithClient(<VelocityChart projectId={1} />);
    await waitFor(() => {
      expect(screen.getByText('charts.velocity.noData')).toBeInTheDocument();
    });
  });

  it('renders chart when velocity data is returned', async () => {
    (cycleService.getVelocity as any).mockResolvedValue({
      data: [
        { cycleId: 1, cycleName: 'Sprint 1', plannedPoints: 40, completedPoints: 35 },
        { cycleId: 2, cycleName: 'Sprint 2', plannedPoints: 38, completedPoints: 40 },
      ],
    });
    renderWithClient(<VelocityChart projectId={1} />);
    await waitFor(() => {
      expect(screen.getByTestId('responsive-container')).toBeInTheDocument();
    });
  });
});
