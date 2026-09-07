import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { LicenseSettingsTab } from '../components/organizationSettings/LicenseSettingsTab';

// ── Service mock ──────────────────────────────────────────────────────────────

vi.mock('../services/licenseService', () => ({
  licenseService: {
    getStatus: vi.fn(),
    uploadLicense: vi.fn(),
    removeLicense: vi.fn(),
  },
}));

// ── Context mocks ─────────────────────────────────────────────────────────────
// useAuth is re-mocked per test to flip the user's role; useToast captures toasts.

const mockShowToast = vi.fn();
let mockUser: { role: string } | null = { role: 'ADMIN' };

vi.mock('../contexts', () => ({
  useAuth: () => ({ user: mockUser }),
  useToast: () => ({ showToast: mockShowToast }),
}));

// ── i18n keys ─────────────────────────────────────────────────────────────────
// The setup.ts already initializes i18n with initReactI18next. We add the
// license keys the component actually renders so assertions read real text.

import i18n from 'i18next';

const licenseKeys: Record<string, string> = {
  'license.tabLabel': 'Licence',
  'license.title': 'Licence',
  'license.subtitle': 'View your current licence.',
  'license.statusValid': 'Licensed',
  'license.statusGrace': 'Grace Period',
  'license.statusExpired': 'Expired',
  'license.statusCommunity': 'Community Edition',
  'license.licensee': 'Licensed to',
  'license.seatsOf': '{{used}} of {{total}} seats used',
  'license.issuedAt': 'Issued',
  'license.expiresAt': 'Expires',
  'license.supportUntil': 'Support until',
  'license.features': 'Features',
  'license.noFeatures': 'No premium features unlocked',
  'license.communityText': 'Community Edition — up to {{userCap}} users and {{automationCap}} automations.',
  'license.graceWarning': 'Grace period until {{graceEndsAt}}.',
  'license.uploadLabel': 'Install a licence',
  'license.uploadFile': 'Upload a licence file',
  'license.pasteLabel': 'Or paste the licence content',
  'license.uploadButton': 'Upload Licence',
  'license.uploading': 'Uploading…',
  'license.uploadSuccess': 'Licence uploaded successfully',
  'license.invalidError': 'This licence file is invalid.',
  'license.removeButton': 'Remove Licence',
  'license.removeConfirmTitle': 'Remove Licence',
  'license.removeConfirm': 'This will remove the installed licence.',
  'license.removeSuccess': 'Licence removed',
  'common.confirm': 'Confirm',
  'common.cancel': 'Cancel',
};

Object.entries(licenseKeys).forEach(([key, value]) => {
  i18n.addResource('en', 'translation', key, value);
});

// ── Helpers ───────────────────────────────────────────────────────────────────

import { licenseService } from '../services/licenseService';

const makeQueryClient = () =>
  new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

function renderTab() {
  const qc = makeQueryClient();
  return render(
    <QueryClientProvider client={qc}>
      <LicenseSettingsTab />
    </QueryClientProvider>
  );
}

const validStatus = {
  status: 'VALID' as const,
  edition: 'COMMERCIAL',
  licensee: 'Acme Corp',
  seats: 25,
  seatsUsed: 12,
  features: ['sso', 'audit-export'],
  issuedAt: '2026-01-01',
  expiresAt: '2027-01-01',
  supportUntil: '2027-01-01',
  graceEndsAt: null,
  communityCap: 10,
  automationCap: 5,
  automationsUsed: 2,
};

const missingStatus = {
  status: 'MISSING' as const,
  edition: null,
  licensee: null,
  seats: null,
  seatsUsed: 4,
  features: [],
  issuedAt: null,
  expiresAt: null,
  supportUntil: null,
  graceEndsAt: null,
  communityCap: 10,
  automationCap: 5,
  automationsUsed: 1,
};

