import { registerSW } from 'virtual:pwa-register';
import { showGlobalToast } from '@/contexts';
import i18n from '@/i18n';

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
}

// Module-level (not React state) because `beforeinstallprompt` fires once,
// early, outside any component's lifecycle — captured here so any component
// mounted later (e.g. a post-login prompt) can still read/consume it.
let deferredInstallPrompt: BeforeInstallPromptEvent | null = null;
let appInstalled = false;

/**
 * Registers the service worker (src/sw.ts) and wires up the pieces of
 * user-visible feedback that a bare `registerSW()` call doesn't give you:
 *
 * 1. `onOfflineReady` — a one-time toast confirming the app shell is cached
 *    and usable offline (first load / after an update).
 * 2. A `message` listener for `shipflow-sync-complete`, posted by the
 *    service worker's custom background-sync `onSync` handler once queued
 *    offline writes have actually been replayed (see `src/sw.ts`) — without
 *    this, a change made offline appears to vanish silently until the user
 *    happens to refresh and see it.
 * 3. Capturing `beforeinstallprompt` so the app can offer installation at a
 *    moment of its own choosing (see `promptPwaInstall` below) instead of
 *    whatever moment the browser would otherwise pick.
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

  // Chromium-based browsers only — iOS Safari never fires this event, so
  // there is no code-driven install prompt possible there (Apple platform
  // limitation, not something this app can work around).
  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault();
    deferredInstallPrompt = event as BeforeInstallPromptEvent;
  });

  window.addEventListener('appinstalled', () => {
    appInstalled = true;
    deferredInstallPrompt = null;
  });
}

/** Whether the app is already running installed/standalone (Android Chrome via matchMedia, iOS Safari via the non-standard `navigator.standalone`). */
export function isPwaInstalled(): boolean {
  if (appInstalled) return true;
  if (typeof window === 'undefined') return false;
  if (window.matchMedia?.('(display-mode: standalone)').matches) return true;
  return (window.navigator as Navigator & { standalone?: boolean }).standalone === true;
}

/** Whether a captured `beforeinstallprompt` event is available to show right now. */
export function canPromptPwaInstall(): boolean {
  return deferredInstallPrompt !== null;
}

/**
 * Shows the captured native install prompt. Resolves to the user's choice,
 * or `'unavailable'` if no prompt was ever captured (already installed,
 * browser doesn't support it, or the one-shot event was already consumed).
 */
export async function promptPwaInstall(): Promise<'accepted' | 'dismissed' | 'unavailable'> {
  if (!deferredInstallPrompt) return 'unavailable';
  const prompt = deferredInstallPrompt;
  deferredInstallPrompt = null;
  await prompt.prompt();
  const choice = await prompt.userChoice;
  return choice.outcome;
}
