# Knowledge Center — Design Spec

**Date:** 2026-06-04
**Status:** Draft for review
**Target milestone:** v1.3.0 (proposed)
**Scope:** This spec covers sub-project #1 of a larger Knowledge Center initiative — the core platform plus the first two source providers (file upload, URL). GitHub, Confluence, Notion, and Google Drive providers are intentionally deferred to follow-up specs that plug into the same SPI.

---

## 1. Why

ShipFlow is increasingly AI-driven — Q&A, test case generation, Wise Architecture, and risk analysis all depend on retrievable team knowledge. Today that corpus is implicit: it grows only as a side effect of using core ShipFlow entities (pitches, meeting notes, worklogs, evidence, etc.). Teams cannot intentionally feed material the AI should know — engineering handbooks, payment standards, RFCs, retro notes from outside the tool, third-party API docs.

The Knowledge Center is a first-class place to add that material, structured so additional sources (GitHub, Confluence, Notion, Drive) can be plugged in later without touching core retrieval or AI features.

---

## 2. Where this fits in the existing knowledge fabric

ShipFlow already has a complete vector-retrieval stack. The Knowledge Center plugs into it; it does not build a new one.

```
┌──────────────────────────────────────────────────────────────────┐
│  Sources (where knowledge originates — many, growing over time)  │
├───────────────────────────┬──────────────────────────────────────┤
│ Already auto-ingested:    │ New via Knowledge Center providers:  │
│  • Pitches                │  • Uploaded files (v1)               │
│  • Meeting notes          │  • URLs           (v1)               │
│  • Worklogs               │  • GitHub repo    (later spec)       │
│  • Manual notes           │  • Confluence     (later spec)       │
│  • Evidence, Cycles, …    │  • Notion         (later spec)       │
│  • Initiatives, Epics, …  │  • Google Drive   (later spec)       │
└─────────────┬─────────────┴─────────────────┬────────────────────┘
              │                               │
              ▼                               ▼
       Spring ApplicationEvent       KnowledgeSourceProvider.ingest()
              │                               │
              └──────────────┬────────────────┘
                             ▼
             KnowledgeIngestionService.ingest(...)
              • chunks the text
              • writes Postgres row: knowledge_items
              • computes embedding via EmbeddingModel
              • writes vector to EmbeddingStore (Qdrant / Chroma / in-memory)
                             │
                             ▼
         Single retrieval surface used by ALL AI features
              • RAG Q&A        • Test generation
              • Wise Arch      • Risk analysis
```

Key facts to internalize before reading the rest of this spec:

- **The vector store is `EmbeddingStore<TextSegment>` from LangChain4j** (Qdrant in prod, in-memory in dev). Embeddings are computed by an `EmbeddingModel` (Ollama or OpenAI depending on profile).
- **Postgres `knowledge_items` is the metadata/text row** — title, content, scope columns (`teamId`, `cycleId`), `entityType`, `entityId`, `isEmbedded` flag. The actual vector lives in the embedding store, keyed by `knowledge_items.id`.
- **Existing pitch meeting notes are already knowledge.** `KnowledgeEntityType` already includes `PITCH`, `MEETING`, `WORKLOG`, `MANUAL_NOTE`, `EVIDENCE`, `INITIATIVE`, `EPIC`, `RELEASE`. `KnowledgeEventListener` has 7 `@Async @EventListener` handlers that ingest on entity create/update.
- **The Knowledge Center adds a new lane to the same fabric.** Each provider translates its input into chunks and hands them to `KnowledgeIngestionService` — the same service the event listeners already use. A new `KnowledgeEntityType.KNOWLEDGE_SOURCE` value tags those chunks so they're distinguishable in retrieval and filters.
- **Scope filtering is uniform.** Org/team/project filters are applied at retrieval time. A retrieval for "Pitch X" returns Pitch X's own meeting notes + team-scoped Knowledge Center docs + org-wide standards docs — all in one vector search, then re-ranked.

