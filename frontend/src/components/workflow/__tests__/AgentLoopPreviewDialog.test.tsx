import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';

vi.mock('../../../services/automationService', () => ({ automationService: { preview: vi.fn() } }));

import { automationService } from '../../../services/automationService';
import AgentLoopPreviewDialog from '../AgentLoopPreviewDialog';
import type { WorkflowAutomation } from '../../../types/automation';

const automation = { id: 7, name: 'Daily standup' } as WorkflowAutomation;
const preview = vi.mocked(automationService.preview);

describe('AgentLoopPreviewDialog', () => {
  beforeEach(() => vi.clearAllMocks());

  it('shows a loading state, then the title, the agent summary and the facts', async () => {
    preview.mockResolvedValue({
      data: { title: 'Standup digest — P', summary: 'Calm day.', facts: '✅ Finished (1)\n  • A — Sara', message: 'x', wouldSend: true },
    } as never);
    render(<AgentLoopPreviewDialog automation={automation} onClose={vi.fn()} />);

    expect(screen.getByRole('status')).toHaveTextContent('automation.preview.loading');
    expect(await screen.findByText('Standup digest — P')).toBeInTheDocument();
    expect(screen.getByText('Calm day.')).toBeInTheDocument();
    expect(screen.getByText(/Finished \(1\)/)).toBeInTheDocument();
    expect(screen.queryByText('automation.preview.wouldSkip')).not.toBeInTheDocument();
    expect(preview).toHaveBeenCalledWith(7);
  });

  it('says so when a real run would be skipped, and omits the summary block when there is none', async () => {
    preview.mockResolvedValue({
      data: { title: 'Cycle health — P', facts: 'No risks found.', message: 'x', wouldSend: false },
    } as never);
    render(<AgentLoopPreviewDialog automation={automation} onClose={vi.fn()} />);

    expect(await screen.findByText('automation.preview.wouldSkip')).toBeInTheDocument();
    expect(screen.queryByText('automation.preview.agentSummary')).not.toBeInTheDocument();
  });

  it('shows the server\'s reason when the preview cannot be built', async () => {
    preview.mockRejectedValue({ response: { data: { message: 'Betting prep only applies to Shape Up projects' } } });
    render(<AgentLoopPreviewDialog automation={automation} onClose={vi.fn()} />);

    expect(await screen.findByText('Betting prep only applies to Shape Up projects')).toBeInTheDocument();
  });

  it('Close calls onClose', async () => {
    preview.mockResolvedValue({ data: { title: 'T', facts: 'F', message: 'M', wouldSend: true } } as never);
    const onClose = vi.fn();
    render(<AgentLoopPreviewDialog automation={automation} onClose={onClose} />);
    await screen.findByText('T');

    fireEvent.click(screen.getAllByRole('button', { name: 'common.close' })[0]);

    await waitFor(() => expect(onClose).toHaveBeenCalled());
  });
});
