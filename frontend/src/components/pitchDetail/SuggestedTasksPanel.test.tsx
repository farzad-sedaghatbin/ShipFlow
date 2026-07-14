import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SuggestedTasksPanel } from './SuggestedTasksPanel';
import { pitchTaskSuggestionService } from '../../services/pitchTaskSuggestionService';
import { taskService } from '../../services/taskService';

vi.mock('../../services/pitchTaskSuggestionService', () => ({
  pitchTaskSuggestionService: {
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
  pitchId: 1,
  cycleId: 10,
  onTasksCreated: vi.fn(),
};

describe('SuggestedTasksPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders nothing when AI is not available', async () => {
    (pitchTaskSuggestionService.getStatus as any).mockResolvedValue({ available: false });
    const { container } = renderWithClient(<SuggestedTasksPanel {...defaultProps} />);
    await waitFor(() => {
      expect(container).toBeEmptyDOMElement();
    });
  });

  it('shows the trigger button when AI is available', async () => {
    (pitchTaskSuggestionService.getStatus as any).mockResolvedValue({ available: true });
    renderWithClient(<SuggestedTasksPanel {...defaultProps} />);
    await waitFor(() => {
      expect(screen.getByText('suggestedTasksPanel.triggerButton')).toBeInTheDocument();
    });
  });

  it('generates and displays suggestions with discipline badges', async () => {
    (pitchTaskSuggestionService.getStatus as any).mockResolvedValue({ available: true });
    (pitchTaskSuggestionService.generate as any).mockResolvedValue({
      suggestions: [
        {
          title: 'Build settings API',
          description: 'Backend and mobile collaborate on this.',
          sourceContext: 'PITCH',
          disciplines: ['BACKEND', 'MOBILE'],
        },
      ],
      figmaContextUsed: false,
    });

    const user = userEvent.setup();
    renderWithClient(<SuggestedTasksPanel {...defaultProps} />);

    await waitFor(() => screen.getByText('suggestedTasksPanel.triggerButton'));
    await user.click(screen.getByText('suggestedTasksPanel.triggerButton'));
    await user.click(screen.getByText('suggestedTasksPanel.generateButton'));

    await waitFor(() => {
      expect(screen.getByText('Build settings API')).toBeInTheDocument();
    });
    expect(screen.getByText('suggestedTasksPanel.figmaUnavailableNote')).toBeInTheDocument();
  });

  it('creates selected tasks via bulkCreate and notifies parent', async () => {
    (pitchTaskSuggestionService.getStatus as any).mockResolvedValue({ available: true });
    (pitchTaskSuggestionService.generate as any).mockResolvedValue({
      suggestions: [
        {
          title: 'Write migration script',
          description: 'Pure backend.',
          sourceContext: 'PITCH',
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
        createdTasks: [{ id: 99, title: 'Write migration script' }],
      },
    });

    const onTasksCreated = vi.fn();
    const user = userEvent.setup();
    renderWithClient(<SuggestedTasksPanel {...defaultProps} onTasksCreated={onTasksCreated} />);

    await waitFor(() => screen.getByText('suggestedTasksPanel.triggerButton'));
    await user.click(screen.getByText('suggestedTasksPanel.triggerButton'));
    await user.click(screen.getByText('suggestedTasksPanel.generateButton'));
    await waitFor(() => screen.getByText('Write migration script'));

    await user.click(screen.getByText(/suggestedTasksPanel.createSelected/));

    await waitFor(() => {
      expect(taskService.bulkCreate).toHaveBeenCalledWith({
        pitchId: 1,
        cycleId: 10,
        tasks: [
          expect.objectContaining({ title: 'Write migration script' }),
        ],
      });
      expect(onTasksCreated).toHaveBeenCalledWith([{ id: 99, title: 'Write migration script' }]);
    });
  });
});
