# Wiki / Docs Space + Pluggable Object Storage — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship v1.9.0 — a pluggable object-storage abstraction (LOCAL_FS / S3 / MinIO) and a built-in Notion/Confluence-style wiki whose pages auto-feed the Knowledge Center.

**Architecture:** Object storage lands first as an SPI mirroring the Knowledge Center provider pattern (`@Component` beans + registry built from `List<Provider>`, JSON config in a TEXT column, an `ObjectStorageService` façade that is the only thing callers touch). `TaskAttachmentService` is refactored onto the façade behavior-preservingly. The wiki then builds spaces→folders→pages (`WikiPage` self-referencing tree, Envers-audited), with a per-page event-driven Knowledge Center sync via a new `WikiProvider` + `WikiKnowledgeListener`.

**Tech Stack:** Spring Boot 3.4.7 / Java 21 / JPA + Hibernate Envers / Flyway / PostgreSQL (H2 in tests) / AWS SDK v2 S3 / React 18 + TypeScript + Vite / BlockNote editor / React Query / React Hook Form + Zod / Tailwind / i18next.

## Global Constraints

- Branch: `feat/wiki-and-object-storage` (already created, off `feat/knowledge-center`). PR stacks on knowledge-center; retarget `main` once KC merges.
- Layering: Controller → Service → Repository. DTOs only at controller boundary. No `@Transactional` on controllers.
- Every new controller method has `@PreAuthorize`. Current roles: `ADMIN`, `MANAGER`, `MEMBER`, `READONLY`.
- Soft-delete only (`deletedAt`); never hard-delete user data.
- No storage SDK call may escape `service/storage/` (same discipline as LLM/vector-store abstractions).
- Storage secrets: stored plaintext TEXT (recorded decision), but NEVER returned in any DTO (expose `hasSecretKey`/`hasAccessKey` booleans only) and NEVER logged.
- Flyway: sequential `V{N}__desc.sql`. Next numbers: **V110** (storage), **V111** (wiki), **V112** (wiki RBAC seed). Never edit existing migrations. H2-safe DDL only (`BIGINT GENERATED ALWAYS AS IDENTITY`, `TEXT`, `VARCHAR`, `BOOLEAN`, `TIMESTAMP WITH TIME ZONE`, `CREATE INDEX IF NOT EXISTS`). New `@Audited` entities need their `_aud` table DDL in the migration too.
- i18n keys in BOTH `frontend/src/i18n/locales/en.json` AND `fa.json`.
- AWS SDK v2: add BOM `software.amazon.awssdk:bom:2.31.78` to `<dependencyManagement>`, then unversioned `software.amazon.awssdk:s3`.
- BlockNote: add `@blocknote/core`, `@blocknote/react`, `@blocknote/mantine` to `frontend/package.json`.
- Help guides: **`14-wiki.md`**, **`15-object-storage.md`** in `backend/src/main/resources/knowledgebase/help-guides/` (auto-loaded by `HelpGuideAIService`).
- After backend changes: `cd backend && ./mvnw spotless:apply && ./mvnw verify` → `BUILD SUCCESS, Failures: 0, Errors: 0` (JaCoCo 80%). After frontend: `cd frontend && npm test && npm run build`.
- **Test profile reality (verified):** the test profile DISABLES Flyway and uses H2 `create-drop` from entities. Migrations never run in `verify`, so Postgres-only trigram DDL is safe from the suite — but the real wiki search SQL has NO automated coverage and MUST be verified manually on Postgres (noted per-task).

---

## File Map

