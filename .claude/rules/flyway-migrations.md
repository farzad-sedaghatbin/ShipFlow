---
globs: backend/src/main/resources/db/migration/**
---

# ShipFlow Flyway Migration Rules

**NEVER edit an existing migration file.** Flyway checksums will mismatch and the application will refuse to start. Always create a new file.

**Naming — use date-based format for all new files**:
```
V{YYYY}_{MM}_{DD}_{sequence}__{description}.sql
```
Example: `V2026_04_22_0001__add_notification_preferences.sql`

Sequence is a 4-digit counter scoped to the day (0001, 0002, …). Scan existing files for today's date before picking the sequence.

**Legacy formats already in the repo** (do not use for new files):
- `V{N}__{description}.sql` — old sequential (V1__init.sql … V99__)
- `V{YYYYMMDD}{seq}__{description}.sql` — date without underscores

**H2 compatibility** — tests run on H2, so avoid PostgreSQL-only DDL syntax:
- ❌ `jsonb`, `uuid` column type, `SERIAL`, `CREATE INDEX CONCURRENTLY`
- ✅ `BIGINT GENERATED ALWAYS AS IDENTITY`, `TEXT`, `VARCHAR`, `BOOLEAN`, `TIMESTAMP WITH TIME ZONE`, `DECIMAL`
- ✅ `CREATE INDEX IF NOT EXISTS` (standard, works on both)

**Naming conventions**: Foreign keys → `fk_{table}_{referenced_table}`. Indexes → `idx_{table}_{columns}`.

**After creating a migration**: Restart the backend — Flyway runs on startup. Check logs for checksum errors before continuing.