Do not build a parallel store. Do not add a new embedding model. The whole point is one corpus, many sources.

---

## 3. Data model

One new entity, one new enum value on `KnowledgeEntityType`, and one nullable FK column added to `KnowledgeItem`.

### 3.1 `KnowledgeSource` (new entity, new table `knowledge_sources`)

| Field | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | |
| `name` | VARCHAR(255), NOT NULL | User-facing, e.g. "Engineering Handbook" |
| `description` | TEXT, nullable | Optional context; surfaced in UI and retrieval metadata |
| `providerType` | ENUM, NOT NULL | `FILE_UPLOAD`, `URL` (v1); `GITHUB`, `CONFLUENCE`, `NOTION`, `GOOGLE_DRIVE` reserved |
| `scope` | ENUM, NOT NULL | `ORG`, `TEAM`, `PROJECT` |
| `organizationId` | BIGINT FK, NOT NULL | Tenant boundary — always set |
| `teamId` | BIGINT FK, nullable | Required when `scope=TEAM` |
| `projectId` | BIGINT FK, nullable | Required when `scope=PROJECT` |
| `config` | TEXT (JSON), NOT NULL | Provider-specific config; provider validates its own slice |
| `status` | ENUM, NOT NULL | `PENDING`, `INGESTING`, `READY`, `FAILED`, `STALE` |
| `lastIngestedAt` | TIMESTAMP WITH TIME ZONE, nullable | |
| `lastError` | TEXT, nullable | Surfaced in UI on failure; truncated to 1000 chars |
| `createdBy` | BIGINT FK → users | |
| `createdAt`, `updatedAt`, `deletedAt` | TIMESTAMP WITH TIME ZONE | Standard ShipFlow soft-delete |

**Indexes:**
- `idx_knowledge_sources_org` on `organization_id`
- `idx_knowledge_sources_team` on `team_id`
- `idx_knowledge_sources_project` on `project_id`
- `idx_knowledge_sources_status` on `status` (for the scheduled refresh job)

**FK names** per ShipFlow convention: `fk_knowledge_sources_organization`, `fk_knowledge_sources_team`, `fk_knowledge_sources_project`, `fk_knowledge_sources_user`.

### 3.2 `KnowledgeItem` extension

Add nullable `knowledge_source_id BIGINT` column with FK to `knowledge_sources`. Existing items (pitches, meetings, notes) keep working unchanged — the column is null for them. Items produced by Knowledge Center providers point back to their parent source so retrieval can resolve scope and provenance.

Add `KNOWLEDGE_SOURCE` to the `KnowledgeEntityType` enum.

### 3.3 Migration

One new Flyway file: `V{next_sequential}__add_knowledge_sources.sql`. Scan `backend/src/main/resources/db/migration/` for the highest existing `V{N}__*.sql` before picking the number. H2-safe DDL only — no `jsonb`, no PostgreSQL-only types. The `config` column is `TEXT` storing JSON, parsed by Jackson in the provider classes. Standard ShipFlow naming rules apply (see `.claude/rules/flyway-migrations.md`).

### 3.4 Why this shape

- `KnowledgeSource` is the "thing the user manages" (one upload, one URL, later one GitHub repo). `KnowledgeItem` stays the "thing the vector store retrieves" — many chunks per source. The 1:N parent/child relationship matches how users think about it.
- A single `config` JSON column avoids 4–5 provider-specific tables. Each provider validates and parses its own slice.
- Scope columns let permission filters live in repository `@Query` clauses — same pattern the rest of ShipFlow uses for pitches, cycles, teams.
- `status` + `lastError` is what lets the UI show "Reindex" / "View error" without a separate jobs table.

---

## 4. Provider SPI (pluggable connectors)

Lives in `backend/.../service/knowledge/source/`, mirroring `service/llm/` and `service/vectorstore/`.

### 4.1 Interface

