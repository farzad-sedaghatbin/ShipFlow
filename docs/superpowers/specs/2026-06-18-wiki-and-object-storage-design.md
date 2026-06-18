# Wiki / Docs Space + Pluggable Object Storage — Design

**Date:** 2026-06-18
**Milestone:** v1.9.0 — "Wiki / Docs Space" (sessions S51–S53)
**Branch:** single `feat/wiki-and-object-storage` targeting `main`
**Status:** Approved (brainstorm) — pending implementation plan

This spec covers two related features shipped together in v1.9.0:

1. **Pluggable Object Storage** — a storage-backend abstraction (LOCAL_FS / S3 / MinIO) behind an SPI, so attachments can live on a self-hoster's chosen backend.
2. **Wiki / Docs Space** — a built-in Notion/Confluence-style wiki, whose page attachments consume the storage abstraction.

Object storage lands **first** (it is a cross-cutting prerequisite); the wiki builds on top of it.

---

## Locked decisions (from brainstorm)

| # | Decision | Choice | Note |
|---|----------|--------|------|
| 1 | Rich block editor | **BlockNote** | "Notion-like but as easy as Confluence" — BlockNote ships the block UX (slash menu, drag handles, tables, callouts) opinionated and assembled, no hand-wiring of ProseMirror. Content stored as BlockNote document **JSON**. |
| 2 | Space ↔ project model | **Per-space ACL + optional project link** | New `WikiSpacePermission` entity; space may link to a project and fall back to project membership when no explicit ACL grant exists. |
| 3 | Storage credential at-rest handling | **Plaintext TEXT, consistent with existing integration secrets** | ⚠️ Overrides the original brief's "stored encrypted" line. See **Decision: credential handling** below for the compensating controls and the deferred-encryption seam. |
| 4 | Existing local attachments on backend switch | **Backfill / migrate on switch** | `StorageMigrationService` copies existing objects into the newly-activated backend and rewrites references; idempotent, resumable, copy-verified-before-source-untouched. |

### Decision: credential handling (recorded tradeoff)

The original brief required storage credentials to be "stored encrypted, never logged, never committed." During brainstorm the user chose to store them **plaintext** in a TEXT column, consistent with how ShipFlow already stores Figma/GitHub/Notion/Confluence/SharePoint integration secrets today (no at-rest encryption exists in the codebase yet).

To honor the *spirit* of the requirement without introducing a new encryption subsystem this milestone, we keep the other two guarantees and leave a clean upgrade seam:

- **Never returned to the client** — the settings DTO exposes only `hasSecretKey` / `hasAccessKey` booleans, never the secret values (same pattern as `hasFigmaAccessToken`).
- **Never logged** — secret fields are excluded from all logging and from `toString()`; connection-test and migration logs reference bucket/endpoint only.
- **Never committed** — demo/sample config uses LOCAL_FS only; no real credentials in `SampleDataInitializer` or fixtures.
- **Upgrade seam** — secret columns are isolated so a future `EncryptedStringConverter` (AES-GCM JPA `AttributeConverter`, keyed off an app secret) can be applied with no schema change. Tracked as a follow-up, out of scope here.

---

## Feature 2 — Pluggable Object Storage

### Goal
Route every attachment read/write through a backend-agnostic abstraction so a self-hoster can choose AWS S3, MinIO, or local filesystem (and add more later) via config + admin UI. No controller or service may call a storage SDK directly — same discipline as the LLM / vector-store abstractions.

### Existing state (grounding)
- `TaskAttachment` entity (`task_attachments`) stores file **metadata + a relative disk path**; bytes live on local disk under `app.upload.dir` (default `uploads/`). No DB blobs.
- `TaskAttachmentService` is the single touch-point for disk I/O: `uploadAttachment`, `getAttachments`, `downloadAttachment` (returns a `DownloadResult(Resource, originalFileName, contentType)` via Spring `UrlResource`), `deleteAttachment`.
- `TaskController` exposes upload/list/download/delete under `/api/tasks/{id}/attachments...` with `@PreAuthorize("hasPermission('BACKLOG', ...)")`. Downloads stream through the authed endpoint (JWT header), **not** a public `<a href>`.
- No presigned-URL or cloud-storage code exists today.

### SPI (mirrors `KnowledgeSourceProvider`)

