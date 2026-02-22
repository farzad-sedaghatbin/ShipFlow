# Export Data

## Cycle Summary Export
ShipFlow generates AI-powered cycle narratives summarizing what the team accomplished.

### How to Export a Cycle Summary
1. Navigate to the **Dashboard** or a specific **Cycle** view
2. Find the **Cycle Summary** panel
3. Click the **Download** icon (or "Export to Markdown")
4. A `.md` file is generated with the structured summary
5. Use this in company wikis, emails, or status updates

## Data Portability
All your data belongs to you. Ways to extract data:
- **Public REST API** — Bulk export pitches, cycles, tasks in JSON format
- **Database Access** — If self-hosting, use `pg_dump` for full PostgreSQL backups
- **CSV Export** — Planned for a future release

# Webhooks

## Outgoing Webhooks
ShipFlow can send event notifications to external systems.

### How to Set Up Outgoing Webhooks
1. Go to **Organization Settings** > **Integrations**
2. Click **Add Webhook**
3. Enter the target URL (e.g., a Slack incoming webhook endpoint)
4. Select which events to trigger on (pitch created, cycle started, scope stagnation, etc.)
5. Save and test with a ping

### Supported Outgoing Events
- Pitch created / updated / approved / rejected
- Cycle phase changed
- Circuit breaker triggered (scope stagnation)
- Retrospective completed
- Test case failed

### Pluggable Notification Providers
Outgoing notifications use a `NotificationProvider` interface. Slack ships as the built-in provider, but you can add Discord, PagerDuty, or any other service by implementing the interface.

## Incoming Webhooks
External services can push events into ShipFlow via a **generic, vendor-agnostic inbound endpoint**.

### Generic Inbound Webhook Endpoint
ShipFlow exposes `POST /api/inbound/{provider}` — a single endpoint that accepts events from any external service.

**How It Works:**
1. An external service (Intercom, Zendesk, PagerDuty, etc.) sends a POST to `/api/inbound/{provider}`
2. ShipFlow's **InboundWebhookRouter** looks up the handler for that provider
3. The handler validates the request signature (HMAC, shared secret, etc.)
4. The handler maps the payload to a ShipFlow action (create bug, create task, etc.)
5. The controller returns an appropriate HTTP response (200 OK, 401 Invalid Signature, 404 Unknown Provider)

### Supported Event Type Headers
The endpoint auto-detects the event type from common header conventions:
- `X-Event-Type` (generic)
- `X-GitHub-Event` (GitHub)
- `X-Intercom-Event` (Intercom)
- `X-Hook-Event` (generic hook systems)
- `X-PagerDuty-Event` (PagerDuty)
- `X-GitLab-Event` (GitLab)
- `X-Linear-Event` (Linear)

### Listing Active Providers
Call `GET /api/inbound` to see which inbound providers are currently active and accepting events.

### Pre-Built Integrations
- **GitHub** — Commit and PR updates linked to pitches (via dedicated webhook controller)
- **Slack** — Create pitches or log notes from Slack commands

### Adding a Custom Inbound Provider
Developers can add a new provider by implementing the `InboundWebhookHandler` interface as a Spring `@Component`. The router auto-discovers it — zero changes to existing code.

# Public API

## Overview
ShipFlow provides a REST API documented via OpenAPI (Swagger) specification.

### How to Access the API
1. Navigate to `/swagger-ui.html` on your ShipFlow instance
2. Browse endpoints grouped by feature (Cycles, Pitches, Tasks, etc.)
3. Try out endpoints directly from the Swagger UI

### Authentication
1. Go to **User Settings** > **API Tokens**
2. Click **Generate Personal Access Token**
3. Copy the token (it's shown only once)
4. Use it in API requests: `Authorization: Bearer <your-token>`

### Generating API Clients
1. Download the OpenAPI spec from `/v3/api-docs`
2. Use tools like `openapi-generator` to generate client libraries in your language
3. Example: `npx openapi-typescript-codegen --input http://localhost:8080/v3/api-docs --output ./api-client`

# MCP Server Setup

## What is MCP?
Model Context Protocol (MCP) servers provide external real-world context to ShipFlow's AI features (like Wise Architecture).

## Supported MCP Integrations
### GitHub MCP
Connects ShipFlow to your GitHub repositories:
1. Go to **Organization Settings** > **Integrations** > **GitHub**
2. Install the ShipFlow GitHub App or configure OAuth
3. Select which repositories to connect
4. ShipFlow AI can now reference your codebase when generating architecture suggestions

### Figma MCP
Connects ShipFlow to your Figma designs:
1. Go to **Organization Settings** > **Integrations** > **Figma**
2. Authenticate with your Figma account
3. Link Figma projects to ShipFlow pitches
4. ShipFlow AI can reference design files when analyzing pitches

## How MCP Helps
- **Wise Architecture** uses MCP data to provide architecture recommendations grounded in your actual codebase and designs
- **AI Risk Advisor** uses MCP context to identify technical risks based on real code complexity