```java
public interface KnowledgeSourceProvider {

  /** Which provider this is. Used for registry lookup and routing. */
  KnowledgeProviderType getType();

  /** Validate user-supplied config before persisting the KnowledgeSource. */
  void validateConfig(JsonNode config) throws InvalidConfigException;

  /** Optional credential / reachability check. No-op for FILE_UPLOAD / URL. */
  default ConnectionStatus testConnection(JsonNode config) {
    return ConnectionStatus.ok();
  }

  /** Pull raw content from the source. Called on create and refresh. */
  IngestionResult ingest(KnowledgeSource source, IngestionContext ctx);

  /** Whether this provider supports refresh / re-sync. URL=true, FILE_UPLOAD=false. */
  default boolean supportsRefresh() {
    return false;
  }
}
```

### 4.2 Supporting types

- `KnowledgeProviderType` — enum (`FILE_UPLOAD`, `URL`, then `GITHUB`, `CONFLUENCE`, `NOTION`, `GOOGLE_DRIVE`).
- `IngestionResult` — `List<RawChunk> chunks` + `Map<String, Object> sourceMetadata` (final URL after redirects, page title, file MIME, sha256). Merged back into `KnowledgeSource.config` after ingest so the UI can render without refetching.
- `RawChunk` — `{ title, content, ordinal, sourceUrl, hash }`. Providers produce *logical* chunks; final sizing/overlap is handled centrally by `KnowledgeIngestionService` so chunking strategy stays consistent across sources.
- `IngestionContext` — carries the multipart file (for `FILE_UPLOAD`), HTTP client, current user, scope. Avoids each provider re-discovering Spring beans.
- `ConnectionStatus` — `{ ok, message }`.
- `InvalidConfigException` — typed exception used by the controller layer to return `400` with field-level detail.

### 4.3 Registry + orchestration

```
KnowledgeSourceRegistry  ← Spring auto-injects List<KnowledgeSourceProvider>
        │                  indexes by getType()
        ▼
KnowledgeSourceService    ← the only thing controllers and AI features call
        │                  - create()      → resolve provider, validate, persist, enqueue
        │                  - refresh(id)   → re-ingest if supportsRefresh()
        │                  - delete(id)    → soft-delete source + cascade soft-delete items + remove vectors
        │                  - listScoped()  → org/team/project filtered list with permission check
        ▼
@Async ingestion job      → provider.ingest() → KnowledgeIngestionService.ingestChunks()
                                                (existing service — chunks, embeds, stores)
        │
        ▼
Updates source.status (INGESTING → READY / FAILED) and source.lastError
```

### 4.4 v1 providers

| Provider | `config` JSON shape | Chunking + notes |
|---|---|---|
| **`FileUploadProvider`** | `{ "originalFilename", "contentType", "storageKey", "sha256" }` | Apache Tika extracts text for PDF/DOCX/MD/TXT. File bytes stored via the existing attachment storage layer (introduced in S07/S08). `storageKey` references it. |
| **`UrlProvider`** | `{ "url", "fetchedAt", "finalUrl", "etag" }` | Jsoup fetch → boilerpipe-style cleanup (drop nav, footers). `supportsRefresh()=true`. Periodic re-fetch honors `etag` / `If-Modified-Since`. |

### 4.5 Adding a future provider (the payoff)

To add **GitHub** later: create `GitHubProvider implements KnowledgeSourceProvider`, set `getType() = GITHUB`, parse `{repo, branch, paths}` from config, walk the tree via the existing `GitHubMcpProvider`, emit `RawChunk`s. **Zero changes to controllers, AI features, retrieval, UI scope handling, or migrations** — Spring picks the new bean up automatically because it's a `@Component` implementing the interface. The follow-up spec only needs to design the provider's OAuth/sync/dedup specifics.

### 4.6 Why this shape

