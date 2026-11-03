import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';

const showError = vi.fn();
const showSuccess = vi.fn();
let role = 'MANAGER';
vi.mock('../../contexts', () => ({
  useProject: () => ({ currentProject: { id: 5, name: 'Payments' } }),
  useToast: () => ({ showError, showSuccess }),
}));
vi.mock('../../hooks/usePermission', () => ({ usePermission: () => ({ user: { role } }) }));
vi.mock('../../services/automationService', () => ({
  automationService: {
    getByProject: vi.fn(), getTemplates: vi.fn(), getProjectExecutionLog: vi.fn(),
    toggle: vi.fn(), delete: vi.fn(), preview: vi.fn(), runNow: vi.fn(),
  },
}));

import { automationService } from '../../services/automationService';
import AutomationsPage from '../AutomationsPage';
import type { WorkflowAutomation } from '../../types/automation';

const base = {
  projectId: 5, enabled: true, executionCount: 0, createdAt: '2026-11-01T00:00:00', updatedAt: '2026-11-01T00:00:00',
};
const standup: WorkflowAutomation = {
  ...base, id: 1, name: 'Daily standup', triggerType: 'SCHEDULE', actionType: 'AGENT_STANDUP_DIGEST',
  triggerConfig: '{"cron":"0 0 9 * * MON-FRI"}', actionConfig: '{}', nextRunAt: '2026-11-09T09:00:00Z',
};
const ordinary: WorkflowAutomation = {
  ...base, id: 2, name: 'Notify on done', triggerType: 'TASK_COMPLETED', actionType: 'NOTIFY_PROJECT_MEMBERS',
};

const svc = vi.mocked(automationService);

function load(rules: WorkflowAutomation[]) {
  svc.getByProject.mockResolvedValue({ data: rules } as never);
  svc.getTemplates.mockResolvedValue({ data: [] } as never);
  svc.getProjectExecutionLog.mockResolvedValue({ data: [] } as never);
}

describe('AutomationsPage — agent loops', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    role = 'MANAGER';
  });

  it('shows when a scheduled rule next runs, and not for event rules or disabled rules', async () => {
    load([standup, ordinary, { ...standup, id: 3, name: 'Paused', enabled: false }]);
    render(<AutomationsPage />);

    expect(await screen.findByTestId('next-run-1')).toHaveTextContent('automation.nextRun');
    expect(screen.queryByTestId('next-run-2')).not.toBeInTheDocument();
    expect(screen.queryByTestId('next-run-3')).not.toBeInTheDocument();
  });

  it('managers get Preview and Run now on agent loops only', async () => {
    load([standup, ordinary]);
    render(<AutomationsPage />);
    await screen.findByText('Daily standup');

    expect(screen.getAllByRole('button', { name: 'automation.preview.button' })).toHaveLength(1);
    expect(screen.getAllByRole('button', { name: 'automation.runNow.button' })).toHaveLength(1);
  });

  it('other roles get neither', async () => {
    role = 'MEMBER';
    load([standup]);
    render(<AutomationsPage />);
    await screen.findByText('Daily standup');

    expect(screen.queryByRole('button', { name: 'automation.preview.button' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'automation.runNow.button' })).not.toBeInTheDocument();
  });

  it('Run now reports what the run actually did, in the loop\'s own words', async () => {
    load([standup]);
    svc.runNow.mockResolvedValue({ data: { status: 'SUCCESS', resultMessage: 'Sent to Slack (with an AI summary)' } } as never);
    render(<AutomationsPage />);
    await screen.findByText('Daily standup');

    fireEvent.click(screen.getByRole('button', { name: 'automation.runNow.button' }));

    await waitFor(() => expect(showSuccess).toHaveBeenCalledWith('Sent to Slack (with an AI summary)'));
    expect(svc.runNow).toHaveBeenCalledWith(1);
    expect(showError).not.toHaveBeenCalled();
  });

  it('a run that delivered nothing is an error, not a success', async () => {
    load([standup]);
    svc.runNow.mockResolvedValue({
      data: { status: 'FAILURE', resultMessage: 'Action failed: Nothing was sent; skipped: Slack (not configured)' },
    } as never);
    render(<AutomationsPage />);
    await screen.findByText('Daily standup');

    fireEvent.click(screen.getByRole('button', { name: 'automation.runNow.button' }));

    await waitFor(() => expect(showError).toHaveBeenCalledWith(expect.stringContaining('Slack (not configured)')));
    expect(showSuccess).not.toHaveBeenCalled();
  });

  it('Preview opens the preview for that rule', async () => {
    load([standup]);
    svc.preview.mockResolvedValue({ data: { title: 'Standup digest — Payments', facts: 'F', message: 'M', wouldSend: true } } as never);
    render(<AutomationsPage />);
    await screen.findByText('Daily standup');

    fireEvent.click(screen.getByRole('button', { name: 'automation.preview.button' }));

    expect(await screen.findByTestId('agent-loop-preview')).toBeInTheDocument();
    expect(svc.preview).toHaveBeenCalledWith(1);
  });
});
