import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';

const showError = vi.fn();
vi.mock('../../contexts', () => ({ useToast: () => ({ showError, showSuccess: vi.fn() }) }));
vi.mock('../../services/agentActionService', () => ({
  agentActionService: { confirm: vi.fn(), cancel: vi.fn() },
}));

import { agentActionService, AgentActionProposal } from '../../services/agentActionService';
import AgentProposals from '../AgentProposals';

const proposal = (over: Partial<AgentActionProposal> = {}): AgentActionProposal => ({
  id: 1,
  commentId: 20,
  tool: 'update_task_status',
  label: 'update_task_status(status=DONE, taskId=3)',
  summary: 'Mark the task done',
  status: 'PROPOSED',
  canConfirm: true,
  createdAt: '2026-11-03T10:00:00',
  ...over,
});

const confirm = vi.mocked(agentActionService.confirm);
const cancel = vi.mocked(agentActionService.cancel);

describe('AgentProposals', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows the exact tool call the server rendered, the model summary and the status', () => {
    render(<AgentProposals proposals={[proposal()]} onUpdated={vi.fn()} />);

    expect(screen.getByText('update_task_status(status=DONE, taskId=3)')).toBeInTheDocument();
    expect(screen.getByText('Mark the task done')).toBeInTheDocument();
    expect(screen.getByText('comments.proposals.status.proposed')).toBeInTheDocument();
  });

  it('offers Apply and Dismiss only to the person who asked', () => {
    const { rerender } = render(<AgentProposals proposals={[proposal()]} onUpdated={vi.fn()} />);
    expect(screen.getByRole('button', { name: /comments\.proposals\.apply/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /comments\.proposals\.dismiss/ })).toBeInTheDocument();

    rerender(<AgentProposals proposals={[proposal({ canConfirm: false })]} onUpdated={vi.fn()} />);
    expect(screen.queryByRole('button', { name: /comments\.proposals\.apply/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /comments\.proposals\.dismiss/ })).not.toBeInTheDocument();
  });

  it('Apply confirms that proposal and reports the updated one', async () => {
    const updated = proposal({ status: 'EXECUTED', canConfirm: false, resultMessage: 'Done (id 3)' });
    confirm.mockResolvedValue({ data: updated } as never);
    const onUpdated = vi.fn();
    render(<AgentProposals proposals={[proposal()]} onUpdated={onUpdated} />);

    fireEvent.click(screen.getByRole('button', { name: /comments\.proposals\.apply/ }));

    await waitFor(() => expect(onUpdated).toHaveBeenCalledWith(updated));
    expect(confirm).toHaveBeenCalledWith(1);
    expect(cancel).not.toHaveBeenCalled();
  });

  it('Dismiss cancels that proposal', async () => {
    const updated = proposal({ status: 'CANCELLED', canConfirm: false });
    cancel.mockResolvedValue({ data: updated } as never);
    const onUpdated = vi.fn();
    render(<AgentProposals proposals={[proposal()]} onUpdated={onUpdated} />);

    fireEvent.click(screen.getByRole('button', { name: /comments\.proposals\.dismiss/ }));

    await waitFor(() => expect(onUpdated).toHaveBeenCalledWith(updated));
    expect(cancel).toHaveBeenCalledWith(1);
    expect(confirm).not.toHaveBeenCalled();
  });

  it('a failed request is reported, leaves the proposal confirmable and does not claim success', async () => {
    confirm.mockRejectedValue(new Error('boom'));
    const onUpdated = vi.fn();
    render(<AgentProposals proposals={[proposal()]} onUpdated={onUpdated} />);

    fireEvent.click(screen.getByRole('button', { name: /comments\.proposals\.apply/ }));

    await waitFor(() => expect(showError).toHaveBeenCalledWith('comments.proposals.confirmFailed'));
    expect(onUpdated).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: /comments\.proposals\.apply/ })).toBeEnabled();
  });

  it('shows the outcome of applied and failed changes, and nothing for the others', () => {
    render(
      <AgentProposals
        proposals={[
          proposal({ id: 1, status: 'EXECUTED', canConfirm: false, resultMessage: 'Done (id 3)' }),
          proposal({ id: 2, status: 'FAILED', canConfirm: false, resultMessage: "Invalid status 'DONEZO'" }),
          proposal({ id: 3, status: 'CANCELLED', canConfirm: false, resultMessage: 'ignored' }),
        ]}
        onUpdated={vi.fn()}
      />,
    );

    expect(screen.getByText(/Done \(id 3\)/)).toBeInTheDocument();
    expect(screen.getByText(/comments\.proposals\.refreshHint/)).toBeInTheDocument();
    expect(screen.getByText("Invalid status 'DONEZO'")).toBeInTheDocument();
    expect(screen.queryByText('ignored')).not.toBeInTheDocument();
  });

  it('renders nothing for an empty list', () => {
    const { container } = render(<AgentProposals proposals={[]} onUpdated={vi.fn()} />);
    expect(container).toBeEmptyDOMElement();
  });
});