### Feature 2 — Object Storage (backend)
- Create `backend/.../service/storage/StorageProviderType.java` — enum `LOCAL_FS, S3, MINIO`.
- Create `backend/.../service/storage/ObjectStorageProvider.java` — SPI interface.
- Create `backend/.../service/storage/StorePutContext.java` — `@Value @Builder`: inputStream, contentType, size, originalFilename, keyHint.
- Create `backend/.../service/storage/StoredObjectRef.java` — `@Value`: storageProvider, storageKey, size.
- Create `backend/.../service/storage/DownloadResource.java` — `@Value`: resource (Spring `Resource`), contentType, size.
- Create `backend/.../service/storage/ObjectStorageRegistry.java` — `@Component`, byType map from `List<ObjectStorageProvider>`.
- Create `backend/.../service/storage/ObjectStorageService.java` — façade resolving active provider + key gen + validation.
- Create `backend/.../service/storage/provider/LocalFsStorageProvider.java` — wraps today's disk logic; default.
- Create `backend/.../service/storage/provider/AwsS3BaseStorageProvider.java` — abstract S3Client-based store/retrieve/delete/presign.
- Create `backend/.../service/storage/provider/S3StorageProvider.java` — AWS endpoint.
- Create `backend/.../service/storage/provider/MinioStorageProvider.java` — custom endpoint + path-style.
- Create `backend/.../entity/StorageConfig.java` — active provider + JSON config (singleton per org), soft-delete.
- Create `backend/.../repository/StorageConfigRepository.java`.
- Create `backend/.../service/StorageConfigService.java` — get/update active config, test-connection delegation.
- Create `backend/.../service/StorageMigrationService.java` — idempotent backfill across backends.
- Create `backend/.../controller/StorageConfigController.java` — `/api/admin/storage`, admin-only.
- Create `backend/.../dto/storage/*` — `StorageConfigDTO`, `UpdateStorageConfigRequest`, `ConnectionTestResponse`, `MigrationResultDTO`.
- Modify `backend/.../entity/TaskAttachment.java` — add `storageProvider`, `storageKey`.
- Modify `backend/.../service/TaskAttachmentService.java` — route through `ObjectStorageService`.
- Modify `backend/pom.xml` — AWS SDK v2 BOM + s3.
- Create `backend/src/main/resources/db/migration/V110__add_object_storage.sql`.

### Feature 2 — Object Storage (frontend)
- Create `frontend/src/services/storageService.ts`.
- Create `frontend/src/components/organizationSettings/StorageSettingsTab.tsx`.
- Modify `frontend/src/pages/OrganizationSettings.tsx` — register the Storage tab.

### Feature 1 — Wiki (backend)
- Create entities: `WikiSpace.java`, `WikiPage.java`, `WikiSpacePermission.java`, `WikiAttachment.java` (all in `entity/`).
- Create repositories: `WikiSpaceRepository`, `WikiPageRepository`, `WikiSpacePermissionRepository`, `WikiAttachmentRepository`.
- Create `backend/.../service/WikiPermissionService.java` — effective access resolution.
- Create `backend/.../service/WikiService.java` — tree CRUD, reorder/reparent (cycle-safe), history/restore.
- Create `backend/.../controller/WikiController.java` — `/api/wiki/...`.
- Create `backend/.../dto/wiki/*` — space/page/permission/attachment DTOs + requests + tree node.
- Create `backend/.../service/knowledge/source/provider/WikiProvider.java` — KC provider (type `WIKI`).
- Create `backend/.../event/WikiPageChangedEvent.java` (record) + `backend/.../service/knowledge/WikiKnowledgeListener.java`.
- Modify `backend/.../entity/enums/KnowledgeProviderType.java` — add `WIKI`.
- Modify `backend/.../entity/enums/KnowledgeEntityType.java` — add `WIKI_PAGE`.
- Modify `backend/.../service/GlobalSearchService.java` — add wiki branch to the UNION.
- Modify `backend/.../service/SampleDataInitializer.java` — demo space/pages + LOCAL_FS StorageConfig.
- Create `backend/src/main/resources/db/migration/V111__add_wiki.sql` and `V112__add_wiki_resource_permissions.sql`.

### Feature 1 — Wiki (frontend)
- Create `frontend/src/services/wikiService.ts`.
- Create `frontend/src/components/wiki/WikiTree.tsx` (drag reorder/reparent).
- Create `frontend/src/components/wiki/WikiEditor.tsx` (BlockNote wrapper), `WikiBreadcrumbs.tsx`, `WikiTableOfContents.tsx`, `WikiHistoryPanel.tsx`.
- Create `frontend/src/pages/WikiSpace.tsx`, `frontend/src/pages/WikiPage.tsx`.
- Modify `frontend/src/components/Layout.tsx` — nav entry (`BookOpen`, `/wiki`, `data-tour="wiki-link"`).
- Modify `frontend/src/App.tsx` (or router) — `/wiki` routes.

