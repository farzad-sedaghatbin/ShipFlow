-- V93: Remove scope_id from bug_reports table
-- =============================================================================
-- Since tasks are now integrated with scopes, the separate scope_id field
-- in bug_reports is redundant. The task_id field provides all necessary
-- traceability to both task and scope.
-- =============================================================================

-- Drop the index first
DROP INDEX IF EXISTS idx_bug_scope;

-- Drop the foreign key constraint
ALTER TABLE bug_reports DROP CONSTRAINT IF EXISTS bug_reports_scope_id_fkey;

-- Drop the scope_id column
ALTER TABLE bug_reports DROP COLUMN IF EXISTS scope_id;

-- Add comment to document the change
COMMENT ON TABLE bug_reports IS 'Bug reports tracked in the system. Tasks provide traceability to scopes through task.scope_id relationship.';
