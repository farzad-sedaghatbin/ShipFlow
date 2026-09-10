import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import BugReportModal from '../BugReportModal';

// Regression coverage for a production bug: bug reports were sometimes created twice.
// Root cause (see CHANGELOG): the PWA service worker's background-sync queue replays a POST
// that actually succeeded server-side but looked like it failed client-side, and there was no
// synchronous re-entrancy guard against a rapid double-click either. This file covers the two
// frontend-side pieces of the fix — the idempotency key and the re-entrancy guard — the
// server-side dedup itself is covered by BugReportServiceTest (backend).

vi.mock('../../contexts', () => ({
  useProject: () => ({
    currentProject: undefined,
    isScrumProject: false,
  }),
  useAuth: () => ({
    user: { id: 1, personId: 1 },
  }),
}));

vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn().mockRejectedValue(new Error('not needed in this test')),
  },
}));

vi.mock('../../services/releaseService', () => ({
  releaseService: { getByProject: vi.fn().mockResolvedValue({ data: [] }) },
}));

vi.mock('../../services/pitchService', () => ({
  pitchService: { getMyPitches: vi.fn().mockResolvedValue({ data: [] }) },
}));

vi.mock('../../services/taskService', () => ({
  taskService: { getByCycleId: vi.fn().mockResolvedValue({ data: [] }) },
}));

vi.mock('../../services/documentService', () => ({
  documentService: { uploadBugAttachment: vi.fn().mockResolvedValue(undefined) },
}));

function fillRequiredFields() {
  fireEvent.change(screen.getByLabelText(/Title/i), { target: { value: 'Something is broken' } });
  fireEvent.change(screen.getByLabelText(/Description/i), { target: { value: 'It just is.' } });
}

describe('BugReportModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('includes a fresh idempotencyKey on the create payload for a new bug report', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);

    render(<BugReportModal open onClose={vi.fn()} onSubmit={onSubmit} />);
    fillRequiredFields();

    fireEvent.click(screen.getByRole('button', { name: /Report Bug|Submit|Create/i }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    const payload = onSubmit.mock.calls[0][0];
    expect(typeof payload.idempotencyKey).toBe('string');
    expect(payload.idempotencyKey.length).toBeGreaterThan(0);
  });

  it('mints a new idempotencyKey each time the modal is reopened for a new bug', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const { rerender } = render(<BugReportModal open={false} onClose={vi.fn()} onSubmit={onSubmit} />);

    rerender(<BugReportModal open onClose={vi.fn()} onSubmit={onSubmit} />);
    fillRequiredFields();
    fireEvent.click(screen.getByRole('button', { name: /Report Bug|Submit|Create/i }));
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    const firstKey = onSubmit.mock.calls[0][0].idempotencyKey;

    // Close and reopen for a second, distinct bug report.
    rerender(<BugReportModal open={false} onClose={vi.fn()} onSubmit={onSubmit} />);
    rerender(<BugReportModal open onClose={vi.fn()} onSubmit={onSubmit} />);
    fillRequiredFields();
    fireEvent.click(screen.getByRole('button', { name: /Report Bug|Submit|Create/i }));
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(2));
    const secondKey = onSubmit.mock.calls[1][0].idempotencyKey;

    expect(secondKey).not.toEqual(firstKey);
  });

  it('does not double-submit when two clicks land before React re-renders the disabled button', async () => {
    // `disabled={loading}` alone doesn't help here — it only takes effect on the NEXT render,
    // and `fireEvent` normally flushes (via act()) between calls, which would hide exactly the
    // race this test exists to catch. Nesting both dispatches inside one outer act() call
    // defers that flush until after both clicks have already reached handleSubmit, reproducing
    // the rapid double-click/Enter-then-click scenario the synchronous `if (loading) return`
    // guard protects against.
    let resolveSubmit: () => void;
    const onSubmit = vi.fn(
      () => new Promise<undefined>((resolve) => {
        resolveSubmit = () => resolve(undefined);
      })
    );

    render(<BugReportModal open onClose={vi.fn()} onSubmit={onSubmit} />);
    fillRequiredFields();

    const button = screen.getByRole('button', { name: /Report Bug|Submit|Create/i });
    await act(async () => {
      fireEvent.click(button);
      fireEvent.click(button);
    });

    resolveSubmit!();
    await waitFor(() => expect(onSubmit).toHaveBeenCalled());
    expect(onSubmit).toHaveBeenCalledTimes(1);
  });
});
