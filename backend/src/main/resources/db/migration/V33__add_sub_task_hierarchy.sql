-- Add parent_task_id column to support sub-task hierarchy
ALTER TABLE tasks
ADD COLUMN parent_task_id BIGINT;

-- Add foreign key constraint for parent-child relationship
ALTER TABLE tasks
ADD CONSTRAINT fk_tasks_parent_task
FOREIGN KEY (parent_task_id) REFERENCES tasks(id) ON DELETE CASCADE;

-- Add index for parent_task_id to optimize hierarchy queries
CREATE INDEX idx_tasks_parent_task_id ON tasks(parent_task_id);

-- Add comment
COMMENT ON COLUMN tasks.parent_task_id IS 'Reference to parent task for sub-task hierarchy';
