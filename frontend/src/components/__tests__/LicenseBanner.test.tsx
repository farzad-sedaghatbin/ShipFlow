import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { LicenseBanner } from '../LicenseBanner';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

const mockUseAuth = vi.fn();
vi.mock('../../contexts', () => ({
  useAuth: () => mockUseAuth(),
}));

const mockUseLicenseStatus = vi.fn();
vi.mock('../../hooks/useLicense', () => ({
  useLicenseStatus: () => mockUseLicenseStatus(),
}));

const adminUser = { role: 'ADMIN' };
const memberUser = { role: 'MEMBER' };

interface LicenseOverrides {
  status: 'MISSING' | 'VALID' | 'GRACE' | 'EXPIRED';
  expiresAt?: string | null;
  graceEndsAt?: string | null;
}

function licenseQueryResult(overrides: LicenseOverrides) {
  return {
    data: {
      status: overrides.status,
      edition: 'COMMERCIAL',
      licensee: 'Acme',
      seats: 10,
      seatsUsed: 5,
      features: [],
      issuedAt: '2026-01-01',
      expiresAt: overrides.expiresAt ?? '2027-01-01',
      supportUntil: '2027-01-01',
      graceEndsAt: overrides.graceEndsAt ?? null,
      communityCap: 10,
      automationCap: 5,
      automationsUsed: 1,
    },
    isLoading: false,
    isError: false,
  };
}

describe('LicenseBanner', () => {
  beforeEach(() => {
    localStorage.clear();
    mockUseAuth.mockReturnValue({ user: adminUser });
  });

  it('renders nothing for a non-admin user', () => {
    mockUseAuth.mockReturnValue({ user: memberUser });
    mockUseLicenseStatus.mockReturnValue(licenseQueryResult({ status: 'EXPIRED' }));

    render(<LicenseBanner />);
    expect(screen.queryByTestId('license-banner')).not.toBeInTheDocument();
  });

  it('renders nothing when status is VALID', () => {
    mockUseLicenseStatus.mockReturnValue(licenseQueryResult({ status: 'VALID' }));
    render(<LicenseBanner />);
    expect(screen.queryByTestId('license-banner')).not.toBeInTheDocument();
  });

  it('renders nothing when status is MISSING', () => {
    mockUseLicenseStatus.mockReturnValue(licenseQueryResult({ status: 'MISSING' }));
    render(<LicenseBanner />);
    expect(screen.queryByTestId('license-banner')).not.toBeInTheDocument();
  });

  it('renders nothing while the status query is loading', () => {
    mockUseLicenseStatus.mockReturnValue({ data: undefined, isLoading: true, isError: false });
    render(<LicenseBanner />);
    expect(screen.queryByTestId('license-banner')).not.toBeInTheDocument();
  });

  it('renders nothing when the status query errors', () => {
    mockUseLicenseStatus.mockReturnValue({ data: undefined, isLoading: false, isError: true });
    render(<LicenseBanner />);
    expect(screen.queryByTestId('license-banner')).not.toBeInTheDocument();
  });

  it('renders the grace message when GRACE', () => {
    mockUseLicenseStatus.mockReturnValue(
      licenseQueryResult({ status: 'GRACE', graceEndsAt: '2027-02-01' })
    );
    render(<LicenseBanner />);
    expect(screen.getByTestId('license-banner')).toBeInTheDocument();
    expect(screen.getByText('license.graceBannerMessage')).toBeInTheDocument();
  });

  it('renders the expired message when EXPIRED', () => {
    mockUseLicenseStatus.mockReturnValue(licenseQueryResult({ status: 'EXPIRED' }));
    render(<LicenseBanner />);
    expect(screen.getByTestId('license-banner')).toBeInTheDocument();
    expect(screen.getByText('license.expiredBannerMessage')).toBeInTheDocument();
  });

  it('dismiss button hides it, and it stays hidden on a re-render with the same status+expiresAt', () => {
    mockUseLicenseStatus.mockReturnValue(
      licenseQueryResult({ status: 'EXPIRED', expiresAt: '2027-01-01' })
    );
    const { rerender } = render(<LicenseBanner />);
    expect(screen.getByTestId('license-banner')).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('common.close'));
    expect(screen.queryByTestId('license-banner')).not.toBeInTheDocument();

    // Re-render with the identical {status, expiresAt} — stays dismissed.
    rerender(<LicenseBanner />);
    expect(screen.queryByTestId('license-banner')).not.toBeInTheDocument();
  });

  it('reappears if the status changes after a prior dismissal', () => {
    mockUseLicenseStatus.mockReturnValue(
      licenseQueryResult({ status: 'GRACE', expiresAt: '2027-01-01', graceEndsAt: '2027-02-01' })
    );
    const { rerender } = render(<LicenseBanner />);
    fireEvent.click(screen.getByLabelText('common.close'));
    expect(screen.queryByTestId('license-banner')).not.toBeInTheDocument();

    // Status flips GRACE -> EXPIRED with the same expiresAt: must reappear.
    mockUseLicenseStatus.mockReturnValue(
      licenseQueryResult({ status: 'EXPIRED', expiresAt: '2027-01-01' })
    );
    rerender(<LicenseBanner />);
    expect(screen.getByTestId('license-banner')).toBeInTheDocument();
    expect(screen.getByText('license.expiredBannerMessage')).toBeInTheDocument();
  });

  it('reappears if expiresAt changes after a prior dismissal (a new licence was uploaded)', () => {
    mockUseLicenseStatus.mockReturnValue(
      licenseQueryResult({ status: 'EXPIRED', expiresAt: '2027-01-01' })
    );
    const { rerender } = render(<LicenseBanner />);
    fireEvent.click(screen.getByLabelText('common.close'));
    expect(screen.queryByTestId('license-banner')).not.toBeInTheDocument();

    // Same status, but a different expiresAt (freshly uploaded licence): must reappear.
    mockUseLicenseStatus.mockReturnValue(
      licenseQueryResult({ status: 'EXPIRED', expiresAt: '2027-06-01' })
    );
    rerender(<LicenseBanner />);
    expect(screen.getByTestId('license-banner')).toBeInTheDocument();
  });
});