```
service/storage/
  ObjectStorageProvider.java        (interface)
  ObjectStorageRegistry.java        (built from List<ObjectStorageProvider>, byType map)
  ObjectStorageService.java         (façade — resolves ACTIVE provider; the ONLY thing callers touch)
  StorageProviderType.java          (enum: LOCAL_FS, S3, MINIO)
  StorePutContext.java              (@Value/@Builder: inputStream, contentType, size, originalFilename, keyHint)
  StoredObjectRef.java              (@Value: storageProvider, storageKey, size)
  DownloadResource.java             (@Value: Resource resource, contentType, size)
  provider/
    LocalFsStorageProvider.java     (wraps today's disk logic; DEFAULT)
    S3StorageProvider.java          ─┐ share an abstract AwsS3BaseStorageProvider
    MinioStorageProvider.java       ─┘ (AWS SDK v2 S3Client; MinIO = custom endpoint + path-style access)
```

Interface:

```java
public interface ObjectStorageProvider {
  StorageProviderType getType();
  void validateConfig(JsonNode config) throws InvalidConfigException;
  default ConnectionStatus testConnection(JsonNode config) { return ConnectionStatus.ok(); }
  StoredObjectRef store(StorePutContext ctx);          // returns provider + key
  DownloadResource retrieve(String storageKey);
  void delete(String storageKey);
  default Optional<URI> presignUrl(String storageKey, Duration ttl) { return Optional.empty(); }
}
```

- Auto-registration: each provider is a `@Component`; `ObjectStorageRegistry` receives `List<ObjectStorageProvider>` and maps by `getType()` (identical to `KnowledgeSourceRegistry`).
- `ObjectStorageService` reads the active provider from `StorageConfig`, delegates store/retrieve/delete, and centralizes key generation (`{entityType}/{uuid}_{sanitizedName}`), size/content-type validation, and the existing 10 MB / MIME-whitelist guards (lifted out of `TaskAttachmentService`).
- AWS SDK v2 (`software.amazon.awssdk:s3`) added to `backend/pom.xml`. Presigned URLs implemented for S3/MINIO; LOCAL_FS returns empty (callers fall back to the authed streaming endpoint, which still works for all backends).

### Persistence
- New `StorageConfig` entity / `storage_config` table — effectively singleton per org (one active config), soft-delete, fields: `active_provider` (enum string), `config` (TEXT JSON: `bucket`, `endpoint`, `region`, `accessKey`, `secretKey`, `pathStyleAccess`), `createdAt`/`updatedAt`/`deletedAt`. Secret fields plaintext per Decision #3.
- `TaskAttachment` gains `storage_provider` (enum string, default `LOCAL_FS`) and `storage_key` (TEXT) columns. Existing rows backfilled to `LOCAL_FS` + their current path as the key, so mixed-backend serving works during/after migration.
- Flyway: `V{next}__add_object_storage.sql` (scan for highest `V{N}` first). H2-safe DDL only (`BIGINT GENERATED ALWAYS AS IDENTITY`, `TEXT`, `VARCHAR`, `BOOLEAN`, `TIMESTAMP WITH TIME ZONE`, `CREATE INDEX IF NOT EXISTS`). FK `fk_*`, index `idx_*` naming.

### Routing & migration
- `TaskAttachmentService` refactored to call `ObjectStorageService` for all bytes I/O. Each attachment records the backend + key that stored it, so downloads resolve the correct provider per object. No SDK type escapes `service/storage/`.
- `StorageMigrationService` + admin-triggered endpoint (`POST /api/admin/storage/migrate`, `@PreAuthorize("hasRole('ADMIN')")`):
  - Streams each object from its recorded backend into the newly-activated backend, rewrites `storage_provider` + `storage_key`.
  - **Idempotent / resumable**: skips rows already on the target backend.
  - **Copy-verified-before-source-untouched**: never deletes the source object until the destination write is confirmed; soft-fails per object and reports counts (migrated / skipped / failed). Soft-delete discipline preserved — never hard-delete user data.

