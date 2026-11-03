import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';

const showError = vi.fn();
const showSuccess = vi.fn();
vi.mock('../../../contexts', () => ({ useToast: () => ({ showError, showSuccess }) }));
vi.mock('../../../services/automationService', () => ({
  automationService: { create: vi.fn(), update: vi.fn() },
}));

import { automationService } from '../../../services/automationService';
import AutomationRuleForm from '../AutomationRuleForm';
import type { WorkflowAutomation } from '../../../types/automation';

const standup: WorkflowAutomation = {
  id: 7, name: 'Daily standup', projectId: 5, enabled: true, executionCount: 0,
  triggerType: 'SCHEDULE', triggerConfig: '{"cron":"0 0 9 * * MON-FRI","timezone":"Europe/Berlin"}',
  actionType: 'AGENT_STANDUP_DIGEST',
  actionConfig: '{"channels":["SLACK"],"lookbackHours":24,"useAi":true}',
  createdAt: '2026-11-01T00:00:00', updatedAt: '2026-11-01T00:00:00',
};

const update = vi.mocked(automationService.update);

const renderForm = (automation: WorkflowAutomation | null) =>
  render(<AutomationRuleForm open projectId={5} automation={automation} onClose={vi.fn()} onSaved={vi.fn()} />);

describe('AutomationRuleForm — schedule and agent loop sections', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    update.mockResolvedValue({ data: standup } as never);
  });

  it('editing an agent loop shows its schedule and delivery settings, not the raw JSON box', () => {
    renderForm(standup);

    expect(screen.getByTestId('schedule-fields')).toBeInTheDocument();
    expect(screen.getByTestId('agent-loop-fields')).toBeInTheDocument();
    expect(screen.queryByLabelText('automation.actionConfig')).not.toBeInTheDocument();
    expect((screen.getByLabelText('automation.schedule.timezone') as HTMLInputElement).value).toBe('Europe/Berlin');
    expect((screen.getByLabelText('automation.schedule.time') as HTMLInputElement).value).toBe('09:00');
    expect((screen.getByLabelText('automation.agentLoop.lookback') as HTMLInputElement).value).toBe('24');
    expect(screen.getByLabelText('automation.agentLoop.channel.SLACK')).toBeChecked();
    expect(screen.getByLabelText('automation.agentLoop.channel.TEAMS')).not.toBeChecked();
  });

  it('saving writes the schedule and delivery settings back as JSON', async () => {
    renderForm(standup);

    fireEvent.click(screen.getByLabelText('automation.agentLoop.channel.TEAMS'));
    fireEvent.change(screen.getByLabelText('automation.schedule.time'), { target: { value: '08:30' } });
    fireEvent.change(screen.getByLabelText('automation.agentLoop.lookback'), { target: { value: '48' } });
    fireEvent.click(screen.getByRole('button', { name: 'common.save' }));

    await waitFor(() => expect(update).toHaveBeenCalled());
    const [id, payload] = update.mock.calls[0];
    expect(id).toBe(7);
    expect(payload).toMatchObject({ projectId: 5, triggerType: 'SCHEDULE', actionType: 'AGENT_STANDUP_DIGEST', enabled: true });
    expect(JSON.parse(payload.triggerConfig!)).toEqual({ cron: '0 30 8 * * MON-FRI', timezone: 'Europe/Berlin' });
    expect(JSON.parse(payload.actionConfig!)).toEqual({ channels: ['SLACK', 'TEAMS'], lookbackHours: 48, useAi: true });
  });

  it('unticking every channel blocks saving and explains why', async () => {
    renderForm(standup);

    fireEvent.click(screen.getByLabelText('automation.agentLoop.channel.SLACK'));

    expect(screen.getByRole('alert')).toHaveTextContent('automation.agentLoop.error.channels');
    expect(screen.getByRole('button', { name: 'common.save' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: 'common.save' }));
    expect(update).not.toHaveBeenCalled();
  });

  it('a custom cron needs text before it can be saved', () => {
    renderForm({ ...standup, triggerConfig: '{"cron":"0 0 10 * * MON,WED,FRI"}' });
    expect((screen.getByLabelText('automation.schedule.cron') as HTMLInputElement).value).toBe('0 0 10 * * MON,WED,FRI');

    fireEvent.change(screen.getByLabelText('automation.schedule.cron'), { target: { value: '' } });

    expect(screen.getByRole('button', { name: 'common.save' })).toBeDisabled();
  });

  it('a server-side validation message is shown to the user', async () => {
    update.mockRejectedValue({ response: { data: { message: 'This schedule fires more than once an hour.' } } });
    renderForm(standup);

    fireEvent.click(screen.getByRole('button', { name: 'common.save' }));

    await waitFor(() => expect(showError).toHaveBeenCalledWith('This schedule fires more than once an hour.'));
  });

  it('an ordinary rule still gets the JSON config box and no agent or schedule sections', () => {
    renderForm({
      ...standup, triggerType: 'TASK_COMPLETED', triggerConfig: '{}',
      actionType: 'NOTIFY_PROJECT_MEMBERS', actionConfig: '{"message":"done"}',
    });

    expect(screen.queryByTestId('schedule-fields')).not.toBeInTheDocument();
    expect(screen.queryByTestId('agent-loop-fields')).not.toBeInTheDocument();
    expect((screen.getByLabelText('automation.actionConfig') as HTMLTextAreaElement).value).toBe('{"message":"done"}');
  });

  it('only the settings that apply to the chosen loop are shown', () => {
    const { unmount } = renderForm({ ...standup, actionType: 'AGENT_CYCLE_HEALTH_CHECK', actionConfig: '{"channels":["IN_APP"]}' });
    expect(screen.queryByLabelText('automation.agentLoop.lookback')).not.toBeInTheDocument();
    expect(screen.getByLabelText('automation.agentLoop.onlyWhenAtRisk')).toBeInTheDocument();
    unmount();

    renderForm({ ...standup, actionType: 'AGENT_BETTING_PREP', actionConfig: '{"channels":["IN_APP"]}' });
    expect(screen.queryByLabelText('automation.agentLoop.lookback')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('automation.agentLoop.onlyWhenAtRisk')).not.toBeInTheDocument();
  });
});
