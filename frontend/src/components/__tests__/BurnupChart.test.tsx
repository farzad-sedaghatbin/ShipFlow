import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BurnupChart } from '../BurnupChart';
import { cycleService } from '../../services/cycleService';

vi.mock('../../services/cycleService', () => ({
  cycleService: {
    getBurnup: vi.fn(),
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

describe('BurnupChart', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows loading skeleton while fetching', () => {
    (cycleService.getBurnup as any).mockReturnValue(new Promise(() => {}));
    const { container } = renderWithClient(<BurnupChart cycleId={1} cycleName="Sprint 1" />);
    expect(container.querySelector('.animate-pulse')).toBeTruthy();
  });

  it('renders empty state when no burnup data is returned', async () => {
    (cycleService.getBurnup as any).mockResolvedValue({ data: [] });
    renderWithClient(<BurnupChart cycleId={1} cycleName="Sprint 1" />);
    await waitFor(() => {
      expect(screen.getByText('scrumReports.burnup.noData')).toBeInTheDocument();
    });
  });

  it('renders chart when burnup data is returned', async () => {
    (cycleService.getBurnup as any).mockResolvedValue({
      data: [
        { date: '2026-09-01', completedPoints: 0, totalScopePoints: 40 },
        { date: '2026-09-02', completedPoints: 10, totalScopePoints: 40 },
      ],
    });
    renderWithClient(<BurnupChart cycleId={1} cycleName="Sprint 1" />);
    await waitFor(() => {
      expect(screen.getByTestId('responsive-container')).toBeInTheDocument();
    });
  });
});
