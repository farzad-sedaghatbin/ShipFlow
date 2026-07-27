# ShipFlow PWA & Offline Support Guide

Covers the Progressive Web App shell shipped in v1.11.0 (S57): installable app manifest, service worker, caching strategy, and background sync for offline writes. Read this before touching `frontend/src/sw.ts`, `frontend/vite.config.mts`'s `VitePWA` block, or anything under `frontend/src/lib/pwa.ts`.

## Why `injectManifest`, not `generateSW`

`vite-plugin-pwa` offers two strategies:

- `generateSW` — you declare `runtimeCaching` rules and the plugin writes the whole service worker for you. No custom code allowed.
- `injectManifest` — you hand-write the service worker (`frontend/src/sw.ts`); the plugin only injects the precache manifest (`self.__WB_MANIFEST`) into it at build time.

ShipFlow uses **`injectManifest`** because the background-sync replay needs a custom `onSync` callback that `postMessage`s open tabs once queued offline writes actually go through (see below) — `generateSW`'s declarative config has no hook for that.

## What's Cached, and How

| Route | Strategy | Why |
|---|---|---|
| Navigations (`/pitches/42`, `/dashboard`, …) | `NetworkFirst`, denylist `/api/`, `/swagger-ui`, `/actuator`, `/mcp` | Hard refresh on a deep link while offline still shows the SPA shell instead of the browser's offline page. |
| `GET /api/**` | `NetworkFirst`, 8s timeout | Fresh data when online; falls back to the last good response when offline so previously-visited pages stay browsable. Not a substitute for real sync — just keeps last-seen state visible. |
| Images / fonts | `CacheFirst` | Rarely change; safe to serve from cache indefinitely (bounded by `ExpirationPlugin`). |
| `POST/PUT/PATCH/DELETE /api/**` | `NetworkOnly` + `BackgroundSyncPlugin` | See below — never served from cache, a queued write must never look like it silently succeeded. |

The precache manifest (`globPatterns` in `vite.config.mts`'s `injectManifest` block) intentionally **excludes PNGs** — `public/icon.png` alone is ~8MB (a marketing-page source image, not an app icon) and precaching it would bloat install time for every user on every deploy. Images still get cached, just lazily via the runtime `CacheFirst` route on first use instead of eagerly at SW install time.

## Background Sync for Offline Writes

When a mutation request to `/api/**` fails (offline), Workbox's `BackgroundSyncPlugin` queues it in IndexedDB (queue name `shipflow-mutations-queue`, 24h retention) and replays it once the browser fires a `sync` event — or, on browsers without the Background Sync API (Safari), the next time a request hits this route while online.

**The default plugin gives no feedback to the page once a queued request finally sends.** ShipFlow's `onSync` callback in `src/sw.ts` replays the queue itself and, once at least one request replays successfully, `postMessage`s every open tab with `{ type: 'shipflow-sync-complete', count }`. `frontend/src/lib/pwa.ts` listens for that message and shows a toast via `i18n.t('pwa.syncComplete', { count })`.

Client-side, `services/api.ts`'s response interceptor distinguishes "queued because offline" from a real network error: if a mutation request fails with no `error.response` **and** `!navigator.onLine`, it shows `GLOBAL_ERROR_MESSAGES.offlineQueued` (info toast) instead of the generic network-error message. This is a hard-coded string like the other `GLOBAL_ERROR_MESSAGES` — the axios interceptor runs outside the i18n-initialized React tree, matching the existing pattern in that file.

**Known limitation:** queued requests replay in FIFO order but there's no conflict resolution beyond whatever the backend's normal optimistic-locking/validation already does — if a later queued write depends on an earlier one that fails to replay, it will still be attempted and may error against stale state.

## `useOnlineStatus` / `OfflineBanner`

`frontend/src/hooks/useOnlineStatus.ts` tracks `navigator.onLine` plus the `online`/`offline` window events. `frontend/src/components/OfflineBanner.tsx` renders a persistent (non-dismissible) bar across the top of `Layout.tsx`'s content column whenever offline — it's not a toast because "you're offline" is ongoing state, not a one-time event.

Note `navigator.onLine` only reflects whether a network interface is up, not whether the ShipFlow API is actually reachable (a captive portal or backend outage can leave it `true`). That's a browser-wide limitation, not specific to this implementation — the service worker's network strategies are what ultimately determine whether a request succeeds.

## Dev Workflow

The Vite dev server's HMR module rewriting doesn't round-trip cleanly through a precaching service worker, so `devOptions.enabled: false` disables the PWA plugin in `npm run dev`. **Verify PWA/offline behavior against a production build instead:**

```bash
cd frontend
npm run build
npm run preview
```

Then use DevTools → Application → Service Workers (or throttle to "Offline" in the Network tab) to exercise the offline paths.

## TypeScript Setup

`src/sw.ts` runs in the `ServiceWorkerGlobalScope`, which conflicts with the app's `DOM` lib (both type `self` differently). It's excluded from the main `tsconfig.json` (`"exclude": ["src/sw.ts"]`) and has its own standalone `tsconfig.sw.json` (`lib: ["ES2020", "WebWorker"]`) for IDE type-checking — run `npx tsc -p tsconfig.sw.json --noEmit` to check it manually. It's intentionally **not** wired in via TS project `references`, since referenced projects require `composite: true`, which in turn requires `noEmit: false` — not worth the friction for a file that's type-checked standalone and bundled independently by `vite-plugin-pwa`'s own build step anyway.

## Extending This

- **New API route that shouldn't be cached** (e.g. a streaming/SSE endpoint): add it to the navigation `denylist` regex list in `src/sw.ts`, or give it its own `registerRoute` with `NetworkOnly` before the generic `GET /api/**` rule.
- **New mutation type that shouldn't be queued offline** (e.g. a one-time action that must never silently "succeed later," like triggering a payment): exclude its path from the `NetworkOnly` + `BackgroundSyncPlugin` route matcher, or add an explicit higher-priority route for it with a plain `NetworkOnly` (no queue plugin) so it fails immediately instead of queuing.
- **Push notifications (S59)**: will add a `push` event listener to `src/sw.ts` and a `notificationclick` handler — this file is the right place for that, reusing the existing precache/routing setup already in place.
