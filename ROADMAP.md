# ShipFlow Roadmap

The live roadmap is at **https://shipflow.dev/public-roadmap** (no login required).
It is the canonical, always-current source — this file is a contributor-facing mirror.

## Recently Shipped

| Version | Theme | Date |
|---------|-------|------|
| v1.12.0 | Plugin SDK completion (`shipflow-plugin-api` + Maven archetype), GitLab + Azure DevOps MCP client integrations | August 20, 2026 |
| v1.11.0 | Mobile PWA — offline support, responsive audit, Web Push notifications, passkey (WebAuthn) sign-in | July 28, 2026 |
| v1.10.0 | Wiki References (Pitch/Task links), Drag-and-Drop Task Reordering, Target Release for Standalone Tasks | July 14, 2026 |
| v1.9.0 | Production-Grade Self-Hosting — Helm chart, OpenTelemetry, Grafana, audit export, air-gapped AI | July 1, 2026 |
| v1.8.0 | Custom Fields & Advanced RBAC, Wiki / Docs Space, pluggable Object Storage | June 27, 2026 |
| v1.7.0 | Workflow Automations — trigger/action engine, 20 templates, full UI | June 16, 2026 |
| v1.6.0 | MCP Ecosystem — agentic write tools, plugin SDK, Notion/Confluence clients | June 15, 2026 |
| v1.5.0 | AI Copilot v2 — AI pitch writer, retro summarizer, proactive dashboard insights | June 7, 2026 |
| v1.4.0 | Enterprise Auth & UX Depth — SSO (SAML2/OIDC), SCIM 2.0, roadmap interactivity | June 7, 2026 |
| v1.3.0 | MCP Server Admin & API Keys | June 5, 2026 |
| v1.2.0 | Competitor Migration Tooling (Jira, Linear, CSV) | May 23, 2026 |
| v1.1.0 | Scrum Mode — Sprints, Story Points & Velocity | May 19, 2026 |
| v1.0.0 | Public release — MCP server, audit trail, RBAC, E2E tests | April 2026 |

## Planned

| Version | Theme |
|---------|-------|
| v1.13.0 | Live Presence & Truth — presence indicators, conflict-safe editing, SSE foundation rework, public-page accuracy, public-API hardening |
| v1.14.0 | AI You Can Trust — cycle-summary reliability, RAG hardening, unified AI prompt context |
| v1.15.0 | ShipFlow Agent — a mentionable, assignable AI teammate with scheduled agent loops; runs fully self-hosted / air-gapped via Ollama |
| v1.16.0 | Time to Value — project & pitch templates, public share links, guest access |
| v1.17.0 | Dev Loop — GitHub Issues two-way sync, branch/PR status automation, GitLab repo parity |
| v1.18.0 | Stakeholder View — portfolio rollup dashboards, capacity & timesheet reports, goals (OKR-lite) |
| v1.19.0 | Platform Trust — OAuth 2.0 for MCP, credential encryption at rest, MCP prompt templates & resources |
| v1.20.0 | Knowledge & Docs Graph — GitHub/Confluence/Notion/Drive knowledge ingestion; wiki co-editing (demand-gated) |

> **Note**: v1.13.0 was previously announced as "Collaborative Editing — real-time CRDT co-editing".
> It has been re-scoped: live presence and conflict-safe editing ship in v1.13.0 on the existing
> SSE infrastructure; full CRDT co-editing is deferred to the demand-gated wiki slot in v1.20.0.

For the full session-by-session plan see [CLAUDE.md](./CLAUDE.md).