### Cross-cutting (docs / public pages / i18n)
- Modify `CHANGELOG.md`, `README.md`, `COMPETITOR_ANALYSIS.md`.
- Modify `frontend/src/pages/ReleaseNotes.tsx` + `frontend/src/pages/PublicRoadmap.tsx` (v1.9.0, lockstep).
- Create `backend/.../knowledgebase/help-guides/14-wiki.md`, `15-object-storage.md`.
- Modify `frontend/src/contexts/TourContext.tsx` + `TOUR_GUIDE.md` (new `wiki-link` step).
- Modify `frontend/src/i18n/locales/en.json` + `fa.json`.

---

## Interface Contracts (authoritative signatures)

```java
// service/storage/ObjectStorageProvider.java
public interface ObjectStorageProvider {
  StorageProviderType getType();
  void validateConfig(JsonNode config);                       // throws InvalidConfigException
  default ConnectionStatus testConnection(JsonNode config) { return ConnectionStatus.ok(); }
  StoredObjectRef store(JsonNode config, StorePutContext ctx);
  DownloadResource retrieve(JsonNode config, String storageKey);
  void delete(JsonNode config, String storageKey);
  default Optional<URI> presignUrl(JsonNode config, String storageKey, Duration ttl) { return Optional.empty(); }
}

// service/storage/ObjectStorageService.java (façade — callers use ONLY this)
public StoredObjectRef store(StorePutContext ctx);            // resolves active provider+config
public DownloadResource retrieve(String storageProvider, String storageKey);
public void delete(String storageProvider, String storageKey);
public Optional<URI> presignUrl(String storageProvider, String storageKey, Duration ttl);
public StorageProviderType activeProvider();

// service/storage/ObjectStorageRegistry.java
public ObjectStorageProvider get(StorageProviderType type);  // IllegalStateException if absent
public boolean isAvailable(StorageProviderType type);

// entity/StorageConfig.java fields
Long id; StorageProviderType activeProvider; String config /*TEXT JSON*/;
OffsetDateTime createdAt, updatedAt, deletedAt;

// service/StorageMigrationService.java
public MigrationResultDTO migrateToActiveBackend();          // idempotent; counts migrated/skipped/failed

// service/WikiPermissionService.java
public boolean canRead(Long userId, WikiSpace space);
public boolean canWrite(Long userId, WikiSpace space);
public void requireRead(Long userId, Long spaceId);          // throws AccessDeniedException
public void requireWrite(Long userId, Long spaceId);

// service/WikiService.java
public WikiSpaceDTO createSpace(CreateWikiSpaceRequest req, Long userId);
public List<WikiTreeNodeDTO> getTree(Long spaceId, Long userId);
public WikiPageDTO createPage(CreateWikiPageRequest req, Long userId);  // publishes WikiPageChangedEvent(CREATED)
public WikiPageDTO updatePage(Long pageId, UpdateWikiPageRequest req, Long userId); // event UPDATED
public void movePage(Long pageId, MovePageRequest req, Long userId);    // cycle-checked, re-sequences position
public void deletePage(Long pageId, Long userId);                      // soft-delete; event DELETED
public List<WikiRevisionDTO> getHistory(Long pageId, Long userId);     // via Envers AuditReader
public WikiPageDTO restoreRevision(Long pageId, int revision, Long userId);

// event/WikiPageChangedEvent.java
public record WikiPageChangedEvent(Long pageId, Long spaceId, ChangeType type) {}  // CREATED|UPDATED|DELETED|RESTORED

// service/knowledge/WikiKnowledgeListener.java
@Async @EventListener void onPageChanged(WikiPageChangedEvent e);      // ingests/removes chunks for that page

// service/knowledge/source/provider/WikiProvider.java
KnowledgeProviderType getType();                  // WIKI
void validateConfig(JsonNode);                    // requires spaceId, space exists
IngestionResult ingest(KnowledgeSource, IngestionContext); // bulk walk of space pages
boolean supportsRefresh();                        // true
```

### REST endpoints
Storage (`StorageConfigController`, all `@PreAuthorize("hasRole('ADMIN')")`):
- `GET /api/admin/storage` → `StorageConfigDTO`
- `PUT /api/admin/storage` → update active provider + config
- `POST /api/admin/storage/test` → `ConnectionTestResponse`
- `POST /api/admin/storage/migrate` → `MigrationResultDTO`

