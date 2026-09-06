# Sign-in Activity (Authentication Audit)

## What it is
A record of every attempt to sign in to ShipFlow — successful or not — showing
who tried, from which IP address and country, on what device, and what
happened. Administrators can review it from **Organization Settings →
Sign-in Activity**.

## Why it exists
Without it, there is no way to answer "who signed in and changed this?" Account
changes leave a timestamp but no attribution: Hibernate Envers does not audit
the users table, and logins were previously recorded nowhere at all.

## What gets recorded
Each entry captures:

- **When** — the exact time of the attempt
- **User** — the username *as it was typed*, even if no such account exists
- **Event** — signed in, sign-in failed, passkey added, password changed, and so on
- **Outcome** — successful or failed, with the reason for failures
- **IP address** — the real client address, and the country when available
- **Device** — a readable summary such as "Chrome on macOS" or "Safari on iPhone",
  with the full browser identification available on hover

Failed attempts against usernames that do not exist are recorded too. Those are
often the most informative entries — a run of them from one address is what a
password-guessing attempt looks like.

## How to use it
1. Go to **Organization Settings → Sign-in Activity** (administrators only).
2. Filter by username, IP address, or outcome.
3. Use the **Failed** outcome filter to spot repeated unsuccessful attempts.

Typical questions it answers:

- *Did anyone sign in to this account besides me?* — filter by username and scan
  the IP and device columns for anything unfamiliar.
- *Is someone trying to guess a password?* — filter to Failed and look for many
  attempts from one address in a short window.
- *Which device did that come from?* — the Device column, with the full browser
  identification on hover.

## Things worth knowing
- **The device summary is a best guess.** Browsers deliberately impersonate one
  another in their identification strings. The full raw value is always stored
  and shown on hover, so nothing is lost when the summary is imprecise.
- **The country requires Cloudflare.** It comes from a header Cloudflare adds;
  instances not behind Cloudflare will show no country.
- **The IP is the real visitor's**, not the proxy's, on correctly configured
  deployments. If your instance sits behind a proxy or CDN, set
  `app.rate-limit.trusted-proxies` to that proxy's address — otherwise every
  entry will show the proxy instead of the visitor.
- **The log is append-only.** Entries are never edited or removed by the
  application.
- **Administrators only.** The log contains IP addresses and device details for
  every account, so it is not visible to other roles.

## Related
- Passkeys and sign-in methods: see the Web Push & Passkey Authentication guide.
- Exporting change history for tasks, bugs and pitches: see the Audit Export guide.