- **One interface, one orchestrator** is the simplest abstraction that still lets each provider own its quirks (OAuth, sync semantics, rate limits).
- **`RawChunk`, not pre-embedded chunks**, so chunking strategy and embedding model can change centrally without touching providers.
- **Async ingestion** keeps the create-source HTTP request snappy and aligns with the existing `@Async @EventListener` pattern in `KnowledgeEventListener`.
- **`testConnection` on the interface even though file/URL don't need it** — keeps the UI's "Test connection" button uniform across future OAuth providers.

---

## 5. Ingestion & refresh flow

### 5.1 Create flow (happy path)

```
[1] POST /api/knowledge/sources
    body: { name, providerType, scope, teamId?, projectId?, config }
       │
       ▼
[2] KnowledgeSourceService.create()
    • resolve provider via KnowledgeSourceRegistry
    • provider.validateConfig(config)        ← sync, throws InvalidConfigException on bad input
    • enforce scope/RBAC (see §6)
    • persist KnowledgeSource with status=PENDING
    • publish KnowledgeSourceCreatedEvent
       │
       ▼  HTTP returns 202 Accepted + source DTO (status=PENDING)
[3] @Async @EventListener — IngestionOrchestrator.onCreated()
    • source.status = INGESTING; save
    • try {
        IngestionResult r = provider.ingest(source, ctx);
        knowledgeIngestionService.ingestChunks(
            r.chunks,
            KnowledgeEntityType.KNOWLEDGE_SOURCE,
            source.id,
            scopeFields(source));         // organizationId / teamId / projectId pass-through
                                          // (cycleId is N/A for KNOWLEDGE_SOURCE — sources are scoped Org/Team/Project)
        source.config = merge(source.config, r.sourceMetadata);
        source.status = READY;
        source.lastIngestedAt = now();
      } catch (Exception e) {
        source.status = FAILED;
        source.lastError = truncate(e.getMessage(), 1000);
      }
       │
       ▼
[4] SSE event "knowledge.source.updated" → frontend re-fetches the row
```

### 5.2 Refresh flow

- **Manual:** `POST /api/knowledge/sources/{id}/refresh` → same orchestrator path, skips `validateConfig`.
- **Scheduled:** `@Scheduled` job runs once per day looking for `URL` sources where `lastIngestedAt < now() - 24h` and `status IN (READY, STALE)`. Cheap because Jsoup sends `If-None-Match` using the saved `etag` — a 304 response short-circuits and just bumps `lastIngestedAt`.
- **Dedup on refresh:** each `RawChunk` has a `hash` (sha256 of content). On re-ingest, the orchestrator diffs old vs new chunks for the same source:
  - Same hash → keep existing `KnowledgeItem` + embedding (no re-embed cost).
  - New hash → embed and insert.
  - Missing from new set → soft-delete the `KnowledgeItem` and remove its vector from the embedding store.

  This is the only place dedup logic lives.

### 5.3 Delete flow

`DELETE /api/knowledge/sources/{id}`:

1. Soft-delete `KnowledgeSource` (`deletedAt`).
2. Cascade soft-delete all `KnowledgeItem`s where `knowledge_source_id = id`.
3. **Hard-delete** their vectors from the embedding store — soft-deletion is meaningless to a vector store, and stale vectors would leak into retrieval. The existing `EmbeddingStore` API supports id-based removal.
4. Publish `KnowledgeSourceDeletedEvent` for any consumers (cache invalidation).

### 5.4 Status state machine

```
              ┌──────► READY ──────────► STALE  (scheduler marks when source > 30d unrefreshed)
PENDING ──────┤            ▲                 │
              └──► INGESTING ──► FAILED      └──► (manual refresh) ──► INGESTING
                       ▲                                │
                       └────────── refresh ─────────────┘
```

`STALE` is informational only — it does not block retrieval. The UI surfaces a "Refresh" CTA.

### 5.5 Failure handling

