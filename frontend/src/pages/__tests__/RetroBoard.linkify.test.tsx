import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import RetroBoard from '../RetroBoard';
import { retroService } from '../../services/retroService';
import { Retrospective, RetroItem } from '../../types';

// Covers the "pasted link should render as a clickable link" fix — retro item
// text is now rendered through the same markdown+remark-gfm auto-linkify
// pipeline already used by Comments, instead of a raw <p>{content}</p>.
// This file does not attempt to cover the rest of RetroBoard.tsx.

vi.mock('../../services/retroService', () => ({
  retroService: {
    getById: vi.fn(),
    getItems: vi.fn(),
    createItem: vi.fn(),
    updateItem: vi.fn(),
    deleteItem: vi.fn(),
    toggleVote: vi.fn(),
    toggleDislike: vi.fn(),
    markDiscussed: vi.fn(),
    mergeItems: vi.fn(),
    unmergeItem: vi.fn(),
    open: vi.fn(),
    close: vi.fn(),
  },
}));

const authState = vi.hoisted(() => ({
  user: { userId: 1, username: 'admin', role: 'ADMIN' as string },
}));
vi.mock('../../contexts/AuthContext', () => ({
  useAuth: () => ({ user: authState.user }),
}));

vi.mock('../../contexts', () => ({
  useToast: () => ({ showSuccess: vi.fn(), showError: vi.fn() }),
  useAuth: () => ({ user: authState.user }),
}));

vi.mock('../../components/RetroSummaryPanel', () => ({
  RetroSummaryPanel: () => null,
}));

vi.mock('../../components/ActOnRetroItemsDialog', () => ({
  ActOnRetroItemsDialog: () => null,
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, fallback?: string) => fallback ?? key,
    i18n: { language: 'en' },
  }),
}));

const mockRetro: Retrospective = {
  id: 1,
  title: 'Sprint 1 Retro',
  status: 'OPEN',
  cycleId: 1,
  cycleName: 'Cycle 1',
  projectId: 1,
  createdAt: '2026-02-01T00:00:00Z',
};

const itemWithLink: RetroItem = {
  id: 20,
  content: 'See the plan at https://wiki.example.com/docs/retro-plan for context',
  columnType: 'WENT_WELL',
  retrospectiveId: 1,
  voteCount: 0,
  hasVoted: false,
  dislikeCount: 0,
  hasDisliked: false,
  createdAt: '2026-02-01T00:00:00Z',
};

function renderBoard() {
  return render(
    <MemoryRouter initialEntries={['/retros/1']}>
      <Routes>
        <Route path="/retros/:id" element={<RetroBoard />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('RetroBoard item link rendering', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authState.user = { userId: 1, username: 'admin', role: 'ADMIN' };
    vi.mocked(retroService.getById).mockResolvedValue({ data: mockRetro } as any);
    vi.mocked(retroService.getItems).mockResolvedValue({ data: [itemWithLink] } as any);
  });

  it('renders a pasted URL inside retro item text as a clickable link', async () => {
    renderBoard();

    const link = await screen.findByRole('link', { name: 'https://wiki.example.com/docs/retro-plan' });
    expect(link).toHaveAttribute('href', 'https://wiki.example.com/docs/retro-plan');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    expect(screen.getByText(/See the plan at/)).toBeInTheDocument();
  });
});
