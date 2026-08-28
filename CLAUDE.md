# ShipFlow — Claude Code Guide

This file is the entry point for Claude Code (and any AI coding assistant) working on this repository.
Read it before touching any code.

---

## Current Milestone: v1.13.0 — "Live Presence & Truth"

**Current version**: v1.12.1 (released 2026-08-28)
**All PRs target**: `main` branch

> **Note on versioning**: v1.10.0 (wiki-linking, drag-and-drop task reordering, target-release for standalone tasks) was fully built and merged on 2026-07-14 but never actually tagged/released — work continued straight into the "Mobile PWA" sessions without cutting a release in between. v1.11.0's release absorbed that gap: the git tag jumps directly from v1.9.1 to v1.11.0. See `CHANGELOG.md`'s `[1.11.0]` entry for the full note. All three Mobile PWA sessions (S57 PWA shell/offline, S58 responsive audit, S59 Web Push + passkey auth) shipped as independent PRs — see the v1.11.0 session map below.

ShipFlow is **methodology-agnostic** — supports Shape Up + Kanban + Scrum (v1.1.0 shipped). Competitor migration tooling (v1.2.0) shipped. MCP Server Admin & API Keys (v1.3.0) shipped. Enterprise Auth & UX Depth (v1.4.0) shipped. AI Copilot v2 — AI Pitch Writer, Retrospective Summarizer, Proactive Dashboard Insights (v1.5.0) shipped. MCP Ecosystem (v1.6.0) shipped. Workflow Automations — trigger/action engine, 20 templates, full UI (v1.7.0) shipped. Custom Fields & Advanced RBAC, Wiki / Docs Space, and pluggable Object Storage (v1.8.0) shipped. Production-Grade Self-Hosting — Helm chart, OpenTelemetry, Grafana, audit export, and air-gapped AI mode (v1.9.0) shipped. Wiki references (Pitch/Task links), drag-and-drop task reordering, and target-release for standalone tasks (v1.10.0) shipped. Mobile PWA — offline support, responsive audit, Web Push, passkey auth (v1.11.0) shipped. Plugin SDK completion, GitLab + Azure DevOps MCP client integrations (v1.12.0) shipped. Next: Live Presence & Truth — presence indicators + conflict-safe editing on the existing SSE infrastructure, SSE foundation rework (Redis pub/sub fan-out, multi-tab), public-page accuracy sweep, and public-API hardening (v1.13.0).

