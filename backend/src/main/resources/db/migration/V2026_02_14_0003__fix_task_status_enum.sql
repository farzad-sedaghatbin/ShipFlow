-- V2026_02_14_0003: Fix TaskStatus enum values
-- =============================================================================
-- TaskStatus enum has DONE, not COMPLETED. Update any existing COMPLETED tasks.
-- This fixes production data that may have been created with incorrect status values.
-- =============================================================================

UPDATE tasks 
SET status = 'DONE' 
WHERE status = 'COMPLETED';
