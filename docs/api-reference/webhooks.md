# Webhooks

ShipFlow supports inbound webhooks for receiving events from external services.

## Inbound webhooks

Configure inbound webhooks in Organization Settings → Integrations → Webhooks.

Each webhook endpoint has a unique URL and an optional secret for HMAC signature verification.

### Supported event sources

- GitHub (push, pull_request, issues)
- Custom HTTP POST from any source

### Payload

```http
POST /api/inbound/{provider}
Content-Type: application/json
X-Hub-Signature-256: sha256=<hmac>

{ "event": "...", "payload": { ... } }
```

Where `{provider}` is the configured provider slug (e.g. `github`, `custom`).

::: info Authentication
Inbound webhook endpoints (`/api/inbound/**`) are intentionally unauthenticated. Requests are validated via HMAC signature verification using your configured webhook secret.
:::

## Outbound webhooks

ShipFlow can POST to external URLs when internal events occur. Configure in Organization Settings → Integrations → Outbound Webhooks.

### Supported trigger events

| Event | Payload |
|-------|---------|
| `task.created` | Task DTO |
| `task.status_changed` | Task DTO + old/new status |
| `pitch.status_changed` | Pitch DTO + old/new status |
| `cycle.started` | Cycle DTO |
| `cycle.completed` | Cycle DTO |

### Signature verification

Each outbound request includes `X-ShipFlow-Signature: sha256=<hmac>` computed with your configured webhook secret.