### Admin UI
- New **"Storage"** tab in `frontend/src/pages/OrganizationSettings.tsx` (the page already has a tabbed model — General, Email, SSO, SCIM, Plugins, …). React Hook Form + Zod, mirroring `SsoSettingsTab.tsx`.
- Controls: select active backend (LOCAL_FS / S3 / MINIO); connection fields (bucket, endpoint, region, access key, secret key, path-style toggle) shown conditionally per backend; **Test Connection** button (modeled on `EmailSettingsTab.handleSendTestEmail` → `POST /api/admin/storage/test`); **Migrate existing files** action with progress/result summary.
- Secret fields render write-only (blank shows "configured" via `hasSecretKey`); never pre-filled with the stored value.
- New typed `frontend/src/services/storageService.ts` (Axios, project service style). Backend `StorageConfigController` under `/api/admin/storage`, DTOs at the boundary, `@PreAuthorize("hasRole('ADMIN')")`.

---

## Feature 1 — Wiki / Docs Space (consumes the storage SPI)

### Goal
A built-in wiki so teams write/read docs without Confluence/Notion: a tree of **spaces → folders → pages** with a Notion-style block editor, history, search, mentions, internal links, attachments, and AI ingestion.

### RBAC grounding
- Current role model (per `PERMISSION_MATRIX.md`): **`ADMIN`, `MANAGER`, `MEMBER`, `READONLY`** (CLAUDE.md still lists the older 6-role names; we target the current 4). No per-resource ACLs exist today — permissions are role × resource-type. "Space-level read/write" is therefore a **new** concept, introduced via `WikiSpacePermission`.

### Data model
- **`WikiSpace`** — `id`, `name`, `key` (short slug, unique), `description`, `project_id` (nullable — optional project link), `createdBy`, timestamps, `deletedAt`. **Envers-audited.**
- **`WikiPage`** — self-referencing tree:
  - `id`, `space_id`, `parent_id` (nullable; null = top-level; "folders" are simply pages with children), `title`, `slug`,
  - `content` (TEXT = BlockNote document JSON),
  - `content_text` (TEXT = derived plaintext, maintained on save — powers search + knowledge ingestion),
  - `position` (INT — sibling ordering for drag-to-reorder),
  - `createdBy`, timestamps, `deletedAt`. **Envers-audited** → page history + restore.
- **`WikiSpacePermission`** — `space_id`, `grantee_type` (ROLE | USER), `grantee_ref` (role name or userId), `level` (READ | WRITE). The per-space ACL.
- **`WikiAttachment`** — `page_id`, `storage_provider`, `storage_key`, `fileName`, `contentType`, `size`, `uploadedBy`, timestamps. **All bytes via `ObjectStorageService`** (never disk-direct).
- Flyway: `V{next+1}__add_wiki.sql`, H2-safe, after the storage migration. Indexes on `space_id`, `parent_id`, `(space_id, parent_id, position)`, trigram-friendly index strategy for search consistent with `GlobalSearchService`.

### Permission resolution — `WikiPermissionService`
Effective access for (user, space):
1. `ADMIN` → full access always.
2. Explicit `WikiSpacePermission` grant (USER match, then ROLE match) → READ/WRITE.
3. Else, if the space has `project_id` → fall back to project membership (project members read; project write-roles write).
4. Else → deny (fail-closed).
Controllers combine `@PreAuthorize` (coarse, e.g. authenticated + `WIKI` resource) with `WikiPermissionService` checks (fine, per-space) in the service layer.

### Backend
- `WikiService` — tree CRUD; reorder/reparent with **cycle prevention** (a page cannot become its own descendant) and `position` re-sequencing; restore-from-revision via the Envers `AuditReader`.
- `WikiController` — DTOs at boundary, `@PreAuthorize` + `WikiPermissionService`. Endpoints: space CRUD + ACL management, page CRUD, move/reorder, list-tree, get-history, restore-revision, attachment upload/list/download/delete (delegating to `ObjectStorageService`).
- **Mentions**: reuse `CommentService` mention regex + `notificationService.notifyCommentMention(...)`; add a `WIKI_PAGE` value to the comment/notification entity-type enum.
- **Search**: extend `GlobalSearchService`'s native trigram + LIKE union with a wiki branch (trigram on `title` + `content_text`, scoped/filtered by space and permission), returning a `/wiki/{spaceKey}/{pageId}` route.
- **Internal page links**: server provides a page-search endpoint for the editor's link autocomplete; links stored as BlockNote nodes referencing `pageId` (resolve to current slug on render).

