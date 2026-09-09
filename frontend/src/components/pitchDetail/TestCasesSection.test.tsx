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
    getTestCasesByTask: vi.fn(),
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

  it('shows the task-specific empty state and fetches by taskId when taskId is passed', async () => {
    (qaTestManagementService.getTestCasesByTask as any).mockResolvedValue({ data: [] });
    renderWithClient(<TestCasesSection taskId={42} />);

    await waitFor(() => {
      expect(screen.getByText('testCasesSection.emptyTask')).toBeInTheDocument();
    });
    expect(qaTestManagementService.getTestCasesByTask).toHaveBeenCalledWith(42);
    expect(qaTestManagementService.getTestCasesByPitch).not.toHaveBeenCalled();
  });

  it('navigates to the pre-filled create form when "Add Test Case" is clicked for a task', async () => {
    (qaTestManagementService.getTestCasesByTask as any).mockResolvedValue({ data: [] });
    const user = userEvent.setup();
    renderWithClient(<TestCasesSection taskId={42} />);

    await waitFor(() => screen.getByText('testCasesSection.addNew'));
    await user.click(screen.getAllByText('testCasesSection.addNew')[0]);

    expect(navigateMock).toHaveBeenCalledWith('/qa/test-cases/new?taskId=42');
  });

  // Regression test: a task with more than 5 test cases used to show a dead
  // "+N more" text with nowhere useful to go — the global test-case list has
  // no task filter, so it would have shown every test case in the project.
  it('expands to show every test case in place when a task has more than 5, without navigating anywhere', async () => {
    const manyTestCases = Array.from({ length: 8 }, (_, i) => ({
      id: i + 1,
      testCaseKey: `TC-${i + 1}`,
      title: `Test case ${i + 1}`,
      type: 'FUNCTIONAL',
      priority: 'MEDIUM',
      status: 'READY',
      totalRuns: 0,
    }));
    (qaTestManagementService.getTestCasesByTask as any).mockResolvedValue({ data: manyTestCases });
    const user = userEvent.setup();
    renderWithClient(<TestCasesSection taskId={42} />);

    await waitFor(() => screen.getByText('TC-1'));
    expect(screen.queryByText('TC-6')).not.toBeInTheDocument();

    const showMoreButton = screen.getByText('testCasesSection.andMoreTask {"count":3}');
    await user.click(showMoreButton);

    expect(screen.getByText('TC-6')).toBeInTheDocument();
    expect(screen.getByText('TC-8')).toBeInTheDocument();
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
