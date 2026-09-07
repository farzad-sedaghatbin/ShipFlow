import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { AlertTriangle, X } from 'lucide-react';
import { useAuth } from '../contexts';
import { useLicenseStatus } from '../hooks/useLicense';
import { formatDate } from '../utils/dateUtils';

const DISMISS_KEY = 'shipflow_license_banner_dismissed';

interface DismissedRecord {
  status: string;
  expiresAt: string | null;
}

function readDismissed(): DismissedRecord | null {
  try {
    const raw = localStorage.getItem(DISMISS_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as DismissedRecord;
  } catch {
    // Private browsing or storage disabled — treat as "not dismissed".
    return null;
  }
}

function writeDismissed(record: DismissedRecord): void {
  try {
    localStorage.setItem(DISMISS_KEY, JSON.stringify(record));
  } catch {
    // Private browsing or storage disabled — the banner simply reappears
    // next load, an acceptable degradation for a dismiss preference.
  }
}

/**
 * Bar shown to admins across the top of the page content while the ShipFlow
 * licence is in its grace period or has expired. Mounted in Layout.tsx
 * immediately after OfflineBanner.
 *
 * Unlike OfflineBanner (ongoing connectivity state, deliberately NOT
 * dismissible), a licence's grace/expired status is comparatively static, so
 * it's fair to let an admin dismiss it — but the dismissal is scoped to the
 * CURRENT {status, expiresAt} pair (not a permanent one-time-forever flag
 * like PostLoginPrompts' dismiss keys). A status change (e.g. GRACE ->
 * EXPIRED) or a freshly uploaded licence (different expiresAt) makes the
 * banner reappear even though a stale dismissal is still in localStorage.
 */
export function LicenseBanner() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const { data: license, isLoading, isError } = useLicenseStatus();

  const [dismissedRecord, setDismissedRecord] = useState<DismissedRecord | null>(() => readDismissed());

  if (user?.role !== 'ADMIN') {
    return null;
  }
  if (isLoading || isError || !license) {
    return null;
  }
  if (license.status !== 'GRACE' && license.status !== 'EXPIRED') {
    return null;
  }

  const currentExpiresAt = license.expiresAt ?? null;
  const isDismissed =
    dismissedRecord !== null &&
    dismissedRecord.status === license.status &&
    dismissedRecord.expiresAt === currentExpiresAt;

  if (isDismissed) {
    return null;
  }

  const handleDismiss = () => {
    const record: DismissedRecord = { status: license.status, expiresAt: currentExpiresAt };
    writeDismissed(record);
    setDismissedRecord(record);
  };

  const isGrace = license.status === 'GRACE';

  return (
    <div
      role="status"
      data-testid="license-banner"
      className={
        isGrace
          ? 'flex items-center justify-between gap-2 bg-warning/10 border-b border-warning/50 text-warning text-sm px-3 py-1.5'
          : 'flex items-center justify-between gap-2 bg-destructive/10 border-b border-destructive/50 text-destructive text-sm px-3 py-1.5'
      }
    >
      <span className="flex items-center gap-2">
        <AlertTriangle className="h-3.5 w-3.5 shrink-0" />
        <span>
          {isGrace
            ? t('license.graceBannerMessage', {
                graceEndsAt: license.graceEndsAt ? formatDate(license.graceEndsAt) : '',
              })
            : t('license.expiredBannerMessage')}
        </span>
      </span>
      <button
        type="button"
        onClick={handleDismiss}
        aria-label={t('common.close')}
        className="shrink-0 opacity-70 hover:opacity-100"
      >
        <X className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}

export default LicenseBanner;
