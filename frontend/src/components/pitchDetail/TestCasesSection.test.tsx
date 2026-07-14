import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TestCasesSection } from './TestCasesSection';
import qaTestManagementService from '../../services/qaTestManagementService';

const navigateMock = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => navigateMock,
}));

vi.mock('../../services/qaTestManagementService', () => ({
  default: {
    getTestCasesByPitch: vi.fn(),
  },
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: any) => (opts ? `${key} ${JSON.stringify(opts)}` : key),
  }),
}));

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

describe('TestCasesSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows the empty state when the pitch has no linked test cases', async () => {
    (qaTestManagementService.getTestCasesByPitch as any).mockResolvedValue({ data: [] });
    renderWithClient(<TestCasesSection pitchId={1} />);

    await waitFor(() => {
      expect(screen.getByText('testCasesSection.empty')).toBeInTheDocument();
    });
  });

  it('lists linked test cases with key, title, and status badge', async () => {
    (qaTestManagementService.getTestCasesByPitch as any).mockResolvedValue({
      data: [
        {
          id: 5,
          testCaseKey: 'TC-5',
          title: 'Login with valid credentials',
          type: 'FUNCTIONAL',
          priority: 'HIGH',
          status: 'APPROVED',
          totalRuns: 0,
        },
      ],
    });
    renderWithClient(<TestCasesSection pitchId={1} />);

    await waitFor(() => {
      expect(screen.getByText('TC-5')).toBeInTheDocument();
    });
    expect(screen.getByText('Login with valid credentials')).toBeInTheDocument();
    expect(screen.getByText('APPROVED')).toBeInTheDocument();
  });

  it('navigates to the pitch test-management page when "View All" is clicked', async () => {
    (qaTestManagementService.getTestCasesByPitch as any).mockResolvedValue({ data: [] });
    const user = userEvent.setup();
    renderWithClient(<TestCasesSection pitchId={7} />);

    await waitFor(() => screen.getByText('testCasesSection.viewAll'));
    await user.click(screen.getByText('testCasesSection.viewAll'));

    expect(navigateMock).toHaveBeenCalledWith('/pitches/7/test');
  });
});