Wiki (`WikiController`, `@PreAuthorize("isAuthenticated()")` + `WikiPermissionService` checks in-service):
- `POST /api/wiki/spaces`, `GET /api/wiki/spaces`, `GET /api/wiki/spaces/{id}`, `PUT /api/wiki/spaces/{id}`, `DELETE /api/wiki/spaces/{id}`
- `GET /api/wiki/spaces/{id}/tree`
- `GET/POST /api/wiki/spaces/{id}/permissions`, `DELETE /api/wiki/permissions/{permId}`
- `POST /api/wiki/pages`, `GET /api/wiki/pages/{id}`, `PUT /api/wiki/pages/{id}`, `DELETE /api/wiki/pages/{id}`
- `POST /api/wiki/pages/{id}/move`
- `GET /api/wiki/pages/{id}/history`, `POST /api/wiki/pages/{id}/restore/{revision}`
- `POST /api/wiki/pages/{id}/attachments`, `GET /api/wiki/pages/{id}/attachments`, `GET /api/wiki/attachments/{attId}/download`, `DELETE /api/wiki/attachments/{attId}`
- `GET /api/wiki/pages/search?q=` (internal-link autocomplete)

---

## Risks & Ordering Notes

- **R1 (highest): T5 `TaskAttachmentService` refactor must be behavior-preserving.** Existing `TaskAttachment*` tests are the regression gate. Keep exception types/messages, the 10 MB + MIME whitelist, and the `DownloadResult` shape identical, or update those tests in the same commit. Run the existing attachment tests before and after.
- **R2: wiki search SQL has no suite coverage** (test profile disables Flyway, `GlobalSearchServiceTest` mocks the EntityManager). Write the wiki branch to match the existing trigram+LIKE UNION, but verify manually on Postgres. Do NOT add a test that would require trigram on H2.
- **R3: new `@Audited` entities** (`WikiSpace`, `WikiPage`) need explicit `_aud` table DDL in `V111` for dev/prod; H2 create-drop generates them automatically for tests.
- **R4: AWS SDK in tests must not hit network.** S3/MinIO provider unit tests inject a mocked `S3Client` (Mockito) or use the SDK's request/response stubs — never a real endpoint. The default test path stays LOCAL_FS.
- **R5: mentions** — `notifyCommentMention(user, author, String entityType, entityId, content)` already takes a `String` entityType, so wiki passes `"WIKI_PAGE"` directly. Do NOT modify `CommentEntityType`/`CommentService.validateEntityExists` (avoids comment-path regression).
- **Ordering:** storage T1–T8 before wiki T9–T16 (wiki attachments consume the façade; `WikiProvider` consumes the merged KC SPI on this branch).

## Test Strategy
- **Pure unit (no Spring context, like `McpToolDispatcherTest`):** `ObjectStorageRegistry`, each provider's `validateConfig`, `LocalFsStorageProvider` store/retrieve/delete against a JUnit `@TempDir`, S3/MinIO providers with a mocked `S3Client`, `WikiPermissionService` resolution matrix, `WikiService` cycle-prevention on reparent, `StorageMigrationService` idempotency (LOCAL_FS→LOCAL_FS no-op + a fake second provider).
- **Spring `@DataJpaTest`/slice:** repository queries, Envers history read for `WikiPage`.
- **Spring `@SpringBootTest` (H2):** `WikiController` happy-path + permission denial; `StorageConfigController` admin gate; per-page `WikiKnowledgeListener` ingest/remove (assert `ingestChunks` invoked with `WIKI_PAGE` + pageId).
- **Manual on Postgres:** wiki global-search trigram results.

---

## Tasks

### Task 1: Object-storage SPI types + registry

**Files:**
- Create: `service/storage/StorageProviderType.java`, `ObjectStorageProvider.java`, `StorePutContext.java`, `StoredObjectRef.java`, `DownloadResource.java`, `ObjectStorageRegistry.java`
- Test: `src/test/java/.../service/storage/ObjectStorageRegistryTest.java`

**Interfaces:**
- Produces: the SPI interface + `ObjectStorageRegistry.get(type)`/`isAvailable(type)` (signatures above).
- Consumes: nothing (reuses `ConnectionStatus`/`InvalidConfigException` from the KC SPI package).

