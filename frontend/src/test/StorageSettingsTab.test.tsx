import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StorageSettingsTab } from '../components/organizationSettings/StorageSettingsTab';

// ── Service mock ──────────────────────────────────────────────────────────────

vi.mock('../services/storageService', () => ({
  storageService: {
    getStorageConfig: vi.fn(),
    updateStorageConfig: vi.fn(),
    testStorageConnection: vi.fn(),
    migrateStorage: vi.fn(),
  },
}));

// ── Context mock ──────────────────────────────────────────────────────────────

const mockShowToast = vi.fn();

vi.mock('../contexts', () => ({
  useToast: () => ({ showToast: mockShowToast }),
}));

// ── i18n passthrough ──────────────────────────────────────────────────────────
// The setup.ts already initializes i18n with initReactI18next. We need to
// add storage keys so the component renders meaningful text.

import i18n from 'i18next';

// Merge storage keys into the test i18n instance
const storageKeys = {
  'storage.tabLabel': 'Storage',
  'storage.title': 'Object Storage',
  'storage.subtitle': 'Configure where uploaded files are stored',
  'storage.provider': 'Storage Provider',
  'storage.providerLocal': 'Local Filesystem',
  'storage.providerS3': 'Amazon S3',
  'storage.providerMinio': 'MinIO (S3-compatible)',
  'storage.localInfo': 'Files are stored on the server filesystem.',
  'storage.bucket': 'Bucket Name',
  'storage.endpoint': 'Endpoint URL',
  'storage.region': 'Region',
  'storage.accessKey': 'Access Key ID',
  'storage.secretKey': 'Secret Access Key',
  'storage.pathStyleAccess': 'Path-Style Access',
  'storage.keyConfigured': 'Configured — leave blank to keep',
  'storage.testConnection': 'Test Connection',
  'storage.testing': 'Testing…',
  'storage.testSuccess': 'Connection successful',
  'storage.testFailed': 'Connection test failed',
  'storage.migrateFiles': 'Migrate Existing Files',
  'storage.migrateConfirmTitle': 'Migrate Files to New Storage',
  'storage.migrateConfirm': 'This will copy all existing uploads to the new storage backend.',
  'storage.migrateResult': 'Migration complete: {{migrated}} migrated',
  'storage.migrateFailed': 'Migration failed',
  'storage.saveSuccess': 'Storage settings saved',
  'storage.saveFailed': 'Failed to save storage settings',
  'storage.loadFailed': 'Failed to load storage settings',
  'common.save': 'Save',
  'common.cancel': 'Cancel',
  'common.confirm': 'Confirm',
};

// Patch i18n resources — runs once before all tests
Object.entries(storageKeys).forEach(([key, value]) => {
  i18n.addResource('en', 'translation', key, value);
});

// ── Helpers ───────────────────────────────────────────────────────────────────

import { storageService } from '../services/storageService';

const makeQueryClient = () =>
  new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

function renderTab(queryClient: QueryClient) {
  return render(
    <QueryClientProvider client={queryClient}>
      <StorageSettingsTab />
    </QueryClientProvider>
  );
}

const s3Config = {
  activeProvider: 'S3' as const,
  bucket: 'my-bucket',
  region: 'us-east-1',
  hasAccessKey: true,
  hasSecretKey: true,
};

const localConfig = {
  activeProvider: 'LOCAL_FS' as const,
  hasAccessKey: false,
  hasSecretKey: false,
};

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('StorageSettingsTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('(a) provider select renders; switching to S3 shows bucket/region/accessKey/secretKey fields', async () => {
    // Start with S3 config so the form populates with S3 as the active provider.
    // Radix UI Select does not work with jsdom (hasPointerCapture not implemented),
    // so we verify the conditional rendering logic by loading with an S3 config directly.
    vi.mocked(storageService.getStorageConfig).mockResolvedValue({
      data: s3Config,
    } as any);

    const qc = makeQueryClient();
    renderTab(qc);

    // S3 fields appear after the useEffect-driven reset fires
    await waitFor(() => {
      expect(screen.getByLabelText('Bucket Name')).toBeInTheDocument();
    });
    expect(screen.getByLabelText('Region')).toBeInTheDocument();
    expect(screen.getByLabelText('Access Key ID')).toBeInTheDocument();
    expect(screen.getByLabelText('Secret Access Key')).toBeInTheDocument();

    // LOCAL_FS info callout is NOT shown when provider is S3
    expect(
      screen.queryByText('Files are stored on the server filesystem.')
    ).not.toBeInTheDocument();

    // The provider combobox is rendered
    expect(screen.getByRole('combobox')).toBeInTheDocument();
  });

  it('(b) when hasSecretKey is true, secretKey field is empty and helper text appears', async () => {
    vi.mocked(storageService.getStorageConfig).mockResolvedValue({
      data: s3Config,
    } as any);

    const qc = makeQueryClient();
    renderTab(qc);

    // Wait for data to load and form to reset
    await waitFor(() => {
      expect(screen.getByLabelText('Secret Access Key')).toBeInTheDocument();
    });

    // Secret field must be empty — API never returns actual credentials
    const secretField = screen.getByLabelText('Secret Access Key') as HTMLInputElement;
    expect(secretField.value).toBe('');

    // Helper text must appear because hasSecretKey === true
    const helperInstances = screen.getAllByText('Configured — leave blank to keep');
    expect(helperInstances.length).toBeGreaterThanOrEqual(1);
  });

  it('(c) clicking "Test Connection" calls storageService.testStorageConnection', async () => {
    vi.mocked(storageService.getStorageConfig).mockResolvedValue({
      data: s3Config,
    } as any);
    vi.mocked(storageService.testStorageConnection).mockResolvedValue({
      data: { ok: true, message: 'OK' },
    } as any);

    const qc = makeQueryClient();
    renderTab(qc);

    // Wait for the test button to appear (only shown for non-LOCAL_FS)
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Test Connection' })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Test Connection' }));

    await waitFor(() => {
      expect(storageService.testStorageConnection).toHaveBeenCalledOnce();
    });

    expect(mockShowToast).toHaveBeenCalledWith('Connection successful', 'success');
  });

  it('(d) clicking "Migrate Existing Files" calls storageService.migrateStorage after confirm', async () => {
    vi.mocked(storageService.getStorageConfig).mockResolvedValue({
      data: localConfig,
    } as any);
    vi.mocked(storageService.migrateStorage).mockResolvedValue({
      data: { migrated: 10, skipped: 2, failed: 0, total: 12 },
    } as any);

    const qc = makeQueryClient();
    renderTab(qc);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Migrate Existing Files' })).toBeInTheDocument();
    });

    // Open confirm dialog
    fireEvent.click(screen.getByRole('button', { name: 'Migrate Existing Files' }));

    // Confirm dialog should appear
    const confirmBtn = await screen.findByRole('button', { name: 'Confirm' });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(storageService.migrateStorage).toHaveBeenCalledOnce();
    });
  });
});
