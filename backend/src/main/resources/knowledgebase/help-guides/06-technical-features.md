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

## Incoming Webhooks (Inbound Integrations)
External services can automatically send events to ShipFlow via a **generic, vendor-agnostic inbound webhook endpoint** — turning support tickets, incidents, or any external event into bug reports or tasks without manual data entry.

> **Looking for setup instructions?** See the full guide: **"11 – Connect External Services to ShipFlow (Inbound Integrations)"** for step-by-step instructions on connecting any external service.

### Quick Summary
- Any service that supports outgoing webhooks can push events into ShipFlow
- The inbound endpoint is `POST /api/inbound/{provider}` — provider-agnostic by design
- Each provider handler validates signatures, maps payloads, and creates ShipFlow items automatically
- Configuration is done through the admin UI at **Integrations → Inbound Webhooks** — no server access needed

### How Inbound Integrations Work
1. Something happens in your external service (new ticket, incident, conversation, etc.)
2. The external service sends the event as a webhook to `POST /api/inbound/{provider}`
3. ShipFlow validates the signature, maps the event to a ShipFlow action (create bug report, task, etc.)
4. The result appears in ShipFlow — with source details and a reference back to the external system

### Connecting a Service
The general pattern for **any** external service:
1. Go to **Integrations → Inbound Webhooks** and click "Add Provider"
2. Copy the generated webhook URL and paste it in the external service's webhook settings
3. Enter the shared secret from the external service in ShipFlow's provider settings
4. The provider is active immediately — check the Inbound Webhooks page to verify

### Managing Providers
Go to **Integrations → Inbound Webhooks** to view, enable/disable, edit, or delete inbound webhook providers.

### For Developers: Adding a Custom Provider
Developers can add a new provider by implementing the `InboundWebhookHandler` interface as a Spring `@Component`. The router auto-discovers it — zero changes to existing code. See the developer documentation for details.

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