- [ ] **Step 1: Write the failing test** — a fake provider returning `LOCAL_FS`; assert `registry.get(LOCAL_FS)` returns it and `get(S3)` throws `IllegalStateException`.
- [ ] **Step 2: Run** `./mvnw -pl backend test -Dtest=ObjectStorageRegistryTest` → FAIL (classes missing).
- [ ] **Step 3:** Implement the enum, interface (default `testConnection`/`presignUrl`), the three value types (`@Value`/`@Builder`), and the registry (mirror `KnowledgeSourceRegistry`).
- [ ] **Step 4: Run** the test → PASS.
- [ ] **Step 5: Commit** `feat(storage): object-storage SPI types + registry`.

### Task 2: LocalFsStorageProvider

**Files:** Create `service/storage/provider/LocalFsStorageProvider.java`; Test `LocalFsStorageProviderTest.java`.
**Interfaces:** Produces `LocalFsStorageProvider` (`getType()=LOCAL_FS`). Consumes Task 1 SPI.

- [ ] **Step 1:** Failing test — using `@TempDir`, `store()` a byte stream then `retrieve()` returns identical bytes; `delete()` removes it; `validateConfig` accepts `{}` (uses `app.upload.dir`).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3:** Implement using `Files.copy`/`UrlResource` (lift logic from current `TaskAttachmentService`); key = `{keyHint}/{uuid}_{sanitized}`; `presignUrl` returns empty.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(storage): local filesystem provider`.

### Task 3: StorageConfig entity + repository + V110 migration

**Files:** Create `entity/StorageConfig.java`, `repository/StorageConfigRepository.java`, `db/migration/V110__add_object_storage.sql`; modify `entity/TaskAttachment.java` (add `storageProvider`, `storageKey`); Test `StorageConfigRepositoryTest.java` (`@DataJpaTest`).
**Interfaces:** Produces `StorageConfig` entity + repo `findFirstByDeletedAtIsNull()`. Consumes Task 1 enum.

- [ ] **Step 1:** Failing `@DataJpaTest` — save a `StorageConfig(LOCAL_FS, "{}")`, read it back.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3:** Create entity (soft-delete, timestamps), repo; add the two `TaskAttachment` columns (default `LOCAL_FS`); write V110 adding `storage_config` + the two `task_attachments` columns + backfill `UPDATE task_attachments SET storage_provider='LOCAL_FS', storage_key=file_path WHERE storage_key IS NULL` (H2-safe).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(storage): StorageConfig entity + V110 migration`.

### Task 4: ObjectStorageService façade + StorageConfigService

**Files:** Create `service/storage/ObjectStorageService.java`, `service/StorageConfigService.java`; Test `ObjectStorageServiceTest.java`.
**Interfaces:** Produces façade methods (above) + `StorageConfigService.getActiveConfig()/updateConfig()/testConnection()`. Consumes Tasks 1–3.

- [ ] **Step 1:** Failing test — with LOCAL_FS active (config from `StorageConfigService`), `store()` then `retrieve()` round-trips; centralized 10 MB + MIME guard rejects oversize/bad type with the same exception the old service used.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3:** Implement façade (resolve active provider via registry, pass parsed `JsonNode` config) and `StorageConfigService` (get-or-create default LOCAL_FS, update, delegate `testConnection`).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(storage): ObjectStorageService façade + config service`.

### Task 5: Refactor TaskAttachmentService onto the façade (behavior-preserving)

**Files:** Modify `service/TaskAttachmentService.java`; Test: run existing `TaskAttachmentServiceTest` + `TaskController` attachment tests.
**Interfaces:** Consumes Task 4 façade. Produces no new public API (signatures unchanged).

- [ ] **Step 1:** Run existing attachment tests → baseline PASS (record names).
- [ ] **Step 2:** Replace direct disk I/O with `objectStorageService.store/retrieve/delete`; persist `storageProvider`/`storageKey` on `TaskAttachment`; keep all validation, exception types/messages, and `DownloadResult` identical.
- [ ] **Step 3: Run** the same tests → PASS unchanged. If any assertion legitimately shifts, update it in THIS commit.
- [ ] **Step 4: Run** `./mvnw -pl backend test` (attachment + controller slices) → 0 failures.
- [ ] **Step 5: Commit** `refactor(storage): route task attachments through ObjectStorageService`.

### Task 6: S3 + MinIO providers (AWS SDK v2)

**Files:** Modify `backend/pom.xml`; Create `provider/AwsS3BaseStorageProvider.java`, `S3StorageProvider.java`, `MinioStorageProvider.java`; Test `S3StorageProviderTest.java` (mocked `S3Client`).
**Interfaces:** Produces both providers (`getType()` S3/MINIO). Consumes Task 1 SPI.

- [ ] **Step 1:** Add AWS SDK BOM 2.31.78 + `s3` to pom; failing test injecting a Mockito `S3Client`, asserting `store()` issues a `PutObjectRequest` with the right bucket/key and `retrieve()` reads `GetObjectRequest`; `validateConfig` requires bucket+region (S3) / bucket+endpoint (MinIO).
- [ ] **Step 2: Run** `-Dtest=S3StorageProviderTest` → FAIL.
- [ ] **Step 3:** Implement abstract base building an `S3Client` from config (MinIO sets `endpointOverride` + `pathStyleAccessEnabled(true)`); `presignUrl` via `S3Presigner`; `testConnection` does a `headBucket`. No network in tests.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(storage): S3 + MinIO providers via AWS SDK v2`.

