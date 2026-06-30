import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import i18n from 'i18next';
import { AuditExportSettingsTab } from '../components/organizationSettings/AuditExportSettingsTab';

// ── Service mock ──────────────────────────────────────────────────────────────

vi.mock('../services/auditService', () => ({
  auditService: {
    exportAuditTrail: vi.fn(),
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

const auditKeys = {
  'auditExport.tabLabel': 'Audit Export',
  'auditExport.title': 'Audit Trail Export',
  'auditExport.subtitle': 'Download the change history.',
  'auditExport.entityType': 'Entity Type',
  'auditExport.entityTask': 'Task',
  'auditExport.entityBug': 'Bug Report',
  'auditExport.entityPitch': 'Pitch',
  'auditExport.entityTestCase': 'Test Case',
  'auditExport.entityAll': 'All',
  'auditExport.fromDate': 'From Date',
  'auditExport.toDate': 'To Date',
  'auditExport.format': 'Format',
  'auditExport.formatCsv': 'CSV',
  'auditExport.formatJson': 'JSON',
  'auditExport.export': 'Export',
  'auditExport.exporting': 'Exporting…',
  'auditExport.helperText': 'Exports the Envers audit trail.',
  'auditExport.exportFailed': 'Failed to export audit trail',
  'auditExport.dateRangeError': 'The "From" date must be on or before the "To" date.',
};

Object.entries(auditKeys).forEach(([key, value]) => {
  i18n.addResource('en', 'translation', key, value);
});

// ── Helpers ───────────────────────────────────────────────────────────────────

import { auditService } from '../services/auditService';

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('AuditExportSettingsTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = { role: 'ADMIN' };
  });

  it('renders the export controls for an admin', () => {
    render(<AuditExportSettingsTab />);

    expect(screen.getByText('Audit Trail Export')).toBeInTheDocument();
    expect(screen.getByLabelText('From Date')).toBeInTheDocument();
    expect(screen.getByLabelText('To Date')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Export' })).toBeInTheDocument();
  });

  it('renders nothing for a non-admin user', () => {
    mockUser = { role: 'DEVELOPER' };
    const { container } = render(<AuditExportSettingsTab />);

    expect(container).toBeEmptyDOMElement();
    expect(screen.queryByRole('button', { name: 'Export' })).not.toBeInTheDocument();
  });

  it('renders nothing when there is no authenticated user', () => {
    mockUser = null;
    const { container } = render(<AuditExportSettingsTab />);

    expect(container).toBeEmptyDOMElement();
  });

  it('clicking Export calls the service with the selected params (defaults)', async () => {
    vi.mocked(auditService.exportAuditTrail).mockResolvedValue(undefined);

    render(<AuditExportSettingsTab />);

    fireEvent.click(screen.getByRole('button', { name: 'Export' }));

    await waitFor(() => {
      expect(auditService.exportAuditTrail).toHaveBeenCalledTimes(1);
    });
    expect(auditService.exportAuditTrail).toHaveBeenCalledWith({
      entityType: 'all',
      format: 'csv',
      from: undefined,
      to: undefined,
    });
  });

  it('passes the chosen date range to the service', async () => {
    vi.mocked(auditService.exportAuditTrail).mockResolvedValue(undefined);

    render(<AuditExportSettingsTab />);

    fireEvent.change(screen.getByLabelText('From Date'), { target: { value: '2026-01-01' } });
    fireEvent.change(screen.getByLabelText('To Date'), { target: { value: '2026-06-30' } });
    fireEvent.click(screen.getByRole('button', { name: 'Export' }));

    await waitFor(() => {
      expect(auditService.exportAuditTrail).toHaveBeenCalledWith({
        entityType: 'all',
        format: 'csv',
        from: '2026-01-01',
        to: '2026-06-30',
      });
    });
  });

  it('blocks an inverted date range and shows an error toast', async () => {
    render(<AuditExportSettingsTab />);

    fireEvent.change(screen.getByLabelText('From Date'), { target: { value: '2026-06-30' } });
    fireEvent.change(screen.getByLabelText('To Date'), { target: { value: '2026-01-01' } });
    fireEvent.click(screen.getByRole('button', { name: 'Export' }));

    expect(mockShowToast).toHaveBeenCalledWith(
      'The "From" date must be on or before the "To" date.',
      'error'
    );
    expect(auditService.exportAuditTrail).not.toHaveBeenCalled();
  });

  it('shows an error toast when the export fails', async () => {
    vi.mocked(auditService.exportAuditTrail).mockRejectedValue(new Error('boom'));

    render(<AuditExportSettingsTab />);

    fireEvent.click(screen.getByRole('button', { name: 'Export' }));

    await waitFor(() => {
      expect(mockShowToast).toHaveBeenCalledWith('Failed to export audit trail', 'error');
    });
  });
});
