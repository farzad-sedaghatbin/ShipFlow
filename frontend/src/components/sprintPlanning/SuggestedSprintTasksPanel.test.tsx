import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SuggestedSprintTasksPanel } from './SuggestedSprintTasksPanel';
import { sprintTaskSuggestionService } from '../../services/sprintTaskSuggestionService';
import { taskService } from '../../services/taskService';

vi.mock('../../services/sprintTaskSuggestionService', () => ({
  sprintTaskSuggestionService: {
    getStatus: vi.fn(),
    generate: vi.fn(),
  },
}));

vi.mock('../../services/taskService', () => ({
  taskService: {
    bulkCreate: vi.fn(),
  },
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: any) =>
      typeof opts === 'object' && opts?.count !== undefined ? `${key} (${opts.count})` : key,
  }),
}));

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

const defaultProps = {
  cycleId: 10,
  onTasksCreated: vi.fn(),
};

describe('SuggestedSprintTasksPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders nothing when AI is not available', async () => {
    (sprintTaskSuggestionService.getStatus as any).mockResolvedValue({ available: false });
    const { container } = renderWithClient(<SuggestedSprintTasksPanel {...defaultProps} />);
    await waitFor(() => {
      expect(container).toBeEmptyDOMElement();
    });
  });

  it('shows the trigger button when AI is available', async () => {
    (sprintTaskSuggestionService.getStatus as any).mockResolvedValue({ available: true });
    renderWithClient(<SuggestedSprintTasksPanel {...defaultProps} />);
    await waitFor(() => {
      expect(screen.getByText('suggestedTasksPanel.triggerButton')).toBeInTheDocument();
    });
  });

  it('generates and displays suggestions with a Sprint badge (no Figma UI)', async () => {
    (sprintTaskSuggestionService.getStatus as any).mockResolvedValue({ available: true });
    (sprintTaskSuggestionService.generate as any).mockResolvedValue({
      suggestions: [
        {
          title: 'Wire up sprint API client',
          description: 'Backend work scoped to this sprint.',
          sourceContext: 'SPRINT',
          disciplines: ['BACKEND'],
        },
      ],
      figmaContextUsed: false,
    });

    const user = userEvent.setup();
    renderWithClient(<SuggestedSprintTasksPanel {...defaultProps} />);

    await waitFor(() => screen.getByText('suggestedTasksPanel.triggerButton'));
    await user.click(screen.getByText('suggestedTasksPanel.triggerButton'));
    await user.click(screen.getByText('suggestedTasksPanel.generateButton'));

    await waitFor(() => {
      expect(screen.getByText('Wire up sprint API client')).toBeInTheDocument();
    });
    expect(screen.getByText('suggestedSprintTasksPanel.sourceSprint')).toBeInTheDocument();
    // No Figma-related banner should ever render for sprint suggestions.
    expect(screen.queryByText('suggestedTasksPanel.figmaUnavailableNote')).not.toBeInTheDocument();
  });

  it('creates selected tasks via bulkCreate with pitchId omitted and the right cycleId', async () => {
    (sprintTaskSuggestionService.getStatus as any).mockResolvedValue({ available: true });
    (sprintTaskSuggestionService.generate as any).mockResolvedValue({
      suggestions: [
        {
          title: 'Fix flaky sprint test',
          description: 'Pure backend.',
          sourceContext: 'SPRINT',
          disciplines: ['BACKEND'],
        },
      ],
      figmaContextUsed: false,
    });
    (taskService.bulkCreate as any).mockResolvedValue({
      data: {
        successCount: 1,
        failureCount: 0,
        errors: [],
        createdTasks: [{ id: 99, title: 'Fix flaky sprint test' }],
      },
    });

    const onTasksCreated = vi.fn();
    const user = userEvent.setup();
    renderWithClient(<SuggestedSprintTasksPanel {...defaultProps} onTasksCreated={onTasksCreated} />);

    await waitFor(() => screen.getByText('suggestedTasksPanel.triggerButton'));
    await user.click(screen.getByText('suggestedTasksPanel.triggerButton'));
    await user.click(screen.getByText('suggestedTasksPanel.generateButton'));
    await waitFor(() => screen.getByText('Fix flaky sprint test'));

    await user.click(screen.getByText(/suggestedTasksPanel.createSelected/));

    await waitFor(() => {
      expect(taskService.bulkCreate).toHaveBeenCalledWith({
        cycleId: 10,
        tasks: [
          expect.objectContaining({ title: 'Fix flaky sprint test' }),
        ],
      });
      // pitchId must be entirely omitted, not passed as undefined.
      const callArg = (taskService.bulkCreate as any).mock.calls[0][0];
      expect(Object.prototype.hasOwnProperty.call(callArg, 'pitchId')).toBe(false);
      expect(onTasksCreated).toHaveBeenCalledWith([{ id: 99, title: 'Fix flaky sprint test' }]);
    });
  });
});
