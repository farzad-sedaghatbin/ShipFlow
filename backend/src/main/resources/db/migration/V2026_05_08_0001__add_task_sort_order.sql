-- Add sort_order column to tasks for drag-to-reorder in the backlog
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;

-- Backfill: assign sort order within each cycle by task id
UPDATE tasks t
SET sort_order = sub.rn
FROM (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY cycle_id ORDER BY id) AS rn
  FROM tasks
  WHERE deleted_at IS NULL
) sub
WHERE t.id = sub.id;

CREATE INDEX IF NOT EXISTS idx_tasks_sort_order ON tasks (cycle_id, sort_order);
