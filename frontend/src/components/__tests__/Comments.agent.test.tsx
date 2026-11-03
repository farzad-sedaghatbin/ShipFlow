import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';

vi.mock('../mentions/mentionUtils', () => ({
  formatMentionToken: (n: string) => `@${n}`,
  splitMentionsInText: (t: string) => t,
}));
vi.mock('../../contexts', () => ({ useToast: () => ({ showError: vi.fn(), showSuccess: vi.fn() }) }));
vi.mock('../../services/agentActionService', () => ({ agentActionService: { confirm: vi.fn(), cancel: vi.fn() } }));
vi.mock('../../services/commentService', () => ({
  default: {
    getComments: vi.fn(),
    createComment: vi.fn(),
    searchUsersForMention: vi.fn().mockResolvedValue({ data: [] }),
    getAvailableReactions: vi.fn().mockResolvedValue({ data: [] }),
  },
  REACTION_EMOJIS: {},
}));

import commentService from '../../services/commentService';
import Comments from '../Comments';

const reactions = { reactionCounts: {}, userReactions: {}, totalReactions: 0 };
const human = (id: number, content: string) => ({
  id, content, entityType: 'TASK', entityId: 1, authorId: 1, authorName: 'Sara',
  authorUsername: 'sara', isAgent: false, isEdited: false, createdAt: '2026-11-02T10:00:00',
  reactions, canEdit: true, canDelete: true,
});
const agent = (id: number, content: string) => ({
  ...human(id, content), authorId: 99, authorName: 'ShipFlow', authorUsername: 'shipflow-agent',
  isAgent: true, canEdit: false, canDelete: false,
});

const getComments = vi.mocked(commentService.getComments);
const createComment = vi.mocked(commentService.createComment);

async function post(text: string) {
  const box = screen.getByPlaceholderText(/comments\.placeholder/);
  fireEvent.change(box, { target: { value: text } });
  await act(async () => {
    fireEvent.submit(box.closest('form')!);
  });
}

describe('Comments — ShipFlow Agent', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    getComments.mockReset();
    createComment.mockReset();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('marks an agent-authored comment with the AI badge and a human one without', async () => {
    getComments.mockResolvedValue({ data: [human(1, 'hi'), agent(2, 'Blocked on the DB migration.')] } as never);
    await act(async () => {
      render(<Comments entityType="task" entityId={1} />);
    });

    expect(screen.getAllByText('comments.agentBadge')).toHaveLength(1);
    expect(screen.getByText('Blocked on the DB migration.')).toBeInTheDocument();
  });

  it('shows a thinking indicator after an @ShipFlow comment, then the reply replaces it', async () => {
    getComments.mockResolvedValueOnce({ data: [] } as never);
    createComment.mockResolvedValue({ data: human(1, '@ShipFlow what is blocking this?') } as never);
    await act(async () => {
      render(<Comments entityType="task" entityId={1} />);
    });

    await post('@ShipFlow what is blocking this?');
    expect(screen.getByTestId('agent-thinking')).toBeInTheDocument();

    getComments.mockResolvedValue({
      data: [human(1, '@ShipFlow what is blocking this?'), agent(2, 'Waiting on review.')],
    } as never);
    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });

    expect(screen.queryByTestId('agent-thinking')).not.toBeInTheDocument();
    expect(screen.getByText('Waiting on review.')).toBeInTheDocument();
  });

  it('does not wait for the agent on an ordinary comment or an email address', async () => {
    getComments.mockResolvedValueOnce({ data: [] } as never);
    createComment.mockResolvedValue({ data: human(1, 'mail bob@shipflow.dev') } as never);
    await act(async () => {
      render(<Comments entityType="task" entityId={1} />);
    });

    await post('mail bob@shipflow.dev');

    expect(screen.queryByTestId('agent-thinking')).not.toBeInTheDocument();
  });

  it('gives up waiting after the poll limit instead of spinning forever', async () => {
    getComments.mockResolvedValueOnce({ data: [] } as never);
    createComment.mockResolvedValue({ data: human(1, '@ShipFlow hello') } as never);
    await act(async () => {
      render(<Comments entityType="task" entityId={1} />);
    });
    await post('@ShipFlow hello');
    getComments.mockResolvedValue({ data: [human(1, '@ShipFlow hello')] } as never);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000 * 30);
    });

    expect(screen.queryByTestId('agent-thinking')).not.toBeInTheDocument();
  });
  it('renders the agent\'s proposed changes under its comment and nowhere else', async () => {
    const proposal = {
      id: 9, commentId: 2, tool: 'update_task_status', label: 'update_task_status(status=DONE, taskId=1)',
      status: 'PROPOSED', canConfirm: true, createdAt: '2026-11-03T10:00:00',
    };
    getComments.mockResolvedValue({
      data: [human(1, '@ShipFlow mark it done'), { ...agent(2, 'I can do that.'), proposals: [proposal] }],
    } as never);
    await act(async () => {
      render(<Comments entityType="task" entityId={1} />);
    });

    expect(screen.getAllByTestId('agent-proposals')).toHaveLength(1);
    expect(screen.getByText('update_task_status(status=DONE, taskId=1)')).toBeInTheDocument();
  });
});