### Task 7: StorageMigrationService + StorageConfigController

**Files:** Create `service/StorageMigrationService.java`, `controller/StorageConfigController.java`, `dto/storage/*`; Test `StorageMigrationServiceTest.java`, `StorageConfigControllerTest.java`.
**Interfaces:** Produces `migrateToActiveBackend()` + the 4 REST endpoints. Consumes Tasks 3,4,6.

- [ ] **Step 1:** Failing tests — migration skips rows already on the active backend (idempotent, returns skipped count) and copies others, never deleting source before destination write confirmed; controller GET/PUT/test/migrate require `hasRole('ADMIN')` and never echo secret values.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3:** Implement migration (iterate `TaskAttachment` + `WikiAttachment` once it exists — guard for null repo via separate method called in Task 14), controller + DTOs (`hasSecretKey`/`hasAccessKey` projection, never the raw secret).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(storage): migration service + admin storage controller`.

### Task 8: Storage admin UI

**Files:** Create `frontend/src/services/storageService.ts`, `components/organizationSettings/StorageSettingsTab.tsx`; Modify `pages/OrganizationSettings.tsx`; Test `storageService` + tab render test (Vitest).
**Interfaces:** Consumes Task 7 endpoints.

- [ ] **Step 1:** Failing Vitest — tab renders backend select + conditional fields, Test Connection calls `/admin/storage/test`, Migrate calls `/admin/storage/migrate`; secret field shows "configured" when `hasSecretKey` and never pre-fills the value.
- [ ] **Step 2: Run** `npm test -- StorageSettingsTab` → FAIL.
- [ ] **Step 3:** Implement typed service + RHF/Zod tab (mirror `SsoSettingsTab`), register in the tabs array; add i18n keys (en+fa).
- [ ] **Step 4: Run** → PASS; `npm run build`.
- [ ] **Step 5: Commit** `feat(storage): admin Storage settings tab`.

### Task 9: Wiki entities + V111/V112 migrations

**Files:** Create `entity/WikiSpace.java` (`@Audited`), `WikiPage.java` (`@Audited`), `WikiSpacePermission.java`, `WikiAttachment.java`; repositories; `db/migration/V111__add_wiki.sql` (+ `_aud` tables), `V112__add_wiki_resource_permissions.sql` (seed `WIKI` resource rows for the role×permission matrix); Test `WikiPageRepositoryTest.java`.
**Interfaces:** Produces the four entities + repos (`findBySpaceIdAndParentIdOrderByPosition`, etc.).

- [ ] **Step 1:** Failing `@DataJpaTest` — save a space + nested pages (`parent_id`), read children ordered by `position`; save a `WikiSpacePermission`.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3:** Create entities (soft-delete, `content` TEXT, `content_text` TEXT, `position` INT, `slug`), repos; write H2-safe V111 with indexes (`idx_wiki_pages_space_parent_position`) + Envers `_aud` tables; V112 seeds `WIKI` permission rows.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(wiki): entities + V111/V112 migrations`.

### Task 10: WikiPermissionService

**Files:** Create `service/WikiPermissionService.java`; Test `WikiPermissionServiceTest.java` (pure unit, mocked repos + `PermissionService`).
**Interfaces:** Produces `canRead/canWrite/requireRead/requireWrite`. Consumes Task 9 repos.