### Knowledge tie-in — `WikiProvider`
- `service/knowledge/source/provider/WikiProvider.java implements KnowledgeSourceProvider`:
  - `getType() = KnowledgeProviderType.WIKI` (add enum value),
  - `validateConfig(JsonNode)` — validates the referenced space exists,
  - `ingest(...)` — chunks each page's `content_text` (reuse the ~1200-char/150-overlap splitter pattern) into `RawChunk`s with page title + `/wiki/...` source URL,
  - `supportsRefresh() = true` (re-ingest on page changes / scheduled refresh).
- Chunks flow through `KnowledgeIngestionService.ingestChunks(...)` → embeddings → Q&A / Wise Architecture / test-gen / risk analysis, exactly like other sources.
- Add i18n label `provider.WIKI` to `en.json` + `fa.json`.

### Frontend
- **`WikiTree`** sidebar component — the spaces→folders→pages tree with **drag-to-reorder / reparent** (optimistic + React Query mutation to the move endpoint).
- **`WikiSpace.tsx`** — space landing (page list, space settings/ACL for those with WRITE).
- **`WikiPage.tsx`** — BlockNote editor (headings, lists, checkboxes, tables, code blocks, callouts, image/file embeds via `storageService`, slash-command menu) + **breadcrumbs** + auto **table of contents** + **page history / restore** panel.
- Internal-link autocomplete; @mention autocomplete (reuse existing mention search endpoint).
- Typed `frontend/src/services/wikiService.ts` (Axios), React Query for all server state.
- Sidebar nav entry in `Layout.tsx` (`NavItemConfig`, `BookOpen`-style icon, `/wiki` route, `data-tour="wiki-link"`), `nav.wiki` i18n key.

---

## Cross-cutting / end-of-session checklist (applies to BOTH features, one PR)

1. `CHANGELOG.md` — entries under `[Unreleased]` for both features.
2. `README.md` — features list + comparison table row; refresh screenshots if UI shown.
3. `COMPETITOR_ANALYSIS.md` — wiki closes a Confluence/Notion gap; pluggable storage a self-hosting gap.
4. **Public pages in lockstep**: add the **v1.9.0** milestone to BOTH `ReleaseNotes.tsx` (version card, drop `upcoming` when shipped) and `PublicRoadmap.tsx` (`recentlyShipped` / `upcomingPhases` + i18n `shipped190*` keys). Highlights: Wiki/Docs Space + Pluggable Object Storage. Titles/counts/status identical across both.
5. i18n keys in **both** `en.json` AND `fa.json` (`nav.wiki`, `provider.WIKI`, storage tab, wiki UI, roadmap keys).
6. Help guides — `{NN}-wiki.md` and `{NN+1}-object-storage.md` in `backend/src/main/resources/knowledgebase/help-guides/` (auto-loaded by `HelpGuideAIService`).
7. `SampleDataInitializer.java` — a demo wiki space with nested pages + a `LOCAL_FS` `StorageConfig` (no real credentials).
8. Tour: update `TourContext.tsx` selectors + Step Inventory table in `TOUR_GUIDE.md` if layout changed (new `data-tour="wiki-link"`).
9. Tests: ≥80% line coverage, **full suite 0 failures**. Unit tests for both registries/services (no Spring context, like `McpToolDispatcherTest`), `WikiPermissionService`, migration idempotency, provider `validateConfig`, cycle-prevention on reparent.
10. `./mvnw spotless:apply && ./mvnw verify` → `BUILD SUCCESS, Failures: 0, Errors: 0`. Frontend `npm test` + `npm run build`.
11. PR to `main`, `feat/` prefix, title reflecting full scope.

## Build order
1. Object Storage: SPI + registry + `ObjectStorageService` + `LocalFsStorageProvider` (refactor `TaskAttachmentService` onto it, behavior-preserving) → `S3`/`MinIO` providers → `StorageConfig` + migration `V{next}` → admin Storage tab + `StorageMigrationService`.
2. Wiki: migration `V{next+1}` + entities → `WikiPermissionService` + `WikiService` + `WikiController` → `WikiProvider` (knowledge) → search/mention extensions → frontend (tree, space, page editor, nav).
3. Cross-cutting checklist + public-page sync + PR.

## Out of scope (this milestone)
- At-rest encryption of storage credentials (seam left; tracked follow-up).
- Generalizing `TaskAttachment` + `WikiAttachment` into one polymorphic table (kept separate to avoid churning the working task path).
- Real-time collaborative co-editing (that is v1.13.0).
- Public/anonymous wiki sharing, page comments thread UI beyond mentions, export-to-PDF.
