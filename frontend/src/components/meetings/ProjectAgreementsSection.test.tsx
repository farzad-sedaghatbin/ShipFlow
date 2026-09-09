import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ProjectAgreementsSection from './ProjectAgreementsSection';
import { projectAgreementService } from '../../services/projectAgreementService';
import { meetingService } from '../../services/meetingService';
import { ProjectAgreement } from '../../types';

vi.mock('../../services/projectAgreementService', () => ({
  projectAgreementService: {
    list: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
  },
}));

vi.mock('../../services/meetingService', () => ({
  meetingService: {
    getWithFilters: vi.fn(),
  },
}));

const showSuccess = vi.fn();
const showError = vi.fn();
vi.mock('../../contexts', () => ({
  useToast: () => ({ showSuccess, showError }),
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: any) => {
      if (opts && typeof opts === 'object') {
        return Object.entries(opts).reduce(
          (acc, [k, v]) => acc.replace(`{{${k}}}`, String(v)),
          key
        );
      }
      return key;
    },
    i18n: { language: 'en' },
  }),
}));

function renderWithClient(ui: React.ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

const mockAgreements: ProjectAgreement[] = [
  {
    id: 1,
    projectId: 10,
    title: 'Payment terms',
    content: 'Net 30 for all invoices.',
    agreedDate: '2026-08-01',
    createdByName: 'Alice',
    createdAt: '2026-08-01T00:00:00Z',
  },
];

describe('ProjectAgreementsSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (meetingService.getWithFilters as any).mockResolvedValue({ data: { content: [] } });
  });

  it('renders the empty state when there are no agreements', async () => {
    (projectAgreementService.list as any).mockResolvedValue({ data: [] });

    renderWithClient(<ProjectAgreementsSection projectId={10} />);

    await waitFor(() => {
      expect(screen.getByText('projectAgreements.empty.title')).toBeInTheDocument();
    });
  });

  it('renders the list of agreements', async () => {
    (projectAgreementService.list as any).mockResolvedValue({ data: mockAgreements });

    renderWithClient(<ProjectAgreementsSection projectId={10} />);

    await waitFor(() => {
      expect(screen.getByText('Payment terms')).toBeInTheDocument();
      expect(screen.getByText('Net 30 for all invoices.')).toBeInTheDocument();
    });
  });

  it('creates a new agreement via the add dialog', async () => {
    (projectAgreementService.list as any)
      .mockResolvedValueOnce({ data: [] })
      .mockResolvedValueOnce({ data: [...mockAgreements] });
    (projectAgreementService.create as any).mockResolvedValue({ data: mockAgreements[0] });

    const user = userEvent.setup();
    renderWithClient(<ProjectAgreementsSection projectId={10} />);

    await waitFor(() => {
      expect(screen.getByText('projectAgreements.empty.title')).toBeInTheDocument();
    });

    await user.click(screen.getAllByText('projectAgreements.addAgreement')[0]);

    await waitFor(() => {
      expect(screen.getByText('projectAgreements.dialog.newTitle')).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText(/projectAgreements.dialog.titleLabel/), 'Payment terms');
    await user.type(
      screen.getByLabelText(/projectAgreements.dialog.contentLabel/),
      'Net 30 for all invoices.'
    );

    await user.click(screen.getByText('projectAgreements.dialog.create'));

    await waitFor(() => {
      expect(projectAgreementService.create).toHaveBeenCalledWith(
        10,
        expect.objectContaining({ title: 'Payment terms', content: 'Net 30 for all invoices.' })
      );
      expect(showSuccess).toHaveBeenCalled();
    });
  });

  it('deletes an agreement after confirmation', async () => {
    (projectAgreementService.list as any)
      .mockResolvedValueOnce({ data: mockAgreements })
      .mockResolvedValueOnce({ data: [] });
    (projectAgreementService.remove as any).mockResolvedValue({});

    const user = userEvent.setup();
    renderWithClient(<ProjectAgreementsSection projectId={10} />);

    await waitFor(() => {
      expect(screen.getByText('Payment terms')).toBeInTheDocument();
    });

    await user.click(screen.getByLabelText('projectAgreements.deleteTooltip'));

    await waitFor(() => {
      expect(screen.getByText('projectAgreements.deleteConfirm.title')).toBeInTheDocument();
    });

    await user.click(screen.getByText('projectAgreements.deleteConfirm.confirm'));

    await waitFor(() => {
      expect(projectAgreementService.remove).toHaveBeenCalledWith(10, 1);
      expect(showSuccess).toHaveBeenCalled();
    });
  });
});
