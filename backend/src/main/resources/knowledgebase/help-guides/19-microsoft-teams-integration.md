# Microsoft Teams Integration

Connect ShipFlow to Microsoft Teams to push notifications — task assignments, completions, cycle starts, pitch updates, and more — into your Teams channels automatically. It uses Teams **incoming webhooks** (with optional Power Automate flows for advanced setups).

Find it under **Admin → Integrations → Microsoft Teams** (`/integrations/teams`). Setup requires **Admin** or **Manager** permissions.

## Step 1 — Get a Teams Webhook URL

In Microsoft Teams:

1. Open the channel where you want notifications.
2. Click **⋯ (More options)** next to the channel name → **Connectors**.
3. Find **Incoming Webhook** → **Configure**.
4. Name it (e.g. "ShipFlow Notifications"), optionally set an image, and click **Create**.
5. **Copy the webhook URL** (it looks like `https://outlook.office.com/webhook/...`) and click **Done**.

*(Advanced: you can instead use a Power Automate "When an HTTP request is received" flow set to accept requests from anyone, and copy its POST URL.)*

## Step 2 — Connect the Tenant in ShipFlow

1. Go to **Admin → Integrations → Microsoft Teams** and click **Configure Tenant**.
2. Fill in:
   - **Tenant Name** — a label for your Teams workspace (e.g. "Acme Corp Teams").
   - **Webhook URL** — paste the URL from Step 1.
   - **Default Channel** — optional fallback channel name.
   - **Enable Integration** — turn on.
3. Click **Save**.

## Step 3 — Choose Which Events Notify Which Channels

1. Open the **Channel Notifications** tab and click **Add Channel**.
2. Set the **Channel Name**, an optional channel-specific **Webhook URL** (leave blank to use the tenant default), and the **Flow Type** (Webhook, Power Automate – Post, or Power Automate – Thread).
3. Toggle the events this channel should receive:
   - Task Assigned, Task Completed, Task Blocked
   - Pitch Shaped
   - Cycle Started, Cycle Cooldown
   - Betting Completed, Sprint Started
4. Click **Save**.

You can add multiple channels — e.g. send task events to `#shipflow-alerts` and pitch/betting events to `#leadership`.

## Step 4 — Send a Test

On the **Tenant Configuration** tab, click the **Send** (paper-plane) icon next to your configuration, enter an optional test message, and click **Send Test**. Check the channel for a formatted notification card.

## Permissions

- **Add / edit** tenant and channel configs, and **send test** notifications: **Admin** or **Manager**.
- **Delete** a tenant configuration: **Admin** only.
- Any signed-in user can view the configuration.

## Troubleshooting

**No notifications arriving.** Confirm the integration is enabled, the channel has the event type toggled on, and the webhook still exists in Teams (webhooks can be deleted or expire).

**401 / "webhook rejected" with Power Automate.** Set the flow's trigger to accept requests from **anyone**, not organization-only, and regenerate the URL if its token expired.

**404 Not Found.** The webhook URL is no longer valid — recreate the incoming webhook in Teams and paste the new URL.

**Check delivery history.** Each configuration keeps a notification history so you can see which messages succeeded or failed.
