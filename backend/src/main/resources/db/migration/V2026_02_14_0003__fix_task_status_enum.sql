-- V2026_02_14_0003: Fix TaskStatus enum values
-- =============================================================================
-- NOTE: The COMPLETED → DONE data correction is handled by a dedicated migration
-- (V88__ensure_completed_to_done.sql). This migration is intentionally left as a
-- no-op to avoid duplicating the same data update in multiple migrations.
-- =============================================================================

-- No-op: The data fix is already handled by V88__ensure_completed_to_done.sql
