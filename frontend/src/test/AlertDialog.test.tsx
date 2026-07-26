import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ConfirmDialog } from '../components/ui/confirm-dialog';

describe('AlertDialog', () => {
  it('exposes stable overlay and content hooks for layered dialogs', () => {
    render(
      <ConfirmDialog
        open
        onOpenChange={vi.fn()}
        title="Skip Tour?"
        description="Confirm that the tour should close."
        onConfirm={vi.fn()}
      />,
    );

    expect(screen.getByRole('alertdialog')).toHaveAttribute(
      'data-radix-alert-dialog-content',
    );
    expect(
      document.querySelector('[data-radix-alert-dialog-overlay]'),
    ).toBeInTheDocument();
  });
});