- [ ] **Step 1:** Failing matrix test — ADMIN always; explicit USER grant; ROLE grant; project-link fallback; otherwise deny.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3:** Implement resolution order (ADMIN → user ACL → role ACL → project membership fallback → deny); `require*` throw `AccessDeniedException`.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(wiki): per-space permission service`.

### Task 11: WikiService (tree CRUD + reorder/reparent + history)

**Files:** Create `service/WikiService.java`, `event/WikiPageChangedEvent.java`, `dto/wiki/*`; Test `WikiServiceTest.java`.
**Interfaces:** Produces `WikiService` methods + `WikiPageChangedEvent` (above). Consumes Tasks 9,10; publishes events via `ApplicationEventPublisher`.

- [ ] **Step 1:** Failing tests — `createPage` publishes `WikiPageChangedEvent(CREATED)`; `movePage` into own descendant throws (cycle prevention) and re-sequences siblings; `restoreRevision` reads Envers and writes current.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3:** Implement CRUD, `getTree` (build `WikiTreeNodeDTO` list), cycle-safe move, Envers `AuditReader` history/restore, `content_text` derivation on save, event publication, permission checks via Task 10.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(wiki): tree service with reorder, history, events`.

### Task 12: WikiController + wiki attachments

**Files:** Create `controller/WikiController.java`; Test `WikiControllerTest.java` (`@SpringBootTest`).
**Interfaces:** Produces the wiki REST endpoints (above). Consumes Tasks 4 (façade for attachments), 11.

- [ ] **Step 1:** Failing test — page CRUD happy path; a user without space access gets 403; attachment upload routes through `ObjectStorageService` and records provider/key.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3:** Implement controller (DTOs at boundary, `isAuthenticated()` + in-service permission checks), attachment endpoints delegating to the façade.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(wiki): REST controller + attachments`.

### Task 13: WikiProvider + per-page WikiKnowledgeListener

**Files:** Create `service/knowledge/source/provider/WikiProvider.java`, `service/knowledge/WikiKnowledgeListener.java`; modify `enums/KnowledgeProviderType.java` (+`WIKI`), `enums/KnowledgeEntityType.java` (+`WIKI_PAGE`); Test `WikiProviderTest.java`, `WikiKnowledgeListenerTest.java`.
**Interfaces:** Produces the provider + listener. Consumes Task 11 events + `KnowledgeIngestionService.ingestChunks`.

- [ ] **Step 1:** Failing tests — `WikiProvider.ingest` chunks a space's pages into `RawChunk`s with `/wiki/...` URLs; listener on `UPDATED` deletes prior `(WIKI_PAGE,pageId)` chunks then re-ingests; on `DELETED` removes them; empty pages skipped.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3:** Add enum values; implement provider (reuse ~1200/150 splitter) + `@Async @EventListener` listener calling `ingestChunks(..., WIKI_PAGE, pageId, ...)`; auto-create a backing `KnowledgeSource` on space creation.
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(wiki): Knowledge Center provider + per-page auto-ingest`.

### Task 14: Wiki search + migration coverage of WikiAttachment

**Files:** Modify `service/GlobalSearchService.java`, `service/StorageMigrationService.java` (add wiki-attachment pass); Test: extend with a mocked-EntityManager assertion that the wiki UNION branch is included.
**Interfaces:** Consumes Tasks 7,9.

- [ ] **Step 1:** Failing test — search query string contains the wiki branch (assert via the existing mocked-EM pattern); migration now also iterates `WikiAttachment`.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3:** Add wiki branch to the UNION (trigram on `title`+`content_text`, route `/wiki/{spaceKey}/{pageId}`, permission-scoped); extend migration. **Manually verify search on Postgres** (note in PR).
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(wiki): global search + attachment migration`.

### Task 15: Wiki frontend — service, tree, editor, pages, nav

**Files:** Create `frontend/src/services/wikiService.ts`, `components/wiki/{WikiTree,WikiEditor,WikiBreadcrumbs,WikiTableOfContents,WikiHistoryPanel}.tsx`, `pages/{WikiSpace,WikiPage}.tsx`; modify `Layout.tsx`, router; add `@blocknote/*` deps; Test `wikiService` + `WikiTree` render/reorder (Vitest).
**Interfaces:** Consumes Tasks 12,14 endpoints.

- [ ] **Step 1:** Failing Vitest — `WikiTree` renders nested pages and fires a move mutation on drag; `WikiEditor` mounts BlockNote and emits JSON on change.
- [ ] **Step 2: Run** `npm test -- wiki` → FAIL.
- [ ] **Step 3:** Add BlockNote deps; implement typed service, React Query hooks, tree (drag reorder/reparent), editor wrapper, breadcrumbs, TOC, history panel, pages, nav entry (`data-tour="wiki-link"`), routes; i18n keys (en+fa, incl. `nav.wiki`, `provider.WIKI`).
- [ ] **Step 4: Run** → PASS; `npm run build`.
- [ ] **Step 5: Commit** `feat(wiki): frontend tree, BlockNote editor, pages, nav`.

### Task 16: SampleDataInitializer demo data

**Files:** Modify `service/SampleDataInitializer.java`; Test: existing initializer test stays green.
**Interfaces:** Consumes Tasks 3,9,11.

- [ ] **Step 1:** Run initializer-related tests → baseline.
- [ ] **Step 2:** Add a demo `WikiSpace` with nested pages (sample BlockNote JSON + `content_text`) and a default `LOCAL_FS` `StorageConfig` (no credentials).
- [ ] **Step 3: Run** `./mvnw -pl backend test` → 0 failures.
- [ ] **Step 4: Commit** `feat: demo wiki space + default storage config`.

### Task 17 (extension): Ingest parseable wiki attachments

**Files:** Modify `WikiKnowledgeListener` / add helper; Test extend `WikiKnowledgeListenerTest`.
**Interfaces:** Consumes Tasks 13,12.

- [ ] **Step 1:** Failing test — uploading a PDF/TXT attachment ingests its Tika-extracted text under its own entity id.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3:** Reuse the Tika extraction path (as `FileUploadProvider`) for parseable types; skip others. (Skip this task if time-constrained — pages are the firm requirement.)
- [ ] **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat(wiki): ingest parseable attachments into Knowledge Center`.

### Task 18: Docs, public pages, tour, final verify

**Files:** Modify `CHANGELOG.md`, `README.md`, `COMPETITOR_ANALYSIS.md`, `ReleaseNotes.tsx`, `PublicRoadmap.tsx`, `TourContext.tsx`, `TOUR_GUIDE.md`, `en.json`, `fa.json`; Create help guides `14-wiki.md`, `15-object-storage.md`.

- [ ] **Step 1:** CHANGELOG `[Unreleased]` entries; README features + comparison rows; COMPETITOR_ANALYSIS updates.
- [ ] **Step 2:** Add **v1.9.0** to BOTH `ReleaseNotes.tsx` (version card) and `PublicRoadmap.tsx` (`recentlyShipped` + `shipped190*` i18n in en+fa) — identical titles/counts/status.
- [ ] **Step 3:** Write `14-wiki.md` + `15-object-storage.md` help guides.
- [ ] **Step 4:** Add tour step for `wiki-link` in `TourContext.tsx` + Step Inventory row in `TOUR_GUIDE.md`.
- [ ] **Step 5:** `cd backend && ./mvnw spotless:apply && ./mvnw verify` (0 failures, ≥80%); `cd frontend && npm test && npm run build && npm run validate:i18n`.
- [ ] **Step 6: Commit** `docs: v1.9.0 changelog, public pages, help guides, tour`; open PR.

---

## Self-Review

- **Spec coverage:** SPI (T1), providers LOCAL_FS/S3/MinIO (T2,T6), StorageConfig+migration (T3), façade (T4), behavior-preserving attachment routing (T5), migration-on-switch (T7,T14), admin UI + test-connection (T8); wiki entities/tree/spaces/folders/pages (T9,T11), per-space ACL (T10), controller + attachments via storage (T12), auto per-page KC ingest (T13), search + mentions [mentions = reuse, R5] (T14), frontend incl. BlockNote/breadcrumbs/TOC/history/internal-links (T15), sample data (T16), attachment-ingest extension (T17), all docs + public-page lockstep + i18n en/fa + help guides + tour + verify (T18). No gaps.
- **Placeholder scan:** no TBD/TODO; every task has concrete files, signatures, and a test.
- **Type consistency:** `WikiPageChangedEvent(pageId, spaceId, type)`, `(WIKI_PAGE, pageId)` chunk keying, `StoredObjectRef`/`DownloadResource`, `WikiRevisionDTO`, and the façade signatures are used identically across T1→T18.
