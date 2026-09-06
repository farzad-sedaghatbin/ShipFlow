-- v1.13.0 S64: optimistic-lock conflict detection for live-editing (Pitch, RetroItem, WikiPage).
-- Adds a JPA @Version column to each entity, backfilled to 0 for every existing row.
--
-- Must be NOT NULL with a real backfilled value, not left NULL for pre-existing rows: on the
-- first UPDATE to an entity Hibernate loaded with version = NULL, its generated optimistic-lock
-- WHERE clause is "... AND version = ?" bound to that null value — under standard SQL null
-- semantics "version = NULL" never matches any row (not even one whose column is also NULL), so
-- every pre-existing pitch/retro item/wiki page would fail its very first edit after this
-- migration with a spurious conflict. Backfilling to 0 up front avoids this entirely; Hibernate
-- manages the column from 0 onward for both new and pre-existing rows. (The entity's Java field
-- stays a boxed Long and can still be null in memory for a transient, not-yet-persisted object —
-- that's unrelated to this column's DB-level NOT NULL constraint.)
ALTER TABLE pitches ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE retro_items ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE wiki_pages ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