const graceStatus = {
  ...validStatus,
  status: 'GRACE' as const,
  graceEndsAt: '2027-02-01',
};

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('LicenseSettingsTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = { role: 'ADMIN' };
  });

  it('renders VALID status with licensee, seats, and features', async () => {
    vi.mocked(licenseService.getStatus).mockResolvedValue({ data: validStatus } as any);

    renderTab();

    await waitFor(() => {
      expect(screen.getByText('Licensed')).toBeInTheDocument();
    });
    expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    expect(screen.getByText('12 of 25 seats used')).toBeInTheDocument();
    expect(screen.getByText('sso')).toBeInTheDocument();
    expect(screen.getByText('audit-export')).toBeInTheDocument();
  });

  it('renders MISSING status with the Community Edition text', async () => {
    vi.mocked(licenseService.getStatus).mockResolvedValue({ data: missingStatus } as any);

    renderTab();

    await waitFor(() => {
      expect(screen.getByText('Community Edition')).toBeInTheDocument();
    });
    expect(
      screen.getByText('Community Edition — up to 10 users and 5 automations.')
    ).toBeInTheDocument();
    // No licensee/seats section for MISSING status
    expect(screen.queryByText('Acme Corp')).not.toBeInTheDocument();
  });

  it('renders GRACE status with the grace warning', async () => {
    vi.mocked(licenseService.getStatus).mockResolvedValue({ data: graceStatus } as any);

    renderTab();

    await waitFor(() => {
      expect(screen.getByText('Grace Period')).toBeInTheDocument();
    });
    expect(screen.getByText(/Grace period until/)).toBeInTheDocument();
  });

  it('shows upload/remove controls for an admin', async () => {
    vi.mocked(licenseService.getStatus).mockResolvedValue({ data: validStatus } as any);

    renderTab();

    await waitFor(() => {
      expect(screen.getByLabelText('Upload a licence file')).toBeInTheDocument();
    });
    expect(screen.getByLabelText('Or paste the licence content')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Upload Licence' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Remove Licence' })).toBeInTheDocument();
  });

  it('hides upload/remove controls for a non-admin, but still shows status', async () => {
    mockUser = { role: 'MEMBER' };
    vi.mocked(licenseService.getStatus).mockResolvedValue({ data: validStatus } as any);

    renderTab();

    await waitFor(() => {
      expect(screen.getByText('Licensed')).toBeInTheDocument();
    });
    expect(screen.queryByLabelText('Upload a licence file')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Upload Licence' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Remove Licence' })).not.toBeInTheDocument();
  });

  it('upload success shows a toast and clears the textarea', async () => {
    vi.mocked(licenseService.getStatus).mockResolvedValue({ data: missingStatus } as any);
    vi.mocked(licenseService.uploadLicense).mockResolvedValue({ data: validStatus } as any);

    renderTab();

    await waitFor(() => {
      expect(screen.getByLabelText('Or paste the licence content')).toBeInTheDocument();
    });

    const textarea = screen.getByLabelText('Or paste the licence content') as HTMLTextAreaElement;
    fireEvent.change(textarea, { target: { value: 'raw-license-content' } });

    fireEvent.click(screen.getByRole('button', { name: 'Upload Licence' }));

    await waitFor(() => {
      expect(licenseService.uploadLicense).toHaveBeenCalledWith('raw-license-content');
    });
    await waitFor(() => {
      expect(mockShowToast).toHaveBeenCalledWith('Licence uploaded successfully', 'success');
    });
    await waitFor(() => {
      expect(textarea.value).toBe('');
    });
  });

  it('upload failure with messageKey "license.invalid" shows the inline error', async () => {
    vi.mocked(licenseService.getStatus).mockResolvedValue({ data: missingStatus } as any);
    vi.mocked(licenseService.uploadLicense).mockRejectedValue({
      response: { data: { messageKey: 'license.invalid' } },
    });

    renderTab();

    await waitFor(() => {
      expect(screen.getByLabelText('Or paste the licence content')).toBeInTheDocument();
    });

    const textarea = screen.getByLabelText('Or paste the licence content');
    fireEvent.change(textarea, { target: { value: 'bad-content' } });
    fireEvent.click(screen.getByRole('button', { name: 'Upload Licence' }));

    await waitFor(() => {
      expect(screen.getByText('This licence file is invalid.')).toBeInTheDocument();
    });
    // No success toast on failure
    expect(mockShowToast).not.toHaveBeenCalledWith('Licence uploaded successfully', 'success');
  });

  it('remove flow opens the confirm dialog and calls the service on confirm', async () => {
    vi.mocked(licenseService.getStatus).mockResolvedValue({ data: validStatus } as any);
    vi.mocked(licenseService.removeLicense).mockResolvedValue({ data: missingStatus } as any);

    renderTab();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Remove Licence' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Remove Licence' }));

    const confirmBtn = await screen.findByRole('button', { name: 'Confirm' });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(licenseService.removeLicense).toHaveBeenCalledOnce();
    });
    await waitFor(() => {
      expect(mockShowToast).toHaveBeenCalledWith('Licence removed', 'success');
    });
  });
});
