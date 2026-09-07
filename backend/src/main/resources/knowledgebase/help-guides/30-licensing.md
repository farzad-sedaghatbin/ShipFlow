# Licensing & Community Edition

## What it is
ShipFlow is open-core. This public repository — everything you can build, run,
and self-host from source — is the **Community Edition**. Uploading a signed
**licence file** unlocks commercial features and lifts two Community limits,
from **Organization Settings → Licence**.

## Why it exists
ShipFlow is free and open source, and stays that way. Licensing funds ongoing
development without changing that: the Community Edition is fully functional
project management for Shape Up, Scrum, and Kanban teams, with two soft caps
that only matter once a team outgrows a small, informal setup.

## What Community Edition includes
Everything in this repository — every methodology, every AI feature, every
integration — with two limits:

- **Up to 10 active users.** Creating an 11th user, or reactivating a
  deactivated one past that count, is blocked. Existing users are never
  deactivated to enforce this — the limit only stops you from going *over* it,
  it never takes anything away.
- **Up to 5 enabled workflow automation rules.** A 6th rule can be created, but
  only disabled; enabling it (or a 6th) is blocked the same way.

Both limits are enforced only while no licence is installed, or an installed
licence has expired outside its grace period (see below).

## Licence status
A licence's status shows as one of four states:

- **Community Edition** — no licence installed. The two limits above apply.
- **Valid** — a correctly-signed, unexpired licence. Seats rise to
  `licensed seats × 1.1` (a small buffer over the purchased count) and the
  automation limit is lifted entirely.
- **Grace period** — a valid licence that expired less than 30 days ago.
  Behaves exactly like Valid, but the Licence tab and a dismissible banner
  nag about renewing.
- **Expired** — a valid licence expired 30 or more days ago. Community
  Edition limits apply again, same as no licence at all.

A licence that is malformed, tampered with, or signed with the wrong key
never causes an error — it is simply treated as no licence at all
(Community Edition), with the reason logged for an administrator to check.

## How to use it
1. Go to **Organization Settings → Licence** (visible to everyone; upload and
   remove are administrator-only).
2. To install a licence, either choose the licence file or paste its full
   contents into the text box, then select **Upload**.
3. The tab immediately shows the refreshed status: edition, licensee, seats
   used of total, unlocked features, and the issued / expires / support-until
   dates.
4. To remove an installed licence — for example, to test Community Edition
   behavior, or because it was uploaded to the wrong instance — select
   **Remove licence** and confirm.

## Self-hosting
A licence file only verifies if the instance's public key is configured:

- `app.license.public-key` (env `APP_LICENSE_PUBLIC_KEY`) — the base64-encoded
  public key that verifies a licence's signature. Left unset, no licence can
  ever verify, and the instance always runs as Community Edition — this is
  the correct, safe default for a self-hosted instance that has not
  purchased a licence.
- `app.license.file` (env `APP_LICENSE_FILE`, default `./config/shipflow.license`)
  — an optional path to a licence file on disk, used only when no licence has
  been uploaded through the UI. Useful for provisioning a licence as part of
  a deployment (a mounted volume, a baked-in file) rather than clicking
  through the UI after every deploy. An uploaded licence always takes
  priority over this file.

Licence status is checked once at startup and once a day afterward, so a
renewed licence uploaded near an expiry date takes effect automatically
without a restart.

## Things worth knowing
- **A licence never causes a startup failure.** Whatever is wrong with a
  licence file — missing, expired, corrupted, wrong key — the app starts and
  runs as Community Edition. Licensing degrades gracefully by design.
- **The Licence tab's status section is visible to every role**, so any team
  member can see how many seats are in use; only uploading and removing a
  licence require the administrator role.
- **The seat and automation counts never retroactively remove anything.**
  Going over a limit only blocks the *next* creation, reactivation, or
  enable action — nothing already in place is touched.
- **No feature in this release is gated behind a licence.** The licensing
  plumbing (`@RequiresLicensedFeature`) ships ready for a future commercial
  feature to use; every capability described elsewhere in this knowledge
  base is available in the Community Edition today.

## Related
- Self-hosting configuration in general: see `ENVIRONMENT_SETUP.md` in the
  repository root.
- Managing user accounts and roles: see the Teams, Projects & Settings guide.
- Enabling and disabling workflow automation rules: see the Automations
  guide.
