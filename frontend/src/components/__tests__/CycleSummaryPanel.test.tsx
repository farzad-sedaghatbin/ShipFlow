import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';

vi.mock('../../hooks/usePermission', () => ({
  PermissionGate: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));
vi.mock('../../services/narrativeService', () => ({
  narrativeService: {
    getCycleSummary: vi.fn(),
    regenerateNarrative: vi.fn(),
    regenerateNarratives: vi.fn(),
    exportAsMarkdown: vi.fn(),
  },
}));

import { narrativeService, CycleNarrative } from '../../services/narrativeService';
import CycleSummaryPanel from '../CycleSummaryPanel';

const BET_TEXT = [
  'In the betting meeting for v2.0 — Payments Overhaul, the team committed to 5 pitches with a total appetite of 53 days.',
  '',
  'The bets included:',
  '• **Instant Transfer UI** (14 days)',
  '• **Spending Analytics Dashboard** (10 days)',
  '• **Fraud Alert System** (14 days)',
].join('\n');

const narrative = (content: string): CycleNarrative => ({
  id: 1, cycleId: 7, narrativeType: 'WHAT_WE_BET', content, isAiGenerated: false,
  generatedAt: '2026-03-20T23:26:21',
});

const summary = (bet: CycleNarrative | null) => ({
  cycleId: 7, cycleName: 'v2.0 — Payments Overhaul', projectName: 'P', startDate: '2026-01-01',
  endDate: '2026-02-12', whatWeBet: bet, whatShipped: null, whatWeCut: null, surprises: null,
  fullSummary: null, generatedAt: '2026-03-20T23:26:21',
});

describe('CycleSummaryPanel — narrative text', () => {
  beforeEach(() => {
    vi.mocked(narrativeService.getCycleSummary).mockReset();
  });

  it('renders **bold** as bold instead of showing the asterisks', async () => {
    vi.mocked(narrativeService.getCycleSummary).mockResolvedValue(summary(narrative(BET_TEXT)) as never);
    const { container } = render(<CycleSummaryPanel cycleId={7} />);

    const bold = await screen.findByText('Instant Transfer UI');
    expect(bold.tagName).toBe('STRONG');
    expect(container.textContent).not.toContain('**');
    expect(screen.getByText('Spending Analytics Dashboard').tagName).toBe('STRONG');
  });

  it('keeps the one-bet-per-line layout of the template text', async () => {
    vi.mocked(narrativeService.getCycleSummary).mockResolvedValue(summary(narrative(BET_TEXT)) as never);
    const { container } = render(<CycleSummaryPanel cycleId={7} />);
    await screen.findByText('Instant Transfer UI');

    // Soft line breaks are only visible because the container preserves newlines.
    const body = container.querySelector('.markdown-content');
    expect(body?.className).toContain('whitespace-pre-line');
    expect(body?.textContent).toContain('• Instant Transfer UI (14 days)\n• Spending Analytics Dashboard');
  });

  it('plain text and list syntax from an AI narrative render too, and raw HTML is not injected', async () => {
    vi.mocked(narrativeService.getCycleSummary).mockResolvedValue(
      summary(narrative('Shipped well.\n\n- one\n- two\n\n<script>window.__pwned = 1</script>')) as never,
    );
    const { container } = render(<CycleSummaryPanel cycleId={7} />);

    await waitFor(() => expect(container.querySelectorAll('li').length).toBe(2));
    expect(container.querySelector('script')).toBeNull();
  });
});