- **Provider errors** (bad URL, parse failure, OAuth rejection in later providers) → `status=FAILED`, full message stored in `lastError`, UI shows a red badge and a "Retry" button.
- **Embedding errors** (LLM provider down) → leave `KnowledgeItem.isEmbedded = false`. The existing `processPendingEmbeddings()` job in `KnowledgeIngestionService` picks them up later. The source still transitions to `READY` because the text is stored — embeddings are eventually consistent.
- **Partial chunk failures** → log at item level and continue. One bad chunk does not fail the whole source.
- **Retries:** orchestrator retries the *whole* `ingest()` call once with 30s backoff on transient errors (timeout, 5xx). Implemented with Spring `@Retryable` on the orchestrator method — no new retry queue infrastructure.

---

## 6. AI consumer wiring + permissions

### 6.1 How AI features get the new context

The win: **no AI service needs to know the Knowledge Center exists.** All four already call the existing RAG retrieval entry point, which returns `KnowledgeItem`s. Knowledge Center just adds more rows to that table.

| AI feature | Retrieval call (unchanged) | What changes in the service |
|---|---|---|
| `DocumentQAService` | `ragRetriever.retrieve(query, scope)` | nothing |
| `QATestGenerationService` | `ragRetriever.retrieve(pitch, scope)` | nothing |
| `WiseArchitectureService` | `ragRetriever.retrieve(pitch, scope)` | nothing |
| `RiskAnalysisService` | `ragRetriever.retrieve(pitch, scope)` | nothing |

The only retrieval-layer change: **expand the scope filter** so Knowledge Center items at org/team/project scopes are eligible for the same vector search.

### 6.2 The unified scope filter

```java
// Pseudocode for the predicate applied during vector retrieval
boolean isVisible(KnowledgeItem item, RetrievalScope scope) {
  // Already-supported: entity-bound items (pitch, meeting on this pitch, etc.)
  if (item.entityType != KNOWLEDGE_SOURCE) {
    return existingPredicate(item, scope);
  }

  // New: KNOWLEDGE_SOURCE items use the parent source's scope
  KnowledgeSource src = item.knowledgeSource;
  return switch (src.scope) {
    case ORG     -> src.organizationId == scope.organizationId;
    case TEAM    -> src.organizationId == scope.organizationId
                     && scope.teamIds.contains(src.teamId);
    case PROJECT -> src.organizationId == scope.organizationId
                     && scope.projectId != null
                     && src.projectId == scope.projectId;
  };
}
```

In practice this is one denormalized metadata payload (scope + org/team/project ids) attached to each vector at write time, so the filter is a vector-store-side metadata filter — no post-fetch filtering, no second DB roundtrip.

### 6.3 Provenance tagging in prompts

Each retrieved chunk surfaces its provenance in the prompt:

```
[Pitch meeting note — "Auth retrospective"] ...
[Knowledge Center — "Engineering Handbook" (file: handbook.pdf, p.12)] ...
[Knowledge Center — "Payments standards" (url: https://...)] ...
```

Provider type + source name come straight off `KnowledgeSource`. Lives in the existing prompt builder — one small helper, not a per-feature change.

### 6.4 Permissions

Aligned with `PERMISSION_MATRIX.md`:

| Action | Org scope | Team scope | Project scope |
|---|---|---|---|
| **Create source** | `ADMIN` | Team member with `PROJECT_MANAGER` or `ADMIN` | Project member with `PROJECT_MANAGER`, `DEVELOPER`, `PRODUCT`, or `ADMIN` |
| **Update / refresh / delete** | `ADMIN`, or `createdBy == currentUser` | Same + team membership check | Same + project membership check |
| **List / search / use in AI** | Any authenticated org member | Any team member | Any project member |
| **View source contents (text)** | Same as "use in AI" | Same | Same |

`VIEWER` has read-only access at every level — same rule as everywhere else in ShipFlow.

Implementation: `@PreAuthorize` on every `KnowledgeSourceController` method + a `KnowledgeSourceAccessChecker` bean for the scope+membership lookup. Same shape ShipFlow already uses for pitches/cycles.

### 6.5 Audit + observability

