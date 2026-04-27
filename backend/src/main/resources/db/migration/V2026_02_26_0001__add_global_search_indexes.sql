-- ============================================================================
-- Global Search: Enable pg_trgm extension and create trigram GIN indexes
-- for fast fuzzy search across tasks, bug reports, pitches, and epics.
-- ============================================================================

-- Enable trigram extension for fuzzy/typo-tolerant text matching
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Trigram GIN indexes on title/name/key fields for fast similarity search
CREATE INDEX IF NOT EXISTS idx_tasks_trgm_title ON tasks USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_bug_reports_trgm_title ON bug_reports USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_bug_reports_trgm_bug_key ON bug_reports USING gin (bug_key gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_pitches_trgm_title ON pitches USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_epics_trgm_name ON epics USING gin (name gin_trgm_ops);
