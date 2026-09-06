import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConflictDialog } from '../components/ConflictDialog';

describe('ConflictDialog', () => {
  it('renders the dialog with title, description, and both action buttons', () => {
    render(
      <ConflictDialog
        open
        onOpenChange={vi.fn()}
        entityLabel="pitch"
        onKeepMine={vi.fn()}
        onDiscardMine={vi.fn()}
      />
    );

    // i18next has no 'conflictDialog.*' resources loaded in the test env, so
    // t() falls back to returning the raw key itself (without interpolation)
    // — assert on those keys rather than resolved English/Farsi copy.
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    expect(screen.getByText('conflictDialog.title')).toBeInTheDocument();
    expect(screen.getByText('conflictDialog.description')).toBeInTheDocument();
    expect(screen.getByText('conflictDialog.keepMine')).toBeInTheDocument();
    expect(screen.getByText('conflictDialog.discardMine')).toBeInTheDocument();
  });

  it('calls onKeepMine when the "keep mine" button is clicked', async () => {
    const onKeepMine = vi.fn();
    const onDiscardMine = vi.fn();
    render(
      <ConflictDialog
        open
        onOpenChange={vi.fn()}
        entityLabel="wiki page"
        onKeepMine={onKeepMine}
        onDiscardMine={onDiscardMine}
      />
    );

    const buttons = screen.getAllByRole('button');
    const keepButton = buttons.find((b) => b.textContent?.includes('conflictDialog.keepMine'));
    expect(keepButton).toBeDefined();
    await userEvent.click(keepButton!);

    expect(onKeepMine).toHaveBeenCalledTimes(1);
    expect(onDiscardMine).not.toHaveBeenCalled();
  });

  it('calls onDiscardMine when the "discard mine" button is clicked', async () => {
    const onKeepMine = vi.fn();
    const onDiscardMine = vi.fn();
    render(
      <ConflictDialog
        open
        onOpenChange={vi.fn()}
        entityLabel="retro item"
        onKeepMine={onKeepMine}
        onDiscardMine={onDiscardMine}
      />
    );

    const buttons = screen.getAllByRole('button');
    const discardButton = buttons.find((b) => b.textContent?.includes('conflictDialog.discardMine'));
    expect(discardButton).toBeDefined();
    await userEvent.click(discardButton!);

    expect(onDiscardMine).toHaveBeenCalledTimes(1);
    expect(onKeepMine).not.toHaveBeenCalled();
  });

  it('does not render dialog content when closed', () => {
    render(
      <ConflictDialog
        open={false}
        onOpenChange={vi.fn()}
        entityLabel="pitch"
        onKeepMine={vi.fn()}
        onDiscardMine={vi.fn()}
      />
    );

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });
});
