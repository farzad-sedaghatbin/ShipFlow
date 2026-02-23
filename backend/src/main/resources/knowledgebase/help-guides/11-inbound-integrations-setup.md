# 11 – Connect External Services to ShipFlow (Inbound Integrations)

ShipFlow can **automatically receive events from external services** — like support platforms, incident managers, monitoring tools, or any system that supports webhooks — and turn them into bug reports, tasks, or notifications inside ShipFlow.

---

## How It Works (The Big Picture)

ShipFlow provides a **generic, vendor-agnostic inbound webhook endpoint**. Any external service that can send HTTP POST requests (webhooks) can push events into ShipFlow.

**General Flow:**
1. Something happens in your external service (a customer creates a support ticket, an incident fires, etc.)
2. The external service sends a webhook (HTTP POST) to ShipFlow at `POST /api/inbound/{provider}`
3. ShipFlow's inbound router finds the registered handler for that provider
4. The handler validates the request signature (to ensure it's authentic)
5. The handler maps the external event to a ShipFlow action (e.g., create a bug report)
6. Your team sees the result in ShipFlow — without any manual data entry

---

## Setting Up Any External Service

Everything is configured through the ShipFlow admin UI — no server access or environment variables needed.

### Step 1: Add a Provider in ShipFlow

1. Go to **Integrations → Inbound Webhooks** in the left sidebar
2. Click **"Add Provider"**
3. Enter a provider name (e.g., `intercom`, `zendesk`, `pagerduty`) — this becomes part of the webhook URL
4. Optionally add a display name and description
5. Click **Create Provider**

ShipFlow generates a unique webhook URL for this provider (shown in the table).

### Step 2: Copy the Webhook URL

After creating the provider, copy the generated webhook URL from the table. It follows this pattern:

```
https://YOUR-SHIPFLOW-DOMAIN/api/inbound/{provider-name}
```

### Step 3: Configure the External Service

In your external service's webhook or integration settings:

1. **Add a webhook endpoint** — paste the ShipFlow webhook URL you copied
2. **Copy the shared secret** — the external service usually provides a secret key for verifying requests
3. **Select which events to send** — choose the event types you want to forward to ShipFlow (e.g., "ticket created", "conversation created", "incident triggered")
4. **Save**

### Step 4: Enter the Shared Secret in ShipFlow

1. Go back to **Integrations → Inbound Webhooks**
2. Click **Edit** on the provider you just created
3. Paste the **shared secret** from the external service
4. Set the correct **signature header** (e.g., `X-Hub-Signature-256`) — check the external service docs
5. Choose the matching **HMAC algorithm** (usually `HmacSHA256`)
6. Click **Save**

### Step 5: Verify the Connection

1. Most external services have a **"Send test notification"** button — use it
2. Check your ShipFlow Bug Reports (or Tasks) page for the test event
3. The provider should appear as active in the **Integrations → Inbound Webhooks** page

---

## Managing Providers

All provider management is done in **Integrations → Inbound Webhooks**:

| Action | How |
|--------|-----|
| **Enable/disable** | Toggle the switch in the provider table |
| **Edit settings** | Click "Edit" to update secret, header, or algorithm |
| **Delete** | Click the delete icon (external services will start receiving errors) |
| **View webhook URL** | Shown in the table — click the copy button |

---

## Common External Services You Can Connect

ShipFlow's inbound webhook system works with **any** service that supports outgoing webhooks. Common examples:

| Service | What It Can Do | Provider Name |
|---------|---------------|--------------|
| **Intercom** | Customer conversations → bug reports | `intercom` |
| **Zendesk** | Support tickets → bug reports | `zendesk` |
| **PagerDuty** | Incidents → bug reports | `pagerduty` |
| **Jira** | Issue updates → task sync | `jira` |
| **Linear** | Issues → tasks or bugs | `linear` |
| **GitLab** | CI/CD events → release tracking | `gitlab` |
| **Custom / Internal** | Any event your system sends | `{your-name}` |

---

## Supported Event Type Headers

ShipFlow automatically detects the event type from standard headers used by popular services:

| Header | Service |
|--------|---------|
| `X-Event-Type` | Generic / custom |
| `X-GitHub-Event` | GitHub |
| `X-Intercom-Event` | Intercom |
| `X-Hook-Event` | Generic hook systems |
| `X-PagerDuty-Event` | PagerDuty |
| `X-GitLab-Event` | GitLab |
| `X-Linear-Event` | Linear |

If none of these headers are present, the event type defaults to `unknown`.

---

## Viewing Incoming Events

Bug reports and tasks created by inbound integrations can be filtered in ShipFlow:

1. Go to **Bug Reports** or **Tasks**
2. Filter by tags to see events from a specific provider (e.g., filter by tag `customer-reported`)
3. Each auto-created item includes the source information and a reference back to the external system

---

## Security

All inbound webhooks are verified using **signature validation**:
- ShipFlow validates every incoming request using the shared secret and HMAC algorithm configured in the admin UI
- Requests with invalid or missing signatures are rejected with HTTP 401
- Requests to unknown/unregistered providers return HTTP 404
- Requests to disabled providers return HTTP 503

---

## Troubleshooting

### "I configured a webhook but nothing happens in ShipFlow"

1. **Is the provider active?** Go to **Integrations → Inbound Webhooks** — is the provider toggle enabled?
2. **Is the webhook secret correct?** The secret in ShipFlow must exactly match the one configured in the external service.
3. **Is the signature header correct?** Make sure the signature header name matches what the external service sends (check their docs).
4. **Is ShipFlow reachable?** The webhook URL must be publicly accessible from the internet (or from the external service's network).
5. **Check the external service's webhook logs** — most services show delivery status, response codes, and retry attempts.

### "Events arrive but the data looks wrong"

- The provider handler is responsible for mapping external event fields to ShipFlow fields. Check with your administrator that the handler is configured correctly.

---

## FAQ

**Q: How do I connect an external service to ShipFlow?**
A: Go to Integrations → Inbound Webhooks, add a new provider, copy the generated webhook URL, paste it in your external service, and enter the shared secret. Events will start flowing in immediately.

**Q: Which services can I connect?**
A: Any service that supports outgoing webhooks. Add any provider name in the admin UI and configure its webhook secret.

**Q: Is the connection secure?**
A: Yes. ShipFlow validates every incoming webhook using HMAC signature verification with the shared secret you configure in the admin UI. Invalid or tampered requests are rejected automatically.

**Q: Do I need to write code or access the server?**
A: No. Everything is configured through the ShipFlow admin UI at Integrations → Inbound Webhooks. No environment variables, no server restarts, no code changes needed.

**Q: Can I connect multiple external services at the same time?**
A: Yes. Each provider operates independently. You can have Intercom, Zendesk, PagerDuty, and custom services all active simultaneously.

**Q: What happens if the same event is sent twice?**
A: This depends on the provider handler. Handlers can implement deduplication based on event IDs, but this is provider-specific.

