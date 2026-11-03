import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';

const showError = vi.fn();
const showSuccess = vi.fn();
vi.mock('../../contexts', () => ({ useToast: () => ({ showError, showSuccess }) }));
vi.mock('../../services/documentService', () => ({
  documentService: {
    getPlanningAttachments: vi.fn(),
    uploadPlanningAttachment: vi.fn(),
    deletePlanningAttachment: vi.fn(),
    fetchAttachmentBlobUrl: vi.fn().mockResolvedValue('blob:thumb'),
    downloadAttachment: vi.fn(),
  },
}));

import { documentService } from '../../services/documentService';
import PlanningAttachments, { planningAttachmentRejection, PLANNING_MAX_BYTES } from '../PlanningAttachments';

const doc = (id: number, name: string, type = 'png') => ({
  id, fileName: name, originalFileName: name, fileType: type, textExtracted: false,
  createdAt: '2026-11-03T10:00:00', indexedForQA: false,
});
const file = (name: string, size = 10, type = 'image/png') => new File([new Uint8Array(size)], name, { type });

const get = vi.mocked(documentService.getPlanningAttachments);
const upload = vi.mocked(documentService.uploadPlanningAttachment);
const del = vi.mocked(documentService.deletePlanningAttachment);

beforeEach(() => {
  vi.clearAllMocks();
  // jsdom has no object-URL support; browsers do.
  URL.createObjectURL = vi.fn(() => 'blob:x');
  URL.revokeObjectURL = vi.fn();
  vi.mocked(documentService.fetchAttachmentBlobUrl).mockResolvedValue('blob:thumb');
});

describe('planningAttachmentRejection', () => {
  it('accepts images and clips, rejects other types and oversize files', () => {
    expect(planningAttachmentRejection(file('a.PNG'))).toBeNull();
    expect(planningAttachmentRejection(file('a.mp4'))).toBeNull();
    expect(planningAttachmentRejection(file('notes.pdf'))).toBe('planningAttachments.unsupportedType');
    expect(planningAttachmentRejection(file('a.png', PLANNING_MAX_BYTES + 1))).toBe('planningAttachments.tooLarge');
  });
});

describe('PlanningAttachments', () => {
  it('lists existing attachments for the right entity with an image thumbnail', async () => {
    get.mockResolvedValue({ data: [doc(1, 'mockup.png')] } as never);

    render(<PlanningAttachments entityType="epic" entityId={7} />);

    expect(await screen.findByAltText('mockup.png')).toHaveAttribute('src', 'blob:thumb');
    expect(get).toHaveBeenCalledWith('epic', 7);
  });

  it('shows the empty state when there is nothing attached', async () => {
    get.mockResolvedValue({ data: [] } as never);
    render(<PlanningAttachments entityType="initiative" entityId={3} />);
    expect(await screen.findByText('planningAttachments.empty')).toBeInTheDocument();
  });

  it('uploads a picked image, reloads the list and confirms', async () => {
    get.mockResolvedValueOnce({ data: [] } as never).mockResolvedValue({ data: [doc(5, 'shot.png')] } as never);
    upload.mockResolvedValue({ data: {} } as never);
    render(<PlanningAttachments entityType="epic" entityId={7} />);
    await screen.findByText('planningAttachments.empty');

    const f = file('shot.png');
    fireEvent.change(screen.getByTestId('planning-attachments-input'), { target: { files: [f] } });

    await waitFor(() => expect(upload).toHaveBeenCalledWith('epic', 7, f));
    expect(await screen.findByAltText('shot.png')).toBeInTheDocument();
    expect(showSuccess).toHaveBeenCalled();
  });

  it('rejects an unsupported file on the client without calling the server', async () => {
    get.mockResolvedValue({ data: [] } as never);
    render(<PlanningAttachments entityType="epic" entityId={7} />);
    await screen.findByText('planningAttachments.empty');

    fireEvent.change(screen.getByTestId('planning-attachments-input'), { target: { files: [file('x.exe', 5, 'application/x-msdownload')] } });

    await waitFor(() => expect(showError).toHaveBeenCalled());
    expect(upload).not.toHaveBeenCalled();
  });

  it('removes an attachment', async () => {
    get.mockResolvedValue({ data: [doc(1, 'mockup.png')] } as never);
    del.mockResolvedValue({ data: { message: 'ok' } } as never);
    render(<PlanningAttachments entityType="epic" entityId={7} />);
    await screen.findByAltText('mockup.png');

    fireEvent.click(screen.getByRole('button', { name: 'planningAttachments.remove' }));

    await waitFor(() => expect(del).toHaveBeenCalledWith('epic', 7, 1));
    await waitFor(() => expect(screen.queryByAltText('mockup.png')).not.toBeInTheDocument());
  });
});
