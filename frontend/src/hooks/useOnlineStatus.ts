import { useEffect, useState } from 'react';

/**
 * Tracks the browser's connectivity via the `online`/`offline` window events,
 * seeded from `navigator.onLine`. Backs the offline banner (see
 * `components/OfflineBanner.tsx`) and gates any UI that shouldn't be offered
 * while offline.
 *
 * Note: `navigator.onLine` only reflects whether the device has a network
 * interface up, not whether the ShipFlow API is actually reachable — a
 * captive portal or backend outage can leave this `true` with no real
 * connectivity. That's an accepted limitation shared by every browser
 * implementation of the API; the service worker's own network-first/
 * network-only strategies (src/sw.ts) are what ultimately determine whether
 * a request succeeds.
 */
export function useOnlineStatus(): boolean {
  const [isOnline, setIsOnline] = useState(() =>
    typeof navigator === 'undefined' ? true : navigator.onLine,
  );

  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  return isOnline;
}
