-- V71: Scope-Task Bridge
-- Enables bidirectional synchronization between HillChartPoint (Scope) and Task entities.
-- When a root task (no parent) is linked to a pitch, a scope is auto-created and linked.
-- When a scope is created, a corresponding task is auto-created.

-- Add linked_task_id to hill_chart_points for bidirectional binding
ALTER TABLE hill_chart_points ADD COLUMN linked_task_id BIGINT;

-- Add auto_progress_enabled flag (defaults to true - position auto-updates from subtask completion)
ALTER TABLE hill_chart_points ADD COLUMN auto_progress_enabled BOOLEAN NOT NULL DEFAULT TRUE;

-- Add foreign key constraint for linked_task_id
ALTER TABLE hill_chart_points ADD CONSTRAINT fk_hill_chart_points_linked_task
    FOREIGN KEY (linked_task_id) REFERENCES tasks(id) ON DELETE SET NULL;

-- Add unique constraint to ensure one-to-one relationship
ALTER TABLE hill_chart_points ADD CONSTRAINT uk_hill_chart_points_linked_task UNIQUE (linked_task_id);

-- Create index for faster lookups
CREATE INDEX idx_hill_chart_points_linked_task ON hill_chart_points(linked_task_id);

-- Comment explaining the architecture
COMMENT ON COLUMN hill_chart_points.linked_task_id IS 'The task linked to this scope for unified Scope-Task tracking. Auto-created when task has pitch but no parent.';
COMMENT ON COLUMN hill_chart_points.auto_progress_enabled IS 'When true, position auto-updates from subtask completion. Set to false on manual drag.';
