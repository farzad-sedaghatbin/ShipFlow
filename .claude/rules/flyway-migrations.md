---
globs: backend/src/main/resources/db/migration/**
---

# ShipFlow Flyway Migration Rules

**NEVER edit an existing migration file.** Flyway checksums will mismatch and the application will refuse to start. Always create a new file.

**Naming — use the date-prefixed format for NEW migrations**:
```
V{YYYY}_{MM}_{DD}_{NNNN}__{description}.sql
```
Example: `V2026_06_18_0005__add_object_storage.sql` (today's date, next free sequence for that day).

Scan `backend/src/main/resources/db/migration/` for the highest existing `V{YYYY}_{MM}_{DD}_*` and pick the next sequence (or `_0001` for a new day).

> ⚠️ **Do NOT use bare sequential `V{N}__…` for new migrations.** Flyway parses the version numerically, so a sequential `V110` sorts to version `110`, which is **lower** than every date-prefixed `V2026_*` migration (e.g. `V2026_03_30_0001` that creates `task_attachments`). On a fresh database Flyway applies in version order, so a `V110` that alters `task_attachments` runs *before* the table exists and fails. `spring.flyway.out-of-order=true` rescues already-migrated databases but NOT fresh ones. The repo's legacy `V{N}__…` files exist for history only; match the newest files (date-prefixed) for anything new.
>
> ⚠️ Tests run on H2 with Flyway **disabled** (schema is generated from entities via create-drop), so a broken migration ORDER or PostgreSQL-only DDL will pass `./mvnw verify` and only fail at real startup. Verify new migrations against PostgreSQL before merging.

**H2 compatibility** — tests run on H2, so avoid PostgreSQL-only DDL syntax:
- ❌ `jsonb`, `uuid` column type, `SERIAL`, `CREATE INDEX CONCURRENTLY`
- ✅ `BIGINT GENERATED ALWAYS AS IDENTITY`, `TEXT`, `VARCHAR`, `BOOLEAN`, `TIMESTAMP WITH TIME ZONE`, `DECIMAL`
- ✅ `CREATE INDEX IF NOT EXISTS` (standard, works on both)

**Naming conventions**: Foreign keys → `fk_{table}_{referenced_table}`. Indexes → `idx_{table}_{columns}`.

**After creating a migration**: Restart the backend — Flyway runs on startup. Check logs for checksum errors before continuing.
