-- V24__add_task_category.sql
-- Add category column to tasks table to support Pitch Scope vs Debt & Improvement categorization

ALTER TABLE tasks ADD COLUMN category VARCHAR(20) DEFAULT 'PITCH_SCOPE';

-- Update existing tasks to have the default category
UPDATE tasks SET category = 'PITCH_SCOPE' WHERE category IS NULL;

-- Add NOT NULL constraint after setting defaults
ALTER TABLE tasks ALTER COLUMN category SET NOT NULL;

-- Add index for efficient category filtering
CREATE INDEX idx_tasks_cycle_category ON tasks(cycle_id, category);

-- Add comment for documentation
COMMENT ON COLUMN tasks.category IS 'Task category: PITCH_SCOPE for pitch-related work, DEBT_IMPROVEMENT for technical debt and maintenance';
