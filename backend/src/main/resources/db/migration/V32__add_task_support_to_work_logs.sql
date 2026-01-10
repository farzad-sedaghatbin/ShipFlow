-- Add task_id column to work_logs table to support time logging on tasks
-- Make pitch_id nullable to allow work logs for either pitches or tasks

ALTER TABLE work_logs
    ALTER COLUMN pitch_id DROP NOT NULL;

ALTER TABLE work_logs
    ADD COLUMN task_id BIGINT;

ALTER TABLE work_logs
    ADD CONSTRAINT fk_work_logs_task
        FOREIGN KEY (task_id) REFERENCES tasks(id)
        ON DELETE CASCADE;

-- Add constraint to ensure either pitch_id or task_id is provided, but not both
ALTER TABLE work_logs
    ADD CONSTRAINT chk_work_logs_pitch_or_task
        CHECK (
            (pitch_id IS NOT NULL AND task_id IS NULL) OR
            (pitch_id IS NULL AND task_id IS NOT NULL)
        );

-- Create index for task_id for better query performance
CREATE INDEX idx_work_logs_task_id ON work_logs(task_id);

-- Add comment to document the change
COMMENT ON COLUMN work_logs.task_id IS 'Optional reference to task for time logging. Either pitch_id or task_id must be set.';