> **Roadmap revision (2026-08-23)**: v1.13.0 was previously planned as "Collaborative Editing — real-time CRDT co-editing on pitches and retrospectives". A gap/market/competitor analysis re-scoped it: CRDT co-editing is a from-scratch multi-milestone build (no WebSocket transport, no CRDT library, pitches are plain Markdown textareas), no competitor's 2026 story is co-editing (Linear/Atlassian/Asana are all shipping **AI agents that do work inside the tool**), and adoption — not collaboration depth — is ShipFlow's bottleneck. The replacement v1.13–v1.20 roadmap is in the Future milestones overview below; full CRDT co-editing survives only as a demand-gated wiki-only slot in v1.20.0 (BlockNote's first-party Yjs path). See `ROADMAP.md` and the plan file `~/.claude/plans/i-blive-the-plan-velvety-river.md` for the full rationale.

### v1.3.0 session map

| Session | Task | Status |
|---------|------|--------|
| S31 | MCP server admin toggle + API key management UI — DB-backed runtime toggle, write-tools toggle, API key CRUD with scopes/expiry, admin oversight | ✅ done |

### v1.2.0 session map

| Session | Task | Status |
|---------|------|--------|
| S27 | CSV import backend — `ImportJob` entity, `CsvImportService` (Jira/Linear/Asana/Generic), `ImportController` | ✅ done |
| S28 | CSV import frontend — stepper UI (Upload → Preview → Done), import history page | ✅ done |
| S29 | Linear API import — OAuth2 flow, GraphQL fetch of issues/cycles/projects | ✅ done |
| S30 | Jira API import — Atlassian OAuth 2.0 (3LO), REST API fetch of issues/sprints/epics | ✅ done |

### v0.8.0 session map (sessions S01–S13)

| Session | Task | Status |
|---------|------|--------|
| S01 | Public roadmap page (`/roadmap`) | ✅ done |
| S02 | Demo seed data refresh | ✅ done |
| S03 | Version alignment (pom.xml 0.8.0, java 21, CORS fix) | ✅ done |
| S04 | Spring Boot upgrade 3.2.1 → 3.4.x | ✅ done |
| S05 | Rate limiting (Bucket4j) + CSP headers + startup secret validation | ✅ done |
| S06 | Docker GHCR CI/CD + React.lazy code splitting | ✅ done |
| S07 | File attachments on tasks — backend | ✅ done |
| S08 | File attachments on tasks — frontend | ✅ done |
| S09 | Bulk task operations — backend | ✅ done |
| S10 | Bulk task operations — frontend | ✅ done |
| S11 | @mention triggers notification | ✅ done |
| S12 | CSV export for task backlog | ✅ done |
| S13 | Interactive onboarding tour (wire TourContext + driver.js) | ✅ done |
| S13.1 | Q&A / RAG hardening — entity disambiguation, multi-turn memory, cache isolation | ✅ done |

**v0.8.0 released: 2026-04-05** ✅

### v0.9.0 session map (sessions S14–S26)

| Session | Task | Status |
|---------|------|--------|
| S14 | Saved Filter Views — backend | ✅ done |
| S15 | Saved Filter Views — frontend | ✅ done |
| S16 | Real-time notifications via SSE | ✅ done |
| S17 | Email notifications (SMTP) | ✅ done |
| S18 | MCP Phase 2 write tools | ✅ done |
| S19 | Playwright E2E — auth flow | ✅ done |
| S20 | Playwright E2E — project management flow | ✅ done |
| S21 | Playwright E2E — pitch lifecycle | ✅ done |
| S22 | Playwright E2E — hill chart | ✅ done |
| S23 | Playwright E2E — task management | ✅ done |
| S24 | BacklogPage decomposition | ✅ done |
| S25 | OrganizationSettings decomposition | ✅ done |
| S26 | PitchDetail decomposition | ✅ done |

**v0.9.0 released: 2026-04-14** ✅

### v1.4.0 session map

| Session | Task | Status |
|---------|------|--------|
| S32 | SSO backend — Spring Security SAML2 + OIDC, IdentityProvider entity, Flyway migration | ✅ done |
| S33 | SSO frontend — identity provider config UI in Org Settings, SSO enforcement toggle, login page SSO button | ✅ done |
| S34 | SCIM 2.0 user provisioning — backend endpoint + frontend toggle | ✅ done |
| S35 | Roadmap interactivity — drag-to-move/resize Gantt bars, date range validation, progress indicators | ✅ done |
| S36 | UX polish — inline pitch title editing, retrospective templates (Went Well/Improve/Action Items), i18n interpolation sweep | ✅ done |
| S37 | Navigation hardening — deep-link routing for all sidebar routes, keyboard shortcut cheat sheet overlay | ✅ done |

**v1.4.0 released: 2026-06-07** ✅

### v1.5.0 session map

| Session | Task | Status |
|---------|------|--------|
| S38 | AI Pitch Writer — LLM-powered Shape Up pitch draft from one-sentence problem description, pre-fills pitch form | ✅ done |
| S39 | Retrospective Summarizer — AI-generated cycle retro summary, template fallback, RetroSummaryPanel below board | ✅ done |
| S40 | Proactive Dashboard Insights — DashboardInsightsPanel (overdue pitches, at-risk cycles, scope creep, velocity), Redis cache | ✅ done |

**v1.5.0 released: 2026-06-07** ✅

### v1.6.0 session map

| Session | Task | Status |
|---------|------|--------|
| S41 | Agentic MCP write tools — `update_task` + `update_pitch` (PATCH-semantic full-field MCP write tools) | ✅ done |
| S42 | Plugin SDK — Maven archetype, plugin registry, first-party plugin scaffold | ✅ done |
| S43 | Notion / Confluence MCP clients — read design docs and meeting notes into AI context | ✅ done |
| S44 | Rich link preview (OG metadata) — server-rendered `og:title`/`og:description` for task/pitch/cycle URLs so shared links show meaningful previews in Slack, iMessage, etc. | ✅ done |

**v1.6.0 released: 2026-06-15** ✅

### v1.7.0 session map

| Session | Task | Status |
|---------|------|--------|
| S45 | Workflow Automations backend — TriggerType/ActionType enums, WorkflowAutomation/Template/Execution entities, engine, event listener, action dispatcher, 20 SQL-seeded templates | ✅ done |
| S46 | Shape Up triggers & REST API — HILL_CHART_MOVED, APPETITE_EXCEEDED, SCOPE_CREEP_DETECTED, BETTING_TABLE_LOCKED triggers; WorkflowAutomationController (10 endpoints) | ✅ done |
| S47 | Automations UI — AutomationsPage (Rules + History tabs), AutomationRuleForm, AutomationTemplateGallery, AutomationExecutionLog, sidebar nav, i18n | ✅ done |

**v1.7.0 released: 2026-06-16** ✅

### v1.9.0 session map (shipped)

| Session | Task | Status |
|---------|------|--------|
| S54 | Observability backend — `micrometer-registry-prometheus` + `/actuator/prometheus`, OpenTelemetry OTLP tracing (env-gated), native Spring Boot 3.4 structured JSON logging (`LOG_FORMAT`) with traceId/spanId | ✅ done |
| S55 | Helm chart (`charts/shipflow`) + monitoring stack — Deployment/Service/Ingress/ConfigMap/Secret/HPA/ServiceMonitor; `docker-compose.monitoring.yml` with Prometheus + pre-provisioned Grafana dashboard | ✅ done |
| S56 | Audit export — `GET /api/audit/export` (admin-only, CSV/JSON, entity-type + date-range filter) reusing the `csvEscape` pattern; Org-Settings UI | ✅ done |

### v1.11.0 session map (shipped 2026-07-28)

| Session | Task | Status |
|---------|------|--------|
| S57 | PWA shell + offline support — installable web app manifest (via `vite-plugin-pwa`, `injectManifest` strategy), custom service worker (`frontend/src/sw.ts`) with network-first API/navigation caching and Workbox background sync for queued offline writes, offline banner + sync-complete toast. See `PWA_GUIDE.md`. | ✅ done |
| S58 | Responsive-layout audit across all pages (boards, pitches, hill charts, wiki) — fixed real mobile overflow bugs in the Backlog bulk-action toolbar, Wiki page-tree sidebar/modals, a stat grid, and a task-detail action row, plus a shared `AlertDialog` mobile-margin gap found along the way | ✅ done |
| S59 | Web Push notifications (VAPID via `nl.martijndwars:web-push`) + WebAuthn passkey sign-in (`webauthn4j`) — username-first passkey login issuing the same JWT as password login, self-service passkey management, push subscription + preference toggle on Profile. See `PWA_GUIDE.md`. | ✅ done |

### v1.12.0 session map (shipped 2026-08-20)

| Session | Task | Status |
|---------|------|--------|
| S60 | Plugin SDK completion — real `shipflow-plugin-api` Maven module (`plugin-sdk/shipflow-plugin-api`, moved out of `backend`, aggregated via a new root `pom.xml` reactor; `backend` depends on it as a regular module dependency) and a real `shipflow-plugin-archetype` module (`mvn archetype:generate` verified end-to-end against a scratch project). Corrected `plugin-sdk/README.md`'s and the `12-plugins-mcp.md` help guide's inaccurate "drop a JAR into `plugins/`" runtime-loading claim — plugins still compile into the app as Spring beans (no dynamic runtime loading; out of scope). The admin plugin catalog UI (Organization Settings → Plugins) already existed from v1.6.0/S42 and needed no changes. | ✅ done |
| S61 | GitLab MCP client provider — `GitLabMcpConfig` in `McpConfig` (`MCP_GITLAB_ENABLED`/`MCP_GITLAB_SERVER_URL`); `GitLabMcpProvider implements McpClientService` calling GitLab's REST API v4 directly (`{instance}/api/v4/...`, self-hosted-friendly) rather than a JSON-RPC `tools/call` MCP server — GitLab has no equally ubiquitous MCP-server reference implementation to target, unlike GitHub; PAT auth via GitLab's `PRIVATE-TOKEN` header, token stored via the existing org-settings integration-secret convention (`gitlabAccessToken`, write-only). Automatically enforced by `AirGappedModeValidator`/reported by `SystemStatusService` via their existing `List<McpClientService>` injection; not yet wired into `WiseArchitectureService`'s GitHub-entity-coupled repository scanning (follow-up). | ✅ done |
| S62 | Azure DevOps MCP client provider — `AzureDevOpsMcpConfig` + `AzureDevOpsMcpProvider implements McpClientService`, targeting Azure DevOps REST API 7.1 (Git Items API for list/read, optional Code Search extension for search with a client-side-filtered-listing fallback), PAT via HTTP Basic auth (empty username), registered as a plain `@Service` bean picked up by the existing generic `List<McpClientService>` consumers (not wired into Wise Architecture, which is GitHub/Figma-specific) | ✅ done |

### Future milestones overview

| Version | Theme | Sessions |
|---------|-------|----------|
| v1.8.0 ✅ shipped 2026-06-27 | Custom Fields & Advanced RBAC + Wiki / Docs Space + pluggable Object Storage — custom fields on tasks/pitches, project-level permissions, built-in wiki, attachment storage (LOCAL_FS/S3/MinIO) | S48–S53 |
| v1.9.0 ✅ shipped 2026-07-01 | Production-Grade Self-Hosting — Helm chart, OpenTelemetry, Grafana, audit export | S54–S56 |
| v1.10.0 ✅ shipped (untagged; absorbed into v1.11.0) | Wiki references (Pitch/Task links), drag-and-drop task reordering, target-release for standalone tasks — interim session-driven release, not part of the planned milestone sequence | — |
| v1.11.0 ✅ shipped 2026-07-28 | Mobile PWA — offline support, responsive audit, Web Push, passkey auth | S57–S59 |
| v1.12.0 ✅ shipped 2026-08-20 | Plugin SDK completion tooling (`shipflow-plugin-api`/`shipflow-plugin-archetype`), GitLab + Azure DevOps MCP client integrations (originally announced as "Plugin Marketplace" — no marketplace registry was built, only the SDK/archetype) | S60–S62 |
| v1.13.0 (current) | Live Presence & Truth — S63: SSE foundation rework (Redis pub/sub fan-out for `NotificationSseManager`, multi-emitter-per-user, `useNotificationStream` reconnect fix); S64: presence indicators + optimistic-lock conflict dialogs on pitch/retro/wiki + live refresh; S65: public-page truth sweep (stale `futureItem*` cards, `ROADMAP.md`, `COMPETITOR_ANALYSIS.md`) + `/api/v1/public/**` permitAll hardening | S63–S65 |
| v1.14.0 | AI You Can Trust — S66: cycle-summary reliability (no hallucinated/conversational persistence, invalidation); S67: RAG hardening (structured cycle context in Q&A widget, honest confidence, help-guide indexing); S68: unified prompt context (`ProjectSnapshotService` into Risk Advisor/Q&A/Test Gen), per-feature model config, fix dead Create-Selected | S66–S68 |
| v1.15.0 | **ShipFlow Agent** (flagship) — S69: @mentionable agent in comment threads (RAG + structured context, RBAC-scoped agent identity); S70: agent actions via existing MCP write-tool layer with propose→confirm; S71: agent loops (standup digest to Slack/Teams, cycle-health check, betting-table prep) as Workflow Automation action types. Unique wedge: runs fully self-hosted/air-gapped via Ollama | S69–S71 |
| v1.16.0 | Time to Value — S72: project/pitch templates + clone project; S73: tokenized public share links (pitch, hill chart, roadmap; revocable, noindex); S74: guest access (single-project external stakeholders) | S72–S74 |
| v1.17.0 | Dev Loop — S75: GitHub Issues two-way sync; S76: branch/PR status automation (branch→In Progress, PR→In Review, merge→Done); S77: GitLab repo parity (`GitLabRepository` entity, repo picker, Wise Architecture wiring) + Org-Settings tab for GitLab/ADO tokens | S75–S77 |
| v1.18.0 | Stakeholder View — S78: cross-project portfolio rollup + velocity/appetite-accuracy trends; S79: capacity & timesheet rollups over existing `WorkLog` data; S80: goals (OKR-lite) linked to Initiatives/Pitches | S78–S80 |
| v1.19.0 | Platform Trust — S81: OAuth 2.0 for MCP (GAP 11, plan in `MCP_SERVER_MILESTONE.md`); S82: credential encryption at rest (AES-GCM `AttributeConverter` over isolated secret columns); S83: MCP completeness (prompt templates, resource URIs, missing tools) | S81–S83 |
| v1.20.0 | Knowledge & Docs Graph — S84–S85: Knowledge Center providers (GitHub, Confluence, Notion, Google Drive — SPI ready); S86: wiki-only co-editing via BlockNote's first-party Yjs path, **demand-gated** (skip if still no multi-user usage) | S84–S86 |

Dropped / not scheduled: full CRDT co-editing on pitches & retros (no document model, no demand evidence — revisit only after the v1.20 gate), plugin dynamic runtime loading / marketplace registry (sandboxing risk, deliberately out of scope), native app-store mobile app (PWA holds).

Full session prompts (S01–S29 through v1.0.0) are in:
`/Users/farzad/.claude/plans/smooth-shimmying-catmull.md`

### Mandatory end-of-session checklist
Every session must complete ALL of these before creating the PR:

1. Add entry to `CHANGELOG.md` under `[Unreleased]`
2. Update `README.md` if user-visible feature added — **refresh screenshots if UI changed**
3. Update `COMPETITOR_ANALYSIS.md` if this closes a gap vs Linear/Jira/Asana
4. Add highlight card to `frontend/src/pages/ReleaseNotes.tsx`
5. Update relevant `*_GUIDE.md` or `CLAUDE.md` if a new repeatable pattern was introduced
6. Add i18n keys to **both** `en.json` AND `fa.json`
7. Write unit or integration tests (JaCoCo gate: ≥ 80% line coverage). **The full suite must be at 0 failures** — if your change breaks existing tests (e.g. serialization format change, renamed field), fix those tests in the same commit before pushing.
8. Update `SampleDataInitializer.java` with demo data for the new feature
9. **If UI layout changed**: verify onboarding tour step selectors in `frontend/src/contexts/TourContext.tsx` still target correct elements. Update both the selector and the **Step Inventory** table in `TOUR_GUIDE.md`. See `TOUR_GUIDE.md` for the full maintenance contract.
10. **Help & Guides — mandatory for every new user-facing feature**: add or update a guide in `backend/src/main/resources/knowledgebase/help-guides/`. Name it `{NN}-{feature}.md` (next sequential number). The file is auto-loaded by `HelpGuideAIService` at startup — no registration needed. If an existing feature's UI changed, update the relevant existing guide. **A feature without a help guide is incomplete.**
11. **If in-app help guides reference changed UI**: update the relevant help guide content
12. **Keep public pages in sync** — `ReleaseNotes.tsx` (`/releases`) and `PublicRoadmap.tsx` (`/public-roadmap`) must always match each other. Any feature added to one must appear in the other. Version cards, item titles, descriptions, and milestone status ("upcoming" / "in-progress" / "planned") must be identical across both pages. See the [Public Pages Alignment rule](#public-pages-alignment) below.
13. Create PR targeting `main` using `feat/fix/chore/refactor/test/docs` prefix

---

## Public Pages Alignment

> **Rule**: `ReleaseNotes.tsx` and `PublicRoadmap.tsx` are two views of the same truth. They must always be in sync.

### The two pages

| Page | Route | Audience | File |
|------|-------|----------|------|
| Release Notes | `/releases` | Anyone — changelog-style list, newest first | `frontend/src/pages/ReleaseNotes.tsx` |
| Public Roadmap | `/public-roadmap` | Anyone — phase view with "in-progress / planned / future" | `frontend/src/pages/PublicRoadmap.tsx` (i18n keys in `en.json` / `fa.json`) |

### What must match between them

1. **Every version milestone** must appear in both pages (e.g. if v1.0.0-rc1 is a card in ReleaseNotes, it must be a phase in PublicRoadmap and vice-versa).
2. **Item titles** for each milestone must be the same (or differ only in phrasing, never in substance).
3. **Item count** should be the same for a given milestone — don't add an item to one page and forget the other.
4. **Milestone status** must be consistent: if a version is marked `upcoming: true` in ReleaseNotes it must be `'in-progress'` or `'planned'` in PublicRoadmap's `upcomingPhases` array, not in `recentlyShipped`.
5. **Newly shipped versions** move from "upcoming/in-progress" to the "recently shipped" section in PublicRoadmap and lose the `upcoming: true` flag in ReleaseNotes — both changes in the same PR.

### When releasing a new version

Run this sub-checklist **in addition to** the main end-of-session checklist:

- [ ] Move the version card in **ReleaseNotes.tsx**: remove `upcoming: true`, set `date` to the real release date (e.g. `'April 19, 2026'`).
- [ ] Move the version in **PublicRoadmap.tsx**: remove from `upcomingPhases`, add to `recentlyShipped` with all highlights.
- [ ] Update the i18n `shipped*` keys in **`en.json`** and **`fa.json`** with the correct title and item descriptions.
- [ ] Verify the hero badge in ReleaseNotes now shows the new version as "Latest".
- [ ] Verify the "In Progress" card in PublicRoadmap now points to the next milestone.

### When adding a feature mid-milestone

- Add it to the **current milestone's** highlights in both pages (same session, same PR).
- If the feature uses i18n keys (PublicRoadmap), add the corresponding hardcoded text in ReleaseNotes to match.

---

## What is ShipFlow?

ShipFlow is a full-stack project management platform built around the **Shape Up** methodology by Basecamp.
It supports dual project modes (Shape Up + Kanban), AI-powered features, multi-source MCP integrations, and
enterprise-grade RBAC.

- **Live demo**: https://shipflow.dev
- **Stack**: Spring Boot 3.2.1 (Java 21) + React 18 (TypeScript) + PostgreSQL + Redis

---

## Repository Layout

```
shipflow/
├── backend/          # Spring Boot API (Maven)
│   └── src/main/java/com/github/farzadsedaghatbin/shipflow/
│       ├── controller/   # 65 REST controllers
│       ├── service/      # 86 business-logic services
│       ├── entity/       # 63 JPA entities
│       ├── repository/   # 60+ Spring Data repositories
│       ├── dto/          # request/response DTOs per feature
│       ├── config/       # Spring configs (LLM, Redis, Security…)
│       ├── security/     # JWT + RBAC
│       └── service/mcp/  # MCP client providers (GitHub, Figma)
├── frontend/         # React + Vite + TypeScript
│   └── src/
│       ├── pages/        # 65 page components
│       ├── components/   # 108+ reusable components
│       ├── services/     # API client layer
│       ├── hooks/        # Custom React hooks
│       └── contexts/     # React context providers
├── scripts/          # Utility / generation scripts
├── .github/          # CI workflows + PR/issue templates
└── *.md              # Architecture docs (read these first!)
```

---

## Key Architecture Docs

| File | Topic |
|------|-------|
| `WISE_ARCHITECTURE.md` | AI-powered tech advice feature |
| `RAG_ARCHITECTURE.md` | Retrieval-Augmented Generation + vector stores |
| `PROJECT_TYPE_ARCHITECTURE.md` | Shape Up vs Kanban dual-mode |
| `REDIS_CONFIGURATION_GUIDE.md` | Caching strategy |
| `GITHUB_INTEGRATION_GUIDE.md` | GitHub OAuth + webhooks |
| `PERMISSION_MATRIX.md` | RBAC roles and permissions |
| `ENVIRONMENT_SETUP.md` | Local dev setup |
| `PWA_GUIDE.md` | Service worker, offline caching, background sync (v1.11.0 Mobile PWA) |
| `SEO_GUIDE.md` | Public-page indexing — `useSeo` hook, robots policy, generated sitemap, blog posts |
| `MCP_SERVER_MILESTONE.md` | **Next milestone: ShipFlow as MCP Server** |

---

## Development Setup

### Prerequisites

- Java 21 (Temurin recommended)
- Node 18 LTS
- Docker (PostgreSQL + Redis via `docker compose`)
- Maven 3.9+

### Quick Start

```bash
# 1. Start infrastructure
docker compose up -d   # PostgreSQL + Redis

# 2. Backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Frontend
cd frontend
npm install
npm run dev
```

Backend runs on **http://localhost:8080**
Frontend runs on **http://localhost:3000**
Swagger UI: **http://localhost:8080/swagger-ui.html**

### Environment Variables (dev profile)

```bash
# Database (matches docker-compose defaults)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/shipflowdb
SPRING_DATASOURCE_USERNAME=shipflow
SPRING_DATASOURCE_PASSWORD=shipflow_secret

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=changeme

# JWT (configured via app.jwt.secret in application.properties)
APP_JWT_SECRET=your-dev-secret

# LLM (pick one)
OLLAMA_BASE_URL=http://localhost:11434          # local
OPENAI_API_KEY=sk-...                          # OpenAI

# MCP clients (optional)
MCP_GITHUB_ENABLED=true
MCP_GITHUB_SERVER_URL=http://localhost:3000
MCP_FIGMA_ENABLED=true
MCP_FIGMA_SERVER_URL=http://localhost:3001
MCP_GITLAB_ENABLED=true
MCP_GITLAB_SERVER_URL=https://gitlab.com   # or your self-hosted instance
```

---

## Build & Test

```bash
# Backend
cd backend
./mvnw verify                  # compile + test + coverage
./mvnw spotless:check          # lint (must pass before commit)
./mvnw spotless:apply          # auto-fix formatting

# Frontend
cd frontend
npm test                       # Vitest unit tests
npm run build                  # production build
npm run storybook              # component explorer
```

**Coverage gate**: 80% line coverage enforced by JaCoCo. Tests must pass before any PR is merged.

> **100% pass rule**: The full test suite must be at 0 failures before every commit. If an infrastructure change (e.g. adding `@EnableSpringDataWebSupport`, changing a serialization mode, renaming a field) causes existing tests to fail, fix the tests in the same PR — never leave known failures in the suite. `./mvnw verify` must exit with `BUILD SUCCESS` and `Failures: 0, Errors: 0`.

---

## Coding Conventions

### Backend (Java)

- **Formatting**: Spotless (Eclipse formatter — see `backend/pom.xml`). Run `./mvnw spotless:apply` before committing.
- **Layering**: Controller → Service → Repository. Never skip layers.
- **DTOs**: Always use DTOs at the controller boundary. Never expose entities directly.
- **Caching**: Use `@Cacheable` / `@CacheEvict` from Spring Cache. Redis is the production store.
- **Events**: Use Spring `ApplicationEventPublisher` for cross-cutting side effects. **A listener that reads an entity the publisher just created/updated MUST use `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Transactional(propagation = REQUIRES_NEW)`, never a plain `@EventListener`.** Publishers fire events *inside* their `@Transactional` method, so an `@Async @EventListener` runs before the row commits and its `findById` returns empty — the work is silently skipped (see the wiki-page-not-ingested bug). AFTER_COMMIT guarantees the row is visible; because there is no transaction to join after commit, the listener must open its own (`REQUIRES_NEW`) — a plain `@Transactional` fails at startup. Reference: `WikiKnowledgeListener` and `ScopeProgressListener`.
- **Soft delete**: Use logical deletion (`deletedAt` timestamp). Never hard-delete user data.
- **Auditing**: Hibernate Envers is enabled. Entity changes are versioned automatically.
- **LLM calls**: Route through the pluggable LLM provider system in `service/llm/`. Never call HTTP directly.
- **Vector stores**: Route through `service/vectorstore/`. Supports Qdrant, ChromaDB, In-Memory.

### Frontend (TypeScript / React)

- **State**: React Query for server state, React Context for global UI state.
- **Forms**: React Hook Form + Zod validation.
- **Styling**: Tailwind CSS 4 utility classes.
- **Components**: Radix UI primitives. Keep components in `components/`, pages in `pages/`.
- **i18n**: All user-facing strings go through `i18next`. Add keys to `src/i18n/`.
- **API calls**: Use the typed service files in `services/`. Never use `fetch` directly in components.

---

## RBAC — Roles at a Glance

| Role | Capabilities |
|------|-------------|
| `ADMIN` | Full system access |
| `PROJECT_MANAGER` | Manage projects, cycles, betting |
| `DEVELOPER` | Create/update tasks and scopes |
| `QA` | Manage test cases and bug reports |
| `PRODUCT` | Manage pitches and roadmap |
| `VIEWER` | Read-only |

See `PERMISSION_MATRIX.md` for the full matrix.

---

## Database Migrations

- Managed by **Flyway**. Files live in `backend/src/main/resources/db/migration/`.
- Naming conventions (do **not** rename existing files):
  - **Preferred for new migrations**: `V{YYYY}_{MM}_{DD}_{sequence}__{description}.sql`
    e.g. `V2026_03_24_0001__add_mcp_api_key_scope.sql`
  - **Legacy sequential** (existing files): `V{N}__{description}.sql`
    e.g. `V1__init.sql`, `V99__add_index.sql`
  - **Date-based without underscores** (older files): `V{YYYYMMDD}{seq}__{description}.sql`
- **Never edit an existing migration.** Always add a new file.
- **Use the date-prefixed format for new migrations — NOT bare sequential `V{N}`.** Flyway parses the version numerically, so a sequential `V110` sorts to `110`, which is *lower* than every date-prefixed `V2026_*` migration (e.g. `V2026_03_30_0001` that creates `task_attachments`). On a **fresh** database Flyway applies in version order, so a `V110` that alters a table created by a later-sorting `V2026_*` migration fails. `spring.flyway.out-of-order=true` rescues already-migrated DBs but **not** fresh installs. The legacy `V{N}__` files exist for history only.
- **Tests don't run Flyway** (H2 schema is generated from entities via create-drop), so a broken migration *order* or PostgreSQL-only DDL passes `./mvnw verify` and only fails at real startup — verify new migrations against PostgreSQL.
- H2 is used for tests; PostgreSQL for dev/prod.

---

## MCP Integration (Current State)

ShipFlow currently acts as an **MCP client**, consuming:

- **GitHub MCP** (`service/mcp/GitHubMcpProvider.java`) — reads repo files for Wise Architecture AI feature.
- **Figma MCP** (`service/mcp/FigmaMcpProvider.java`) — reads design context for AI advice.
- **Notion MCP** (`service/mcp/NotionMcpProvider.java`) / **Confluence MCP** (`service/mcp/ConfluenceMcpProvider.java`) — read design docs / wiki pages and meeting notes into AI context.
- **GitLab MCP** (`service/mcp/GitLabMcpProvider.java`, v1.12.0 S61) — reads repository files from gitlab.com or a self-hosted instance. Unlike the four providers above (which proxy through a separate JSON-RPC `tools/call` MCP-server process), this one calls GitLab's REST API v4 directly on the configured instance — see the class Javadoc for why.

All of the above implement the shared `service/mcp/McpClientService` interface and are plain `@Service` beans — no central registry, Spring just autowires each as a provider. `SystemStatusService` and `AirGappedModeValidator` inject `List<McpClientService>` generically (for status reporting / air-gapped enforcement); `WiseArchitectureService` and `FigmaDesignContextService` additionally inject the GitHub/Figma providers concretely for feature-specific, entity-coupled integration (`GitHubRepository`, `Pitch.wireframeLinks`) that the other providers don't yet have an equivalent of.

Config class: `service/mcp/McpConfig.java`
DB settings: `V2026_02_15_0001__add_mcp_organization_settings.sql`

**v0.7.0**: ShipFlow now also acts as an **MCP server** (opt-in via `MCP_SERVER_ENABLED=true`).
External AI tools (Claude Code, Cursor, Claude Desktop) can query and mutate ShipFlow data.
See `MCP_CLIENT_SETUP.md` for client configuration.

---

## AI Features Map

| Feature | Entry Point | Notes |
|---------|------------|-------|
| Wise Architecture | `WiseArchitectureService` | Multi-source context (code + design + roadmap) |
| Risk Analysis | `RiskAnalysisService` | Per-pitch AI risk scoring |
| QA Test Generation | `AITestGenerationService` | Generates test cases from pitches |
| RAG Q&A | `DocumentQAService` | Vector search + LLM answer |
| AI Cache | `AICacheController` | Redis-backed response cache |
| Knowledge Center | `KnowledgeSourceService` + `service/knowledge/source/provider/*` | Pluggable provider SPI; ingested chunks feed Q&A, Wise Architecture, test gen, and risk analysis |

---

## CI / CD

GitHub Actions pipeline (`.github/workflows/ci.yml`):

1. Spotless format check
2. Backend tests (`./mvnw verify`)
3. Frontend tests (`npm test`)

PRs to `main` (via `develop`) must pass all checks. Branch naming follows `CONTRIBUTING.md`:
`feature/*`, `fix/*`, `docs/*` — use `develop` as the integration branch.

---

## Common Tasks for Claude Code

### Add a new REST endpoint

1. Create/update DTO in `dto/<feature>/`
2. Add method to the relevant `*Service`
3. Add method to the relevant `*Controller` with `@PreAuthorize`
4. Add Flyway migration if schema changed
5. Write a service test in `src/test/`
6. Run `./mvnw spotless:apply && ./mvnw verify`

### Add a new MCP tool (server-side)

The MCP server is live as of v0.7.0. To add a new tool:

1. Add the tool method to the relevant `*McpTools` class in `service/mcp/server/tools/`
2. Register it in `McpToolDispatcher` — add to `READ_TOOLS` or `WRITE_TOOLS` map and add a static definition method
3. Map to the existing service layer — **never bypass it**
4. If it is a write tool, ensure `properties.isWriteEnabled()` is checked before dispatching
5. Add unit tests in `McpToolDispatcherTest` (no Spring context needed)
6. Update `MCP_CLIENT_SETUP.md` tool reference table

### Add a new public page or blog post

Every public page needs its own title, description, canonical URL, and sitemap
entry, or it cannot rank — the SPA otherwise serves one identical `<title>` for
the whole site. See `SEO_GUIDE.md` for the full contract. In short:

1. Call `useSeo({ title, description, path, keywords, jsonLd })` at the top of the page component.
2. Add the route to `STATIC_ROUTES` in `frontend/scripts/generate-sitemap.mjs`.
3. **Blog posts live in the private `ShipFlow-blog` repo, NOT here** — so a merged PR against this open-source repo can't publish an article on shipflow.dev. The `.md` files in `frontend/public/blog/posts/` are build-time copies and get overwritten. Never hand-edit `index.json` (generated). For any local production build use `npm run build:deploy` (= `blog:sync` + `build`); a plain `docker build` skips the CI injection step and silently ships no new posts.
4. Cross-link: a new post needs a "Further reading" section *and* a link to it from an existing post.
5. Never hand-edit `sitemap.xml` (generated) and never reintroduce a blanket `Disallow: /` in `robots.txt` (it once silently blocked the homepage and the entire blog).
6. SEO meta strings are a deliberate exception to the i18n rule — they stay in English at the call site, because public pages serve one URL per page regardless of language.
7. **Name all three methodologies.** ShipFlow is methodology-agnostic (Shape Up + Scrum + Kanban). Shape Up leads in copy because it is the differentiator and the winnable search ground, but never write a description implying it is all ShipFlow does — and keep descriptions ≤160 chars, since the coverage reassurance is what gets truncated first.

### Add a new Knowledge Center provider

The Knowledge Center exposes a `KnowledgeSourceProvider` SPI in `service/knowledge/source/provider/` so a new ingestion source (Confluence, Notion, Google Drive, etc.) is a single Spring bean:

1. Create `service/knowledge/source/provider/MyProvider.java` implementing `KnowledgeSourceProvider`.
2. Set `getType() = KnowledgeProviderType.MY_TYPE` — add the enum value if it doesn't exist yet.
3. Implement `validateConfig(JsonNode)` (throws on bad input) and `ingest(KnowledgeSource, IngestionContext)` returning an `IngestionResult` of `RawChunk`s.
4. Override `supportsRefresh()` if the source can be re-fetched on a schedule (URL-like sources usually yes; one-shot uploads no).
5. No other wiring needed — Spring auto-registers it via `KnowledgeSourceRegistry`. Add a `frontend/src/i18n/.../provider.MY_TYPE` label in `en.json` + `fa.json` so the UI renders the provider name.

### Add a new object-storage provider

Object storage uses the same provider-SPI shape as the Knowledge Center. The `ObjectStorageProvider` SPI lives in `service/storage/` so a new backend (Azure Blob, GCS, etc.) is a single Spring bean. **No controller or service may call a storage SDK directly — everything routes through `ObjectStorageService`** (same discipline as the LLM/vector-store abstractions).

1. Add the enum value in `service/storage/StorageProviderType.java`.
2. Create `service/storage/provider/MyProvider.java` implementing `ObjectStorageProvider` (`getType()`, `validateConfig(JsonNode)`, `store/retrieve/delete`, optional `testConnection`/`presignUrl`). S3-compatible backends can extend the shared AWS-SDK base provider.
3. Reuse `StorageKeyGenerator` for object keys — never inline key logic.
4. No other wiring — Spring auto-registers it via `ObjectStorageRegistry`. Add a `provider.MY_TYPE` i18n label and surface it in the Storage tab of Org Settings.
5. Connection secrets follow the existing integration-secret convention (see the Architectural Decisions Log): stored write-only (never returned in a DTO, never logged); a future `AttributeConverter` can encrypt the isolated secret columns without a schema change.

### Debug an AI feature

- Check `application-dev.properties` for active LLM provider
- Ollama logs: `ollama logs`
- Redis cache: `redis-cli monitor`
- Vector store: check `VectorStoreConfig` for active profile

---

### On every feature release (checklist for Claude Code)

This project is **open source** — every significant feature must be documented and visible to contributors and self-hosters. Run this checklist before merging any non-trivial feature PR:

| # | Task | Where |
|---|------|--------|
| 1 | Add entry under `[Unreleased]` or bump version | `CHANGELOG.md` |
| 2 | Add feature to the `✨ Features` list — refresh screenshots if UI changed | `README.md` |
| 3 | Add row to the comparison table if it differentiates vs competitors | `README.md` → `🔀 How ShipFlow Compares` |
| 4 | Add highlight card to the in-app release notes page | `frontend/src/pages/ReleaseNotes.tsx` |
| 5 | Update competitor positioning if relevant | `COMPETITOR_ANALYSIS.md` |
| 6 | Update `CLAUDE.md` if the feature introduces a new repeatable task pattern | `CLAUDE.md` |
| 7 | Add / update guide doc if users need setup instructions | relevant `*_GUIDE.md` or `MCP_CLIENT_SETUP.md` |
| 7a | **Add in-app help guide** for every new user-facing feature — `{NN}-{feature}.md` in `backend/src/main/resources/knowledgebase/help-guides/`. Auto-loaded by `HelpGuideAIService`. **A feature without a help guide is incomplete.** | `knowledgebase/help-guides/` |
| 8 | Add i18n keys to both `en.json` and `fa.json` | `frontend/src/i18n/` |
| 9 | Update `SampleDataInitializer.java` with demo data for the new feature | `src/main/java/.../SampleDataInitializer.java` |
| 10 | If UI layout changed: update `data-tour` selectors in `TourContext.tsx` AND the Step Inventory table in `TOUR_GUIDE.md` | `TOUR_GUIDE.md` |
| 11 | If help guides reference changed UI: update guide content | `knowledgebase/help-guides/` |
| 12 | Tests: ≥ 80% line coverage; **0 failures in the full suite** — fix any tests broken by your change (serialization, renamed fields, etc.) in the same PR | `src/test/` |
| 13 | Run `./mvnw spotless:apply && ./mvnw verify` — must exit `BUILD SUCCESS, Failures: 0, Errors: 0` | CI must stay green |
| 14 | Update PR title to reflect implementation scope (not just "docs:") | GitHub PR |

> These steps keep the open-source community informed, help self-hosters evaluate upgrades, and ensure Claude Code has accurate context in future sessions.

---

## Architectural Decisions Log

Key product/architecture decisions recorded here so future Claude Code sessions don't re-debate them.

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-04-05 | Competitor migration tools ship in **v1.2.0**, after Scrum mode (v1.1.0) | 90% of Jira/Linear users work in Sprints. Without Scrum mode, imported sprint history would be dropped or wrongly mapped to Shape Up cycles. Once v1.1 ships, the mapping is clean: Sprint→Sprint, Epic→Pitch, Issue→Task. Migration sequence: CSV import → Linear API → Jira API. Always import into Kanban project by default; teams adopt Shape Up/Scrum at their own pace. |
| 2026-06-05 | v1.4.0 merges original v1.4 (Enterprise Auth) + v1.5 (UX Depth) into one milestone | SSO and UX polish are both table-stakes before the AI/automation sprint; combining keeps the release train moving without fragmenting small polish fixes into a separate patch version. |
| 2026-06-18 | v1.8.0 attachments go through a pluggable **object-storage SPI** (`service/storage/`, backends LOCAL_FS/S3/MinIO); LOCAL_FS stays the default so nothing breaks out of the box | Self-hosters need S3/MinIO without code changes. Mirrors the Knowledge Center provider pattern (registry over `List<Provider>`). All attachment I/O routes through `ObjectStorageService`; existing local files keep working and are migrated on backend switch (`StorageMigrationService`, copy-verified before source untouched). |
| 2026-06-18 | Storage connection **credentials stored as plaintext TEXT**, consistent with existing integration secrets (Figma/GitHub/Notion/SMTP), rather than introducing at-rest encryption now | Consistency with the current codebase (no encryption subsystem exists yet) per product decision. Compensating controls: secrets never returned in any DTO (`hasSecretKey` boolean only), never logged. Secret columns are isolated so a future AES-GCM `AttributeConverter` can be dropped in with no schema change. |
| 2026-06-25 | Wiki **revision comparison diffs are computed client-side per BlockNote block** (`frontend/src/components/wiki/wikiDiff.ts`: `blocksToLines` → LCS `diffLines`), NOT from the stored `contentText` | `WikiService.extractText` joins every text node with single spaces, so `contentText` is one space-delimited blob with no block/line boundaries — useless for a readable line diff. The fix: a new read-only `GET /wiki/pages/{id}/revisions/{revision}` returns a single revision's full BlockNote `content` JSON; the frontend extracts one line per block (descending into children) and runs an LCS line diff into side-by-side rows (same/added/removed/changed). Future "compare/diff revisions" work should reuse `wikiDiff.ts`, not diff `contentText`. The compare dialog reuses the `["wiki-page", pageId]` React Query cache for the current version (no extra fetch) and renders highlighted text lines rather than mounting a second BlockNote editor (avoids the cost of two live editors). |
| 2026-06-25 | Wiki page **comments reuse the existing polymorphic comment system** (`CommentEntityType.WIKI_PAGE` on `/api/comments`), not a new wiki-specific table | The `Comment` entity is keyed by `(entityType, entityId)` with no FK, so adding a new commentable surface is purely additive — a new enum value, one `validateEntityExists` case, a notification action-URL case (`/wiki/{spaceId}/{pageId}`), and reusing the `Comments.tsx` component. **Gotcha (fixed in `V2026_06_26_0001`):** the `comments` table has a `CHECK (entity_type IN ('TASK','BUG_REPORT'))` constraint from `V64`, so a new enum value REQUIRES a migration to widen it — H2 tests generate the schema from entities (no CHECK) so they pass while real Postgres rejects the insert with a data-integrity violation. Any future commentable entity must both extend the enum AND add a constraint-widening migration. Note: wiki comments are `isAuthenticated()`-only (matching tasks/bugs), NOT gated by per-space wiki ACL — revisit if private-space comment confidentiality becomes a requirement. |
| 2026-06-26 | **ALL persistent file storage routes through `ObjectStorageService`** (not just wiki/task attachments). `DocumentService` (pitch/meeting/cycle/note docs + bug-report media) and the Knowledge Center `FileUploadProvider` were migrated off direct filesystem writes. Transient/streamed flows (CSV/Zephyr imports, report/data exports, avatars-as-URL) persist no downloadable file, so they stay as-is. | Self-hosters expect every uploaded file in their S3/MinIO bucket. Key points future file-handling code MUST follow: (1) Use `ObjectStorageService.store(...)` (validates 10MB + image/PDF/doc MIME allowlist) for general attachments; use `storeWithoutValidation(...)` ONLY when the caller enforces its own broader policy (e.g. bug media allows video + ~50MB — the façade allowlist would wrongly reject it). (2) Buffer the multipart bytes ONCE (`file.getBytes()`) when both storage AND text-extraction consume the stream. (3) Entities that store files carry `storageProvider` (enum) + `storageKey`, with a legacy read/delete fallback while old rows still have only the pre-SPI path. (4) **Object keys follow a grouped folder convention** — `attachments/{task,bug,wiki}/{id}/…`, `documents/{type}/{id}/…`, `knowledge/{sourceId}/…` — generated by the shared `DocumentService.storageKeyHint(entityType, entityId)` helper; reuse it (and keep `StorageMigrationService` calling the same helper) so upload-time and migration-time keys never drift. (5) Knowledge Center file persistence needs NO schema change: `FileUploadProvider` returns `storageKey`/`storageProvider` in `IngestionResult.sourceMetadata`, which the ingest flow merges into the source's `config` JSON. |
| 2026-06-26 | **Knowledge-Center indexing (embedding generation) MUST run in the background, never inline in an upload/save request.** Fixed: `DocumentService.uploadDocument` called `KnowledgeIngestionService.ingestDocument` synchronously — that method is `@Transactional(REQUIRES_NEW)` but was the one `ingest*` method NOT also `@Async`, so a ~1.5 MB doc blocked the HTTP response ~45 s (a large PDF, minutes). | Embedding generation uses a local CPU ONNX model and scales with content size; doing it on the request thread makes uploads look frozen (the file is already stored, so the object appears in the bucket while nothing returns → users re-upload repeatedly). Fix pattern (reuse it for any new "store + index" flow): the upload method only stores + saves + publishes a domain event (e.g. `DocumentUploadedEvent(id)`); a dedicated listener does the indexing with `@Async @TransactionalEventListener(AFTER_COMMIT) @Transactional(REQUIRES_NEW)` — see `DocumentKnowledgeListener` / `WikiKnowledgeListener`. AFTER_COMMIT guarantees the row is visible; `@Async` keeps embedding off the request thread; the `indexedForQA` flag flips when the listener finishes. Keep cheap, response-shaping work (text extraction, which feeds `textExtracted`/`extractedText`) inline; defer only the slow embedding. Note H2 tests don't exercise this latency, so it only shows up at runtime with a real upload — verify upload responsiveness live, not just via unit tests. |
| 2026-07-20 | **MCP URL-embedded-token auth (`/mcp/{api-key}` and `/mcp/{api-key}/sse`, added for claude.ai's hosted connector, which can't send custom headers) grants the key's real configured scope — it is NOT capped to read-only.** Shipped read-only-capped first as the safer default, then explicitly reverted same-day at Farzad's request once it blocked a real use case (wanting write access from claude.ai). | A URL-embedded token can be logged by proxies/browser history — weaker than a header — so read-only-by-default was the cautious first cut. But claude.ai's connector literally cannot send a header, so the URL *is* its only auth channel; forcing read-only there while allowing full scope over headers meant write access was simply unreachable from claude.ai, full stop. Farzad chose to accept the leak risk in exchange for functionality, mitigated operationally (dedicated, short-lived, easily-rotated keys scoped to only what a given connector needs) rather than structurally. **The structural fix is OAuth 2.0** (claude.ai's connectors support DCR + authorization-code + PKCE per Anthropic's own docs — the token would then never appear in a URL at all) — deferred, tracked as `MCP_SERVER_MILESTONE.md` GAP 11, not yet started. Any future change to this capping behavior should treat "grants real scope" as the current intentional state, not a bug to silently re-fix. |
| 2026-07-27 | **Debt/Improvement tasks in Shape Up projects may be created without a cycle** (stored via a direct `projectId` reference, cycle left null); **Pitch Scope tasks in Shape Up still require a cycle.** Confirmed with Farzad as category-scoped, not a blanket relaxation, when he reported the "must pick a cycle" restriction as a regression from earlier behavior. | Debt/Improvement work is opportunistic filler picked up when nothing else is scheduled — forcing an upfront cycle guess produced busywork or wrong-cycle bookkeeping. Pitch Scope tasks are excluded because shaped pitch work must belong to a bet; that discipline is core to Shape Up, not incidental. Backend: `TaskService.createTask`'s existing SCRUM-only "product backlog" no-cycle path (`projectId` present, `cycleId` null) was extended to also allow `SHAPE_UP` projects when the resolved category is `DEBT_IMPROVEMENT` — same mechanism, narrower gate, new `error.task.backlog.unsupported` message covering both. Frontend: two independent gates had to be updated in lockstep — `useBacklogPage`'s dialog-open/validate logic AND `BacklogHeader`'s own separate button-`disabled` calculation (a second, presentation-layer copy of the "needs a cycle" check). Fixing only the first would have left the New Task button inert, since disabled is computed before the click handler ever runs — caught only by live-clicking the button in a browser, not by backend tests or `tsc`. Any future relaxation of cycle requirements should grep for both `useBacklogPage`'s gates and `BacklogHeader`'s `needsCycle` before assuming a fix is complete. |
| 2026-08-05 | **`GitLabMcpProvider` (v1.12.0 S61) calls GitLab's REST API v4 directly on the configured instance, breaking from the JSON-RPC `tools/call`-through-an-MCP-server pattern every other MCP client provider (GitHub/Figma/Notion/Confluence) uses.** `McpConfig.GitLabMcpConfig#serverUrl` is therefore the GitLab *instance* base URL (e.g. `https://gitlab.com` or a self-hosted URL), not an MCP-server endpoint. | GitHub/Figma/Notion/Confluence each proxy through a dedicated MCP-server process that translates a JSON-RPC tool call into a vendor API request; there is no similarly ubiquitous, ready-to-run "GitLab MCP server" to target. GitLab's REST API v4 is stable, well-documented, and directly callable, so `GitLabMcpProvider` talks to it directly (`{serverUrl}/api/v4/projects/:id/repository/tree|files/:path/raw|search`), authenticating with a Personal Access Token via GitLab's own `PRIVATE-TOKEN` header (not `Authorization: Bearer`, which GitLab reserves for OAuth2 tokens). Also **not yet wired into `WiseArchitectureService`'s pitch-level repository scanning**: that integration is coupled to the GitHub-specific `GitHubRepository` entity (persisted owner/repo per pitch, with its own OAuth/webhook machinery — see `GITHUB_INTEGRATION_GUIDE.md`) and a matching frontend repo-picker UI, neither of which has a GitLab equivalent yet. As a `@Service` implementing `McpClientService`, `GitLabMcpProvider` is still a live, non-dead bean today: `SystemStatusService` and `AirGappedModeValidator` both inject `List<McpClientService>` generically (for system-status reporting and air-gapped-mode enforcement respectively), so GitLab is automatically included there with no extra wiring. Full parity with GitHub (a `GitLabRepository` entity + migration + repo-picker UI + `WiseArchitectureService` integration) is real, additional scope — track it as a follow-up session rather than assuming this one covers it. |

---

## Do Not

- Hard-delete user data (use soft delete)
- Bypass `@PreAuthorize` security annotations
- Add `@Transactional` to controller methods
- Call LLM providers directly (use the plugin system)
- Commit `.env` files or secrets
- Skip Spotless (CI will fail)
- Edit existing Flyway migrations
