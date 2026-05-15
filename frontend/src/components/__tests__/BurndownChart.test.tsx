import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BurndownChart } from '../BurndownChart';
import { cycleService } from '../../services/cycleService';

vi.mock('../../services/cycleService', () => ({
  cycleService: {
    getBurndown: vi.fn(),
  },
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: 'en' },
  }),
}));

// Recharts renders SVG with sizes from ResponsiveContainer — stub for jsdom
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

describe('BurndownChart', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows loading skeleton while fetching', () => {
    (cycleService.getBurndown as any).mockReturnValue(new Promise(() => {}));
    const { container } = renderWithClient(
      <BurndownChart cycleId={1} cycleName="Sprint 1" />,
    );
    // Skeleton renders as a div with animate-pulse class
    expect(container.querySelector('.animate-pulse')).toBeTruthy();
  });

  it('renders empty state when API returns no data', async () => {
    (cycleService.getBurndown as any).mockResolvedValue({ data: [] });
    renderWithClient(<BurndownChart cycleId={1} cycleName="Sprint 1" />);
    await waitFor(() => {
      expect(screen.getByText('charts.burndown.noData')).toBeInTheDocument();
    });
  });

  it('renders chart when data is returned', async () => {
    (cycleService.getBurndown as any).mockResolvedValue({
      data: [
        { date: '2026-05-01', remainingPoints: 40, idealPoints: 40 },
        { date: '2026-05-02', remainingPoints: 35, idealPoints: 36 },
        { date: '2026-05-03', remainingPoints: 28, idealPoints: 32 },
      ],
    });
    renderWithClient(<BurndownChart cycleId={1} cycleName="Sprint 1" />);
    await waitFor(() => {
      expect(screen.getByTestId('responsive-container')).toBeInTheDocument();
    });
    expect(screen.getByText(/Sprint 1/)).toBeInTheDocument();
  });
});
