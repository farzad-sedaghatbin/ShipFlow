-- V63__add_soft_delete_functionality.sql
-- Add soft delete functionality to pitches, tasks, and test cases

-- Add soft delete columns to pitches table
ALTER TABLE pitches
ADD COLUMN deleted_at TIMESTAMP NULL;

ALTER TABLE pitches
ADD COLUMN deleted_by_id BIGINT NULL;

ALTER TABLE pitches
ADD CONSTRAINT fk_pitch_deleted_by
    FOREIGN KEY (deleted_by_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_pitches_deleted_at ON pitches(deleted_at);

-- Add soft delete columns to tasks table
ALTER TABLE tasks
ADD COLUMN deleted_at TIMESTAMP NULL;

ALTER TABLE tasks
ADD COLUMN deleted_by_id BIGINT NULL;

ALTER TABLE tasks
ADD CONSTRAINT fk_task_deleted_by
    FOREIGN KEY (deleted_by_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_tasks_deleted_at ON tasks(deleted_at);

-- Add soft delete columns to test_cases table
ALTER TABLE test_cases
ADD COLUMN deleted_at TIMESTAMP NULL;

ALTER TABLE test_cases
ADD COLUMN deleted_by_id BIGINT NULL;

ALTER TABLE test_cases
ADD CONSTRAINT fk_test_case_deleted_by
    FOREIGN KEY (deleted_by_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_test_cases_deleted_at ON test_cases(deleted_at);

-- Add comments for documentation
COMMENT ON COLUMN pitches.deleted_at IS 'Soft delete timestamp - when the pitch was deleted';
COMMENT ON COLUMN pitches.deleted_by_id IS 'User who deleted the pitch';
COMMENT ON COLUMN tasks.deleted_at IS 'Soft delete timestamp - when the task was deleted';
COMMENT ON COLUMN tasks.deleted_by_id IS 'User who deleted the task';
COMMENT ON COLUMN test_cases.deleted_at IS 'Soft delete timestamp - when the test case was deleted';
COMMENT ON COLUMN test_cases.deleted_by_id IS 'User who deleted the test case';