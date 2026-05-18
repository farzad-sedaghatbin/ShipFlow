-- Allow tasks to exist without a cycle (Scrum product backlog)
ALTER TABLE tasks ALTER COLUMN cycle_id DROP NOT NULL;

-- Add direct project reference for tasks not attached to a cycle
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS project_id BIGINT;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_project'
  ) THEN
    ALTER TABLE tasks ADD CONSTRAINT fk_tasks_project
      FOREIGN KEY (project_id) REFERENCES projects(id);
  END IF;
END $$;

-- Populate project_id from existing cycle relationship (backfill for all tasks with a cycle)
UPDATE tasks SET project_id = (
    SELECT c.project_id FROM cycles c WHERE c.id = tasks.cycle_id
) WHERE cycle_id IS NOT NULL AND project_id IS NULL;

-- Pre-check: fail with a descriptive error if any tasks would violate the NOT NULL constraint
-- (e.g. tasks whose cycle has no project, or orphaned tasks that somehow have cycle_id = NULL
-- before this migration made it nullable).
DO $$
DECLARE
  orphan_count INT;
BEGIN
  SELECT COUNT(*) INTO orphan_count FROM tasks WHERE project_id IS NULL;
  IF orphan_count > 0 THEN
    RAISE EXCEPTION 'V101 migration blocked: % task(s) have NULL project_id after backfill. '
      'Fix orphaned tasks before retrying (SELECT id FROM tasks WHERE project_id IS NULL).', orphan_count;
  END IF;
END $$;

-- Enforce NOT NULL after backfill: every task must belong to a project
-- (either via cycle → project or via direct project reference for backlog tasks)
ALTER TABLE tasks ALTER COLUMN project_id SET NOT NULL;

-- Create index for the new column
CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON tasks(project_id);
