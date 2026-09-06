import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import RetroBoard from '../RetroBoard';
import { retroService } from '../../services/retroService';
import { Retrospective, RetroItem } from '../../types';

// Focused tests for the merge-indicator click-to-expand + unmerge interaction
// (see CHANGELOG "merge items — no way to see what was merged" fix).
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

const showSuccess = vi.fn();
const showError = vi.fn();
vi.mock('../../contexts', () => ({
  useToast: () => ({ showSuccess, showError }),
  // usePresence() (wired into RetroBoard.tsx for the S64 presence-avatar feature) imports
  // useAuth from this barrel, not from '../../contexts/AuthContext' directly — keep both
  // mocks in sync via the same authState so a merged-in presence call doesn't need its own.
  useAuth: () => ({ user: authState.user }),
}));

vi.mock('../../components/RetroSummaryPanel', () => ({
  RetroSummaryPanel: () => null,
}));

vi.mock('../../components/ActOnRetroItemsDialog', () => ({
  ActOnRetroItemsDialog: () => null,
}));

// Only the keys this interaction actually renders need real text; everything
// else falls back to the raw key (or an explicit t(key, 'default') fallback).
const translations: Record<string, string> = {
  'retroBoard.viewMergedItems': 'View merged items',
  'retroBoard.mergedItems': 'Merged items:',
  'retroBoard.unmergeItem': 'Unmerge',
  'common.anonymous': 'Anonymous',
};
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, fallback?: string) => translations[key] ?? fallback ?? key,
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

const targetItem: RetroItem = {
  id: 10,
  content: 'The merged-result item',
  columnType: 'WENT_WELL',
  retrospectiveId: 1,
  voteCount: 0,
  hasVoted: false,
  dislikeCount: 0,
  hasDisliked: false,
  createdAt: '2026-02-01T00:00:00Z',
  mergedItemIds: [11],
};

const mergedAwayItem: RetroItem = {
  id: 11,
  content: 'This is the full content of the item that got merged away and should no longer be truncated with an ellipsis in the expanded view',
  columnType: 'WENT_WELL',
  retrospectiveId: 1,
  authorName: 'Jane Doe',
  voteCount: 0,
  hasVoted: false,
  dislikeCount: 0,
  hasDisliked: false,
  createdAt: '2026-02-01T00:00:00Z',
  mergedIntoId: 10,
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

describe('RetroBoard merge visibility', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authState.user = { userId: 1, username: 'admin', role: 'ADMIN' };
    vi.mocked(retroService.getById).mockResolvedValue({ data: mockRetro } as any);
    vi.mocked(retroService.getItems).mockResolvedValue({
      data: [targetItem, mergedAwayItem],
    } as any);
  });

  it('shows a clickable merge badge that expands to list the full content of each merged-away item', async () => {
    const user = userEvent.setup();
    renderBoard();

    const badge = await screen.findByRole('button', { name: 'View merged items' });
    expect(badge).toHaveTextContent('+1');

    // Not visible until the badge is clicked (click-to-expand, not hover-only)
    expect(screen.queryByText(mergedAwayItem.content)).not.toBeInTheDocument();

    await user.click(badge);

    await waitFor(() => {
      expect(screen.getByText(mergedAwayItem.content)).toBeInTheDocument();
    });
    // Full content shown, no truncating ellipsis appended
    expect(screen.getByText(mergedAwayItem.content).textContent).not.toMatch(/\.\.\.$/);
    expect(screen.getByText('Jane Doe')).toBeInTheDocument();
  });

  it('calls retroService.unmergeItem with the merged-away item id when Unmerge is clicked', async () => {
    const user = userEvent.setup();
    vi.mocked(retroService.unmergeItem).mockResolvedValue({
      data: { ...mergedAwayItem, mergedIntoId: undefined },
    } as any);

    renderBoard();

    const badge = await screen.findByRole('button', { name: 'View merged items' });
    await user.click(badge);

    const unmergeButton = await screen.findByRole('button', { name: 'Unmerge' });
    await user.click(unmergeButton);

    await waitFor(() => {
      expect(retroService.unmergeItem).toHaveBeenCalledWith(mergedAwayItem.id);
    });
    await waitFor(() => {
      expect(showSuccess).toHaveBeenCalled();
    });
  });

  it('does not show the Unmerge action for a user who cannot manage the retro', async () => {
    authState.user = { userId: 2, username: 'dev', role: 'DEVELOPER' };
    const user = userEvent.setup();

    renderBoard();

    const badge = await screen.findByRole('button', { name: 'View merged items' });
    await user.click(badge);

    await waitFor(() => {
      expect(screen.getByText(mergedAwayItem.content)).toBeInTheDocument();
    });
    expect(screen.queryByRole('button', { name: 'Unmerge' })).not.toBeInTheDocument();
  });
});
