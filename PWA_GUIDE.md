# ShipFlow PWA & Offline Support Guide

Covers the Progressive Web App shell shipped in v1.11.0: installable app manifest, service worker, caching strategy, and background sync for offline writes (S57), plus Web Push notification delivery (S59). Read this before touching `frontend/src/sw.ts`, `frontend/vite.config.mts`'s `VitePWA` block, or anything under `frontend/src/lib/pwa.ts` or `frontend/src/services/pushService.ts`.

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
## Web Push (S59)

`src/sw.ts` has two additional listeners, additive to everything above:

- `push` — parses the backend's JSON payload (`{title, body, url}`, built by `WebPushNotificationService.buildPayload` server-side) and calls `self.registration.showNotification(...)`. Falls back to a plain-text body if the payload isn't JSON (shouldn't happen with our own backend, but a malformed payload must never crash the worker).
- `notificationclick` — closes the notification and either focuses an already-open tab at the payload's `url` or opens a new one (standard PWA pattern; see MDN's `ServiceWorkerGlobalScope: notificationclick event` page if extending this).

`frontend/src/services/pushService.ts` orchestrates the subscribe/unsubscribe flow against `navigator.serviceWorker.ready` (the same registration `lib/pwa.ts` set up) and the browser's `PushManager`.

**Encoding gotcha — base64url, not plain base64.** The Push API's `applicationServerKey` (the VAPID public key) and a subscription's `p256dh`/`auth` encryption keys are all **base64url**-encoded, not standard base64 — confirmed by decompiling the backend's `nl.martijndwars:web-push` library (`Utils.loadPublicKey` and `Notification`'s string constructor both call `Base64.getUrlDecoder()`). A naive `atob()`/`btoa()` implementation will throw on a real VAPID key's `-`/`_` characters, or worse, silently produce keys the backend can't decode. `pushService.ts` handles this two ways:
- Decodes the fetched VAPID public key with a url-safe-aware decoder (mirrors `lib/webauthn.ts`'s `base64urlToBuffer`, kept as a separate local copy so this module doesn't depend on the WebAuthn one).
- Reads the subscription's own `keys.p256dh`/`keys.auth` via `PushSubscription.toJSON()` — a browser-native method that already base64url-encodes them per spec — instead of hand-building base64 from `subscription.getKey(...)` + `btoa`.

If you ever touch this encoding again: WebAuthn (`lib/webauthn.ts`) and Web Push (`services/pushService.ts`) both happen to use base64url, but for unrelated reasons (their respective specs), and the two modules intentionally don't share a helper — don't assume "whichever encoding the other module uses" without checking the actual spec/library for the field you're touching.

**Configuration**: `app.push.vapid.public-key`/`private-key`/`subject` (backend `application.properties`, all optional — blank disables the feature via `NoOpPushNotificationService`). Generate a keypair with `npx web-push generate-vapid-keys`.

## Passkey Login — Conditional UI / Autofill

The username-first passkey flow from S59 (`PasskeyController`/`PasskeyService`, `Login.tsx`'s "Sign in with passkey" button) required an explicit click, then typing a username, then a second click before the biometric prompt appeared. A second, additive login mode now runs alongside it so a supporting browser can offer a passkey suggestion directly in the username field's autofill dropdown, with no button click at all:

- **Frontend** (`Login.tsx`): on mount, feature-detects `PublicKeyCredential.isConditionalMediationAvailable()` (`lib/webauthn.ts`) and, if supported, fires a passive `navigator.credentials.get({mediation: 'conditional'})` — this resolves only if/when the user picks a suggestion, so it's safe to leave pending indefinitely. The primary username `<input>` needs `autoComplete="username webauthn"` for the browser to associate the field with the request. An `AbortController` (`conditionalAbortRef`) is aborted both on unmount and whenever the password form or the manual passkey form starts an explicit login, so at most one WebAuthn ceremony is ever in flight.
- **Backend**: since there's no username yet, `POST /api/auth/passkeys/login/options/discoverable` (`PasskeyService.beginDiscoverableLogin`) issues a challenge with no associated user and an empty `allowCredentials` — the browser resolves candidates from its own discoverable-credential store for the RP. `PasskeyService.finishLogin` detects this case (blank `username` in the verify request) and instead (1) locates the pending challenge by its own random value, extracted from the assertion's `clientDataJSON` via webauthn4j's `CollectedClientDataConverter` rather than by username hint, and (2) resolves the account from the authenticator-returned `userHandle` (the same id embedded as `PublicKeyCredentialUserEntity.id` at registration) instead of a challenge→user lookup. The existing username-first code path is untouched.
- **Discoverable credentials**: only a passkey created with a discoverable `residentKey` can appear in autofill at all. `beginRegistration` changed from `residentKey: "discouraged"` to `"preferred"` — modern platform authenticators (Touch ID, Windows Hello, Android biometrics) create a discoverable credential by default when "preferred", while authenticators without resident-key storage still succeed at registration, just without autofill support. **Passkeys registered before this change are not discoverable** and won't appear in the autofill suggestion list — they keep working fine through the explicit username-first flow, but need re-registering (delete + re-add on the Profile page) to get the automatic prompt.
- Not all browsers implement conditional mediation (Firefox did not, as of this writing) — `isConditionalMediationAvailable()` is why the explicit button flow is kept as a permanent fallback, not just a migration bridge.
