-- V2026_02_14_0004: Fix COMPLETED task status values
-- =============================================================================
-- TaskStatus enum only has DONE, not COMPLETED.
-- Update any tasks with COMPLETED status to DONE.
-- =============================================================================

UPDATE tasks 
SET status = 'DONE', 
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'COMPLETED';