- Hibernate Envers auto-audits `KnowledgeSource` (entity-level versioning is enabled project-wide).
- Micrometer counters: `knowledge_source_ingest_total{provider,status}`, `knowledge_source_retrieval_total{provider}`, `knowledge_source_chunks_total{provider}`. Matches the existing RAG observability described in `RAG_ARCHITECTURE.md`.
- `DocumentQAService` already supports a `sources` array in its response for citations — Knowledge Center items appear there with source name + provider type so users see *why* the AI said something.

---

## 7. UI

### 7.1 Where it lives

- New route: `/knowledge`. Top-level item in the main sidebar, placed adjacent to "Documents".
- New page component: `frontend/src/pages/KnowledgeCenter.tsx`, with subcomponents in `frontend/src/components/knowledge/`.
- Lazy-loaded via `React.lazy` per the S06 code-splitting convention.

### 7.2 Page layout

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Knowledge Center                                  [+ Add source ▼]      │
│  Sources the AI uses for Q&A, test gen, architecture, and risk advice.   │
├──────────────────────────────────────────────────────────────────────────┤
│  [ Org-wide ]  [ Team: ▾ Platform ]  [ Project: ▾ All ]   🔍 search...   │
├──────────────────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │ 📄  Engineering Handbook                       Org · file (pdf)    │  │
│  │     handbook.pdf · 184 chunks · synced 2h ago     [Refresh] [⋯]    │  │
│  │     ● READY                                                        │  │
│  ├────────────────────────────────────────────────────────────────────┤  │
│  │ 🔗  Payments standards                         Org · url           │  │
│  │     wiki.company.com/payments · 32 chunks · synced 2d ago          │  │
│  │     ● STALE — content older than 30 days       [Refresh] [⋯]      │  │
│  ├────────────────────────────────────────────────────────────────────┤  │
│  │ 📄  Auth retro notes                           Team · file (md)    │  │
│  │     auth-retro.md · ingesting…                                     │  │
│  │     ◐ INGESTING                                                    │  │
│  ├────────────────────────────────────────────────────────────────────┤  │
│  │ 🔗  RFC 9110                                   Project · url       │  │
│  │     httpwg.org/specs/rfc9110.html · failed to fetch                │  │
│  │     ✕ FAILED — connection timeout              [Retry] [⋯]        │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
```

### 7.3 Subcomponents

- **`<ScopeTabs />`** — Org / Team / Project tabs. Team and Project tabs are dropdowns showing only teams/projects the current user belongs to. State stored in the URL (`?scope=team&teamId=42`) so links are shareable.
- **`<SourceList />`** — paginated list driven by React Query: `useKnowledgeSources({scope, teamId, projectId, search})`. Each row is `<SourceRow />`: provider icon, name, status badge, chunk count, last sync, kebab menu (Refresh / Edit / Delete / View text).
- **`<AddSourceDialog />`** — Radix Dialog + React Hook Form + Zod. Three steps:
  1. Pick provider type. File and URL are active in v1. The four deferred integrations (GitHub, Confluence, Notion, Drive) appear as greyed-out cards labelled "Coming soon" — telegraphs the roadmap and reserves the visual slot.
  2. Provider-specific form (drag-and-drop file picker, or URL input).
  3. Pick scope (Org / Team / Project — defaults to the currently-active tab).

### 7.4 Detail panel

Click a row → slide-over panel showing source metadata, list of chunks with content previews (so users can verify what the AI sees), and a "Re-ingest" button. This is the trust-building feature: answers "why didn't the AI find this?" in one click. Ships in v1.

### 7.5 Live status updates

`KnowledgeSourceCreatedEvent` / `…UpdatedEvent` / `…DeletedEvent` are emitted on the existing SSE channel (introduced in S16). The page subscribes; the row's status flips from `INGESTING` to `READY` without a manual refresh.

### 7.6 Surfaces in existing AI features

Two tiny touchpoints in existing UI — both already have RAG context plumbing:

- **Chat (`DocumentQA`):** citation chips already render source names; new chip color/icon for Knowledge Center items, link goes to `/knowledge` filtered to that source.
- **Test generation result:** "Based on" footer gains Knowledge Center items alongside the pitch context.

No other AI surfaces need UI changes — they consume retrieved context server-side.

### 7.7 Tour + onboarding

Per `TOUR_GUIDE.md`: add one new tour step on first visit to `/knowledge` (`data-tour="knowledge-add-source"`). Update `TourContext.tsx` and the Step Inventory table.

### 7.8 i18n + public-pages discipline

- All strings via `useTranslation()`. Keys live under `knowledgeCenter.*` in **both** `en.json` and `fa.json`.
- New feature card in `ReleaseNotes.tsx` AND new bullet under the current milestone in `PublicRoadmap.tsx`, kept in sync per the Public Pages Alignment rule in `CLAUDE.md`.

---

## 8. Testing approach

- **Unit tests** per provider — `validateConfig` happy/sad paths, `ingest` against fixtures (sample PDF, sample HTML). No Spring context needed.
- **Service-layer tests** for `KnowledgeSourceService` and `IngestionOrchestrator` with mock providers — exercise the state machine, dedup-on-refresh, delete-cascade-and-vector-removal.
- **Integration tests** with H2 + in-memory `EmbeddingStore` for the end-to-end flow: POST source → poll until `READY` → query retrieval → assert chunks come back with correct scope.
- **Retrieval-layer test:** create three sources at Org / Team / Project scopes and verify the unified scope filter returns the right set for each retrieval scope.
- **Controller tests** for RBAC: every `@PreAuthorize` exercised with each role.
- **JaCoCo gate ≥ 80%** on new code. Full suite must remain at 0 failures.

---

## 9. Rollout

- Single Flyway migration. No data backfill needed — column on `knowledge_items` is nullable, existing rows stay null.
- Feature is additive: zero changes to existing AI feature behavior when there are no Knowledge Sources defined.
- No new environment variables for v1 (file storage and vector store already configured).
- `SampleDataInitializer.java` seeds two example sources: an org-wide file upload ("Engineering Handbook" stub) and an org-wide URL ("Shape Up methodology").
- Per the end-of-session checklist: `CHANGELOG.md`, `README.md` (+ screenshot), `COMPETITOR_ANALYSIS.md` (this is a meaningful differentiator), `ReleaseNotes.tsx` + `PublicRoadmap.tsx`, both i18n locales, tour step, sample data, tests.

---

## 10. Out of scope (explicitly)

These follow-up specs plug into the same SPI and need their own brainstorming session each:

1. **GitHub provider** — repo selection UI, branch/path config, webhook-driven incremental sync. Reuses existing `GitHubMcpProvider`.
2. **Confluence provider** — reuses Atlassian OAuth from S30 (Jira import). Space/page selection, incremental sync via the Confluence REST API.
3. **Notion provider** — new OAuth from scratch. Workspace/page/database selection.
4. **Google Drive provider** — Google OAuth. Folder-scoped sync of Docs/Sheets/PDFs.

Also explicitly out of scope for v1:

- Rich-text note authoring inside the Knowledge Center (`ManualNote` already exists for pitch-bound notes; if a general note authoring surface is wanted later, it's a separate decision).
- Cross-source dedup ("this Confluence page also lives in this Notion workspace").
- AI-assisted source curation ("the AI suggests this README is outdated").
- Versioning / time-travel browsing of source contents (Envers covers the metadata; chunk history is not retained).
- A separate "personal" / per-user scope.

---

## 11. Open questions

None blocking. Two minor items to confirm during implementation:

- **Exact sidebar position** for the `/knowledge` link — pick during implementation against current nav order.
- **PDF page-level chunking** — whether `RawChunk.ordinal` should encode page boundaries for PDFs so citations can say "p.12". Recommend yes; cheap to do and very valuable for trust. Decide in the plan.
