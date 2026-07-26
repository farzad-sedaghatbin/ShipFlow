import { useTranslation } from 'react-i18next';
import { WifiOff } from 'lucide-react';
import { useOnlineStatus } from '@/hooks/useOnlineStatus';

/**
 * Persistent bar shown across the top of the page content while the browser
 * is offline. Renders nothing when online — it isn't a dismissible toast
 * because "you're offline" is ongoing state, not a one-time event, and
 * dismissing it would leave users guessing why saves keep saying "queued".
 *
 * Actual write-queuing/replay happens in the service worker (src/sw.ts) and
 * the axios interceptor (services/api.ts) — this component only reflects
 * connectivity, it doesn't drive any sync logic itself.
 */
export function OfflineBanner() {
  const { t } = useTranslation();
  const isOnline = useOnlineStatus();

  if (isOnline) {
    return null;
  }

  return (
    <div
      role="status"
      data-testid="offline-banner"
      className="flex items-center justify-center gap-2 bg-warning/10 border-b border-warning/50 text-warning text-sm px-3 py-1.5"
    >
      <WifiOff className="h-3.5 w-3.5 shrink-0" />
      <span>{t('pwa.offlineBanner')}</span>
    </div>
  );
}

export default OfflineBanner;
