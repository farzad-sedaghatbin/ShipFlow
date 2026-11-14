import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { EmailSettingsTab } from '../EmailSettingsTab';

const showToast = vi.fn();
const getEmailStatus = vi.fn();
const sendTestEmail = vi.fn();

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, vars?: Record<string, string>) => (vars?.reason ? `${key}: ${vars.reason}` : key),
  }),
}));
vi.mock('../../../contexts', () => ({ useToast: () => ({ showToast }) }));
vi.mock('../../../services/organizationSettingsService', () => ({
  organizationSettingsService: {
    getEmailStatus: () => getEmailStatus(),
    sendTestEmail: () => sendTestEmail(),
  },
}));

const configured = {
  configured: true,
  host: 'mail.goldorigins.com',
  port: 587,
  username: 'shipflow@goldorigins.com',
  from: 'GoldOrigins ShipFlow <shipflow@goldorigins.com>',
  replyTo: null,
  baseUrl: 'https://shipflow.goldorigins.com',
  starttlsRequired: true,
};

function renderTab() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <EmailSettingsTab />
    </QueryClientProvider>,
  );
}

describe('EmailSettingsTab', () => {
  beforeEach(() => {
    showToast.mockReset();
    getEmailStatus.mockReset();
    sendTestEmail.mockReset();
  });

  it('shows what the server is really using, not editable boxes', async () => {
    getEmailStatus.mockResolvedValue({ data: configured });
    renderTab();

    await waitFor(() => expect(screen.getByTestId('email-status').textContent).toBe('emailSettings.statusOn'));
    expect(screen.getByText('mail.goldorigins.com')).toBeTruthy();
    expect(screen.getByText('GoldOrigins ShipFlow <shipflow@goldorigins.com>')).toBeTruthy();
    expect(screen.queryByRole('textbox')).toBeNull();
    expect(screen.queryByRole('switch')).toBeNull();
  });

  it('says email is off and disables the test button when SMTP is not configured', async () => {
    getEmailStatus.mockResolvedValue({
      data: { ...configured, configured: false, host: null, port: null, username: null, from: null, baseUrl: null },
    });
    renderTab();

    await waitFor(() => expect(screen.getByTestId('email-status').textContent).toBe('emailSettings.statusOff'));
    expect((screen.getByRole('button') as HTMLButtonElement).disabled).toBe(true);
  });

  it('shows the real reason when the test email fails', async () => {
    getEmailStatus.mockResolvedValue({ data: configured });
    sendTestEmail.mockResolvedValue({ data: { error: '535 Authentication failed' } });
    renderTab();
    await waitFor(() => screen.getByTestId('email-status'));

    fireEvent.click(screen.getByRole('button'));

    await waitFor(() =>
      expect(showToast).toHaveBeenCalledWith('emailSettings.testEmailFailed: 535 Authentication failed', 'error'),
    );
  });

  it('confirms when the test email is sent', async () => {
    getEmailStatus.mockResolvedValue({ data: configured });
    sendTestEmail.mockResolvedValue({ data: { message: 'Test email sent to farzad@goldorigins.com' } });
    renderTab();
    await waitFor(() => screen.getByTestId('email-status'));

    fireEvent.click(screen.getByRole('button'));

    await waitFor(() => expect(showToast).toHaveBeenCalledWith('emailSettings.testEmailSent', 'success'));
  });
});
