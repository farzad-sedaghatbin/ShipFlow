import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { PendingAttachments } from '../PendingAttachments';

// Covers "a task can't get an attachment while it's being created": the create dialog holds the
// picked files and the backlog uploads them once the task exists.

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

const toastError = vi.fn();
vi.mock('sonner', () => ({ toast: { error: (msg: string) => toastError(msg) } }));

vi.mock('../../TaskAttachments', () => ({
  ATTACHMENT_ACCEPT: 'image/png',
  attachmentRejection: (file: File) =>
    file.size > 10 ? 'attachments.tooLarge' : file.type === 'image/png' ? null : 'attachments.unsupportedType',
  fileIcon: () => null,
  formatBytes: (n: number) => `${n} B`,
}));

function file(name: string, type: string, size: number) {
  return new File([new Uint8Array(size)], name, { type });
}

describe('PendingAttachments', () => {
  beforeEach(() => toastError.mockClear());

  it('adds valid files and reports rejected ones by name', () => {
    const onChange = vi.fn();
    render(<PendingAttachments files={[]} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText('attachments.title'), {
      target: { files: [file('shot.png', 'image/png', 4), file('big.png', 'image/png', 50), file('x.exe', 'application/x-msdownload', 4)] },
    });

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange.mock.calls[0][0].map((f: File) => f.name)).toEqual(['shot.png']);
    expect(toastError).toHaveBeenCalledWith('big.png: attachments.tooLarge');
    expect(toastError).toHaveBeenCalledWith('x.exe: attachments.unsupportedType');
  });

  it('lists picked files and removes one', () => {
    const onChange = vi.fn();
    const files = [file('a.png', 'image/png', 1), file('b.png', 'image/png', 2)];
    render(<PendingAttachments files={files} onChange={onChange} />);

    expect(screen.getByText('a.png')).toBeInTheDocument();
    expect(screen.getByText('attachments.uploadAfterCreate')).toBeInTheDocument();

    fireEvent.click(screen.getAllByLabelText('attachments.remove')[0]);
    expect(onChange.mock.calls[0][0].map((f: File) => f.name)).toEqual(['b.png']);
  });
});
