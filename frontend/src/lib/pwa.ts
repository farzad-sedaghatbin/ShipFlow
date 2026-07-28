import { registerSW } from 'virtual:pwa-register';
import { showGlobalToast } from '@/contexts';
import i18n from '@/i18n';

/**
 * Registers the service worker (src/sw.ts) and wires up the two pieces of
 * user-visible feedback that a bare `registerSW()` call doesn't give you:
 *
 * 1. `onOfflineReady` — a one-time toast confirming the app shell is cached
 *    and usable offline (first load / after an update).
 * 2. A `message` listener for `shipflow-sync-complete`, posted by the
 *    service worker's custom background-sync `onSync` handler once queued
 *    offline writes have actually been replayed (see `src/sw.ts`) — without
 *    this, a change made offline appears to vanish silently until the user
 *    happens to refresh and see it.
 *
 * Call once from `main.tsx`. No-op during Vitest/SSR where there's no real
 * browser `navigator.serviceWorker`.
 */
export function initPwa(): void {
  if (typeof navigator === 'undefined' || !('serviceWorker' in navigator)) {
    return;
  }

  registerSW({
    immediate: true,
    onOfflineReady() {
      showGlobalToast(i18n.t('pwa.offlineReady'), 'success');
    },
  });

  navigator.serviceWorker.addEventListener('message', (event) => {
    if (event.data?.type === 'shipflow-sync-complete') {
      const count = typeof event.data.count === 'number' ? event.data.count : 1;
      showGlobalToast(i18n.t('pwa.syncComplete', { count }), 'success');
    }
  });
}
