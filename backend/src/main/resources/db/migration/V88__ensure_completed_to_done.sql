-- V88: Ensure all COMPLETED task statuses are changed to DONE
-- This fixes any remaining COMPLETED values that may exist in the database
-- The TaskStatus enum only supports: BACKLOG, TODO, IN_PROGRESS, BLOCKED, IN_REVIEW, DONE, CANCELLED

-- Update any remaining tasks with COMPLETED status to DONE
UPDATE tasks 
SET status = 'DONE', updated_at = CURRENT_TIMESTAMP 
WHERE status = 'COMPLETED'
AND deleted_at IS NULL;

-- Also check for soft-deleted tasks to prevent future errors if they're restored
UPDATE tasks 
SET status = 'DONE'
WHERE status = 'COMPLETED'
AND deleted_at IS NOT NULL;
