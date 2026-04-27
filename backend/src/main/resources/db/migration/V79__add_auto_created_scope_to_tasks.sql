-- V79: Add auto_created_scope_id to tasks table
-- This column was missing from V73 migration

-- Add auto_created_scope_id to tasks for bidirectional binding
ALTER TABLE tasks ADD COLUMN auto_created_scope_id BIGINT;

-- Add foreign key constraint for auto_created_scope_id
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_auto_created_scope
    FOREIGN KEY (auto_created_scope_id) REFERENCES hill_chart_points(id) ON DELETE SET NULL;

-- Add unique constraint to ensure one-to-one relationship
ALTER TABLE tasks ADD CONSTRAINT uk_tasks_auto_created_scope UNIQUE (auto_created_scope_id);

-- Create index for faster lookups
CREATE INDEX idx_tasks_auto_created_scope ON tasks(auto_created_scope_id);

-- Comment explaining the architecture
COMMENT ON COLUMN tasks.auto_created_scope_id IS 'The scope (hill chart point) auto-created for this task. Enables bidirectional Scope-Task bridge.';
