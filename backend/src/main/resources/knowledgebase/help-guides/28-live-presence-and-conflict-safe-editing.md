# Live Presence and Conflict-Safe Editing

See who else is looking at a pitch, retro item, or wiki page, and never silently lose your edits to someone else's.

## Where to Find It

On a **Pitch Detail** page (Shape Up section), a **Retro Board**, and a **Wiki** page, a small stack of avatars near the title shows initials for everyone else currently viewing that same item. Your own avatar is never shown in your own stack.

## What It Shows

Each avatar's initials come from the viewer's name; hovering shows the full name. When more than three people are present, the stack shows the first three plus a "+N" badge for the rest. The stack updates live — no need to refresh to see someone else arrive or leave.

## Conflict-Safe Editing

If someone else saves a change to the same pitch, retro item, or wiki page while you're editing it, ShipFlow no longer lets one save silently overwrite the other. When you try to save, you'll see a dialog explaining that the item changed underneath you, with two choices:

- **Keep my changes** — saves your edit anyway, overwriting the other change.
- **Discard mine & reload** — throws away your local edit and loads the latest saved version.

If you're viewing a pitch or wiki page (not actively editing) when someone else saves a change, the page refreshes automatically. If you *are* mid-edit, ShipFlow shows a small notice instead of pulling the page out from under you — the conflict dialog above is what actually resolves it once you try to save.

## Where the Data Comes From

Presence is tracked ephemerally in Redis, keyed by the item you're viewing — it isn't stored permanently, and it disappears a short time after you close the tab or navigate away. Conflict detection uses a version number attached to each pitch, retro item, and wiki page; every save checks that version against the one you last loaded (`POST`/`PUT` requests include an `expectedVersion` field) and returns a conflict instead of silently overwriting when they don't match. Live updates for all of this ride the same real-time notification stream (`GET /api/notifications/stream`) used for the notification bell.

## Requirements

No extra configuration for a single-server deployment. Multi-replica deployments need Redis reachable (see `README.md`'s "Redis for Production Caching" section) for presence and live-refresh to work correctly across pods — without it, each pod only sees the viewers/updates on its own connections.
