# Offline Support & Installable App (PWA)

ShipFlow can be installed like a native app and keeps working — for already-visited pages — when your connection drops.

## Installing ShipFlow

On desktop Chrome/Edge, click the install icon in the address bar (or the browser menu's "Install ShipFlow…" entry). On mobile, use your browser's "Add to Home Screen" option. Once installed, ShipFlow opens in its own window without browser chrome, using the icon and name from the app's manifest.

## What Works Offline

- **Pages you've already opened** stay browsable — the last data you loaded for a page (dashboard, backlog, a pitch, a hill chart, etc.) is served from a local cache while you're offline.
- **The app shell always loads.** Even a hard refresh on a deep link (e.g. `/pitches/42`) while offline shows the app instead of the browser's "no internet" page.
- **Changes you make while offline are queued**, not dropped. An offline banner appears at the top of the page; when you save something (a comment, a status change, a new task, etc.), you'll see "this change will be sent automatically once you're back online" instead of a generic error. The change is stored locally and replayed automatically as soon as your connection returns — no need to redo it.

## What Doesn't Work Offline

- Anything that requires a live answer from the server or an AI provider (search, AI features, real-time notifications) needs a connection.
- Data you've never loaded before (e.g. a pitch you haven't opened yet) can't be fetched offline — there's nothing cached to show.
- Queued changes replay in the order they were made; if a later change conflicts with one still queued ahead of it, normal conflict handling (e.g. optimistic-locking errors) applies once it reaches the server.

## Notifications You Might See

| Message | Meaning |
|---|---|
| "ShipFlow is ready to work offline." | Shown once, after the app has finished caching itself for offline use. |
| "You're offline — this change will be sent automatically once you're back online." | A save failed only because you're offline — it's queued, not lost. |
| "N offline change(s) synced." | Your queued changes from earlier just went through. |

## For Self-Hosters

No extra configuration is required — the service worker and app manifest are built into the frontend bundle. See `PWA_GUIDE.md` in the repository for the technical implementation (service worker, caching strategy, background sync) if you're extending this feature.
