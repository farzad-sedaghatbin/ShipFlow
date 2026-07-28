/// <reference lib="webworker" />
/// <reference types="vite-plugin-pwa/client" />

// ShipFlow service worker (S57 — PWA shell + offline support).
//
// Uses vite-plugin-pwa's `injectManifest` strategy (not `generateSW`) because
// the background-sync replay notification below needs a hand-written
// `onSync` callback that posts a message back to open tabs — `generateSW`'s
// declarative `runtimeCaching` config has no hook for that.
//
// See CLAUDE.md's Architectural Decisions Log / `PWA_GUIDE.md` for the
// rationale on what's cached vs. always network-only.

import { precacheAndRoute, cleanupOutdatedCaches } from 'workbox-precaching';
import { registerRoute, NavigationRoute } from 'workbox-routing';
import { NetworkFirst, CacheFirst, NetworkOnly } from 'workbox-strategies';
import { BackgroundSyncPlugin } from 'workbox-background-sync';
import { ExpirationPlugin } from 'workbox-expiration';

declare let self: ServiceWorkerGlobalScope;

// Activate the new SW immediately instead of waiting for all tabs of the old
// version to close — paired with `registerType: 'autoUpdate'` client-side so
// deploys roll out without a manual "reload to update" prompt.
self.skipWaiting();
cleanupOutdatedCaches();

// Precache the build's hashed assets + index.html (manifest injected by
// vite-plugin-pwa at build time via `self.__WB_MANIFEST`).
precacheAndRoute(self.__WB_MANIFEST);

// ─── SPA navigation fallback ────────────────────────────────────────────────
// Any full-page navigation (e.g. a hard refresh on /pitches/42 while offline)
// falls back to the cached shell instead of a browser "no internet" page.
// API and backend-served routes are excluded — those should surface a real
// network error, not the SPA shell.
registerRoute(
  new NavigationRoute(new NetworkFirst({ cacheName: 'shipflow-navigation' }), {
    denylist: [/^\/api\//, /^\/swagger-ui/, /^\/actuator/, /^\/mcp/],
  })
);

// ─── Read-through cache for GET /api/** ────────────────────────────────────
// Network-first: always try live data first (8s timeout), fall back to the
// last good response when offline so pages that already loaded once remain
// browsable without a connection. Not a substitute for real sync — just
// keeps the last-seen state visible.
registerRoute(
  ({ url, request }) => url.pathname.startsWith('/api/') && request.method === 'GET',
  new NetworkFirst({
    cacheName: 'shipflow-api-cache',
    networkTimeoutSeconds: 8,
    plugins: [new ExpirationPlugin({ maxEntries: 300, maxAgeSeconds: 60 * 60 * 24 })],
  })
);

// ─── Static assets ──────────────────────────────────────────────────────────
registerRoute(
  ({ request }) => request.destination === 'image' || request.destination === 'font',
  new CacheFirst({
    cacheName: 'shipflow-asset-cache',
    plugins: [new ExpirationPlugin({ maxEntries: 150, maxAgeSeconds: 60 * 60 * 24 * 30 })],
  })
);

// ─── Background sync for offline writes ────────────────────────────────────
// POST/PUT/PATCH/DELETE calls to /api/** are NetworkOnly (never served from
// cache — a queued write must never look like it silently succeeded from a
// stale cache). When the network fetch fails, BackgroundSyncPlugin queues
// the request in IndexedDB and Workbox replays it automatically once the
// browser fires a `sync` event (or, on browsers without the Background Sync
// API — e.g. Safari — on the next fetch through this same route once the
// user is back online, per Workbox's fallback behavior).
//
// The default plugin behavior gives no feedback to the page once a queued
// request finally sends, which leaves users unsure whether a change made
// offline ever went through. The custom `onSync` here replays the queue
// itself and, once at least one request replays successfully, posts a
// `shipflow-sync-complete` message to every open tab — see
// `src/lib/pwa.ts`, which listens for it and shows a toast.
const mutationQueue = new BackgroundSyncPlugin('shipflow-mutations-queue', {
  maxRetentionTime: 24 * 60, // minutes (24 hours)
  onSync: async ({ queue }) => {
    let syncedCount = 0;
    let entry;
    for (;;) {
      entry = await queue.shiftRequest();
      if (!entry) break;
      try {
        await fetch(entry.request.clone());
        syncedCount++;
      } catch {
        // Still offline, or the replayed request failed again — put it back
        // at the front and stop so ordering is preserved and we don't spin
        // through the remaining queue while connectivity is still down.
        await queue.unshiftRequest(entry);
        break;
      }
    }
    if (syncedCount > 0) {
      const clients = await self.clients.matchAll({ type: 'window' });
      for (const client of clients) {
        client.postMessage({ type: 'shipflow-sync-complete', count: syncedCount });
      }
    }
  },
});

const mutationStrategy = new NetworkOnly({ plugins: [mutationQueue] });
for (const method of ['POST', 'PUT', 'PATCH', 'DELETE'] as const) {
  registerRoute(({ url }) => url.pathname.startsWith('/api/'), mutationStrategy, method);
}

// Allow the client to force activation of a waiting worker (used by the
// manual "Update available" path if autoUpdate registration is ever swapped
// for a prompt-based one in the future).
self.addEventListener('message', (event) => {
  if (event.data?.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});

// ─── Web Push notifications (S59) ──────────────────────────────────────────
// Payload is a flat JSON object `{ title, body, url }` — see
// `WebPushNotificationService.buildPayload` server-side. `url` is the
// in-app path to focus/open on click (e.g. `/tasks/42`).
interface PushNotificationPayload {
  title?: string;
  body?: string;
  url?: string;
}

self.addEventListener('push', (event: PushEvent) => {
  let payload: PushNotificationPayload = {};
  try {
    payload = event.data ? (event.data.json() as PushNotificationPayload) : {};
  } catch {
    // Not JSON (shouldn't happen with our own backend, but don't crash the
    // worker over a malformed payload) — fall back to a plain-text body.
    payload = { body: event.data ? event.data.text() : undefined };
  }

  const title = payload.title || 'ShipFlow';
  const url = payload.url || '/';

  event.waitUntil(
    self.registration.showNotification(title, {
      body: payload.body,
      icon: '/android-chrome-192x192.png',
      badge: '/android-chrome-192x192.png',
      data: { url },
    })
  );
});

// Clicking a notification closes it and focuses an already-open ShipFlow tab
// at that URL, or opens a new one — standard PWA push pattern (MDN:
// ServiceWorkerGlobalScope: notificationclick event).
self.addEventListener('notificationclick', (event: NotificationEvent) => {
  event.notification.close();
  const url: string = event.notification.data?.url || '/';

  event.waitUntil(
    (async () => {
      const targetUrl = new URL(url, self.location.origin);
      const windowClients = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });

      for (const client of windowClients) {
        const clientUrl = new URL(client.url);
        if (clientUrl.pathname === targetUrl.pathname && 'focus' in client) {
          return client.focus();
        }
      }

      return self.clients.openWindow(targetUrl.href);
    })()
  );
});
