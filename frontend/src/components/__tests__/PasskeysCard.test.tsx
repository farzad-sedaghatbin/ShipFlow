import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PasskeysCard } from '../PasskeysCard';
import { passkeyService, type PasskeyDTO } from '../../services/passkeyService';
import { isWebAuthnSupported } from '../../lib/webauthn';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: Record<string, unknown>) =>
      opts ? `${key}:${JSON.stringify(opts)}` : key,
    i18n: { language: 'en' },
  }),
}));

const showSuccess = vi.fn();
const showError = vi.fn();
vi.mock('../../contexts', () => ({
  useToast: () => ({ showSuccess, showError }),
}));

vi.mock('../../services/passkeyService', () => ({
  passkeyService: {
    listPasskeys: vi.fn(),
    deletePasskey: vi.fn(),
    getRegistrationOptions: vi.fn(),
    verifyRegistration: vi.fn(),
  },
}));

vi.mock('../../lib/webauthn', async () => {
  const actual = await vi.importActual<typeof import('../../lib/webauthn')>('../../lib/webauthn');
  return {
    ...actual,
    isWebAuthnSupported: vi.fn(() => true),
    registerPasskey: vi.fn(),
  };
});

const mockList = vi.mocked(passkeyService.listPasskeys);
const mockDelete = vi.mocked(passkeyService.deletePasskey);
const mockSupported = vi.mocked(isWebAuthnSupported);

const samplePasskeys: PasskeyDTO[] = [
  { id: 1, deviceName: 'MacBook Pro', createdAt: '2026-07-01T00:00:00Z', lastUsedAt: '2026-07-20T00:00:00Z', transports: ['internal'] },
  { id: 2, deviceName: 'YubiKey 5', createdAt: '2026-07-10T00:00:00Z', transports: ['usb'] },
];

function renderCard() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <PasskeysCard />
    </QueryClientProvider>
  );
}

describe('PasskeysCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockSupported.mockReturnValue(true);
  });

  it('renders nothing when WebAuthn is not supported', () => {
    mockSupported.mockReturnValue(false);
    const { container } = renderCard();
    expect(container).toBeEmptyDOMElement();
  });

  it('renders an empty state when the user has no passkeys', async () => {
    mockList.mockResolvedValue([]);
    renderCard();
    await waitFor(() => expect(mockList).toHaveBeenCalled());
    expect(await screen.findByText('passkeys.noPasskeys')).toBeInTheDocument();
  });

  it('lists registered passkeys with device name, added date, and last-used date', async () => {
    mockList.mockResolvedValue(samplePasskeys);
    renderCard();

    expect(await screen.findByText('MacBook Pro')).toBeInTheDocument();
    expect(screen.getByText('YubiKey 5')).toBeInTheDocument();
    // Second passkey never used — shows the "never used" key instead of an interpolated lastUsedOn.
    const yubikeyRow = screen.getByText('YubiKey 5').closest('li')!;
    expect(within(yubikeyRow).getByText(/passkeys\.neverUsed/)).toBeInTheDocument();
  });

  it('calls deletePasskey when a delete is confirmed', async () => {
    const user = userEvent.setup();
    mockList.mockResolvedValue(samplePasskeys);
    mockDelete.mockResolvedValue(undefined);
    renderCard();

    await screen.findByText('MacBook Pro');

    const deleteButtons = screen.getAllByLabelText('passkeys.deleteTitle');
    await user.click(deleteButtons[0]);

    // Confirmation dialog appears — confirm via the destructive "delete" button.
    const dialogDeleteButton = await screen.findByRole('button', { name: 'common.delete' });
    await user.click(dialogDeleteButton);

    await waitFor(() => expect(mockDelete).toHaveBeenCalledWith(1));
    await waitFor(() => expect(showSuccess).toHaveBeenCalledWith('passkeys.deleteSuccess'));
  });
});
