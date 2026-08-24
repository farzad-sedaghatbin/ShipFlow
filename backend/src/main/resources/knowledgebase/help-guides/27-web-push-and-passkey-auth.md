# Web Push Notifications & Passkey Sign-In

Get native push notifications, and sign in without a password using Face ID, Touch ID, Windows Hello, or a security key.

## Web Push Notifications

### Turning It On

Go to your **Profile** page and toggle **Push Notifications** on. Your browser will ask for permission — allow it. You'll get a native OS notification for mentions, task assignments, and cycle events, even when ShipFlow isn't open in a browser tab.

If the toggle isn't available, the server hasn't been configured with Web Push keys yet (see below for self-hosters) — ask your admin.

### What You'll Get Notified About

The same events that already trigger an in-app notification or email (task assignment, @mention, cycle start/end, etc.) now also fire a push notification if you've turned it on. Clicking the notification opens ShipFlow to the relevant page.

### Turning It Off

Toggle it off from your Profile page. This removes your browser's subscription both locally and on the server — you won't be notified from that browser anymore, but the setting doesn't affect other devices you've enabled it on separately.

### For Self-Hosters

Generate a VAPID keypair with `npx web-push generate-vapid-keys` and set `app.push.vapid.public-key`, `app.push.vapid.private-key`, and `app.push.vapid.subject` (a `mailto:` contact address) in your backend configuration. Leave them blank to keep push notifications disabled — the feature safely no-ops with no other configuration changes required.

## Passkey Sign-In

### Post-Login Setup Prompt

If you sign in with your password and don't have a passkey registered yet, you'll see a one-time prompt right after login offering to set one up on the spot — no need to know it's tucked away in Profile settings. Choose "Not now" to dismiss it (it won't ask again), or "Set up passkey" to register one immediately using the same flow described below.

### Registering a Passkey

On your **Profile** page, find the **Passkeys** card and click **Add a Passkey**. Give it a name (e.g. "MacBook Touch ID") and follow your device's prompt — Face ID, Touch ID, Windows Hello, or a security key, whatever your device offers. You can register more than one passkey (e.g. one per device).

### Signing In with a Passkey

On supported browsers, just click into the **Username** field on the login page — your browser offers your passkey right there in the autofill suggestions, and picking it triggers the same biometric/security-key check used during registration, with no other clicks and no typing needed. If your browser doesn't support this yet (or you'd rather do it explicitly), click **Sign in with a passkey**, type your username, and click through instead — both paths sign you in the same way.

If the automatic prompt doesn't show up for a passkey you registered a while ago, remove it from your Profile page and add it again — a small server-side change made passkeys "discoverable" (so the browser can offer them in autofill), and it only applies going forward. The explicit "Sign in with a passkey" button keeps working for older passkeys either way.

### Managing Your Passkeys

The Passkeys card on your Profile page lists every passkey you've registered (device name, when it was added, when it was last used) and lets you remove one you no longer use — for example, after replacing a device.

### Requirements

Your browser and device need to support WebAuthn (all modern browsers and most devices from the last several years do). If your browser doesn't support it, the passkey options simply don't appear — you can still sign in with your password as usual, and Passkey sign-in never replaces the password login option.
