---
globs: backend/src/main/resources/db/migration/**
---

# ShipFlow Flyway Migration Rules

**NEVER edit an existing migration file.** Flyway checksums will mismatch and the application will refuse to start. Always create a new file.

**Naming — use sequential format (matches all existing files in this repo)**:
```
V{N}__{description}.sql
```
Example: if the highest existing version is V60, the new file is `V61__add_notification_preferences.sql`

Scan the migration directory for the highest `V{N}__*.sql` before picking the next number.

**H2 compatibility** — tests run on H2, so avoid PostgreSQL-only DDL syntax:
- ❌ `jsonb`, `uuid` column type, `SERIAL`, `CREATE INDEX CONCURRENTLY`
- ✅ `BIGINT GENERATED ALWAYS AS IDENTITY`, `TEXT`, `VARCHAR`, `BOOLEAN`, `TIMESTAMP WITH TIME ZONE`, `DECIMAL`
- ✅ `CREATE INDEX IF NOT EXISTS` (standard, works on both)

**Naming conventions**: Foreign keys → `fk_{table}_{referenced_table}`. Indexes → `idx_{table}_{columns}`.

**After creating a migration**: Restart the backend — Flyway runs on startup. Check logs for checksum errors before continuing.
