# ShipFlow — Claude Code Guide

This file is the entry point for Claude Code (and any AI coding assistant) working on this repository.
Read it before touching any code.

---

## Current Milestone: v0.8.0 — "Core Product + Hardening"

**Current version**: v0.7.0 → targeting v0.8.0
**Target**: v1.0.0 open-source release (~12-17 weeks, 29 Claude Code sessions total)
**All PRs target**: `main` branch

ShipFlow is **methodology-agnostic** — supports Shape Up + Kanban today; Scrum (Sprints ≈ Cycles) planned for v1.1.

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
7. Write unit or integration tests (JaCoCo gate: ≥ 80% line coverage)
8. Update `SampleDataInitializer.java` with demo data for the new feature
9. **If UI layout changed**: verify onboarding tour step selectors in `frontend/src/contexts/TourContext.tsx` still target correct elements. Update both the selector and the **Step Inventory** table in `TOUR_GUIDE.md`. See `TOUR_GUIDE.md` for the full maintenance contract.
10. **If in-app help guides reference changed UI**: update the relevant help guide content
11. Create PR targeting `main` using `feat/fix/chore/refactor/test/docs` prefix

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

---

## Coding Conventions

### Backend (Java)

- **Formatting**: Spotless (Eclipse formatter — see `backend/pom.xml`). Run `./mvnw spotless:apply` before committing.
- **Layering**: Controller → Service → Repository. Never skip layers.
- **DTOs**: Always use DTOs at the controller boundary. Never expose entities directly.
- **Caching**: Use `@Cacheable` / `@CacheEvict` from Spring Cache. Redis is the production store.
- **Events**: Use Spring `ApplicationEventPublisher` for cross-cutting side effects.
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
- H2 is used for tests; PostgreSQL for dev/prod.

---

## MCP Integration (Current State)

ShipFlow currently acts as an **MCP client**, consuming:

- **GitHub MCP** (`service/mcp/GitHubMcpProvider.java`) — reads repo files for Wise Architecture AI feature.
- **Figma MCP** (`service/mcp/FigmaMcpProvider.java`) — reads design context for AI advice.

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
| 8 | Add i18n keys to both `en.json` and `fa.json` | `frontend/src/i18n/` |
| 9 | Update `SampleDataInitializer.java` with demo data for the new feature | `src/main/java/.../SampleDataInitializer.java` |
| 10 | If UI layout changed: update `data-tour` selectors in `TourContext.tsx` AND the Step Inventory table in `TOUR_GUIDE.md` | `TOUR_GUIDE.md` |
| 11 | If help guides reference changed UI: update guide content | relevant `*_GUIDE.md` |
| 12 | Tests: ≥ 80% line coverage enforced by JaCoCo; write unit + integration tests | `src/test/` |
| 13 | Run `./mvnw spotless:apply && ./mvnw verify` and `npm test` | CI must stay green |
| 14 | Update PR title to reflect implementation scope (not just "docs:") | GitHub PR |

> These steps keep the open-source community informed, help self-hosters evaluate upgrades, and ensure Claude Code has accurate context in future sessions.

---

## Do Not

- Hard-delete user data (use soft delete)
- Bypass `@PreAuthorize` security annotations
- Add `@Transactional` to controller methods
- Call LLM providers directly (use the plugin system)
- Commit `.env` files or secrets
- Skip Spotless (CI will fail)
- Edit existing Flyway migrations
