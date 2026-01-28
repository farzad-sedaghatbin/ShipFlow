-- V53: Add traceability relationships for improved tracking across modules
-- This migration adds optional pitch/scope relationships to tasks, and scope/task relationships to bugs and test cases
-- Supports both pitch-scoped work and technical debt/improvement work
-- NOTE: Existing records will have NULL values for these new fields, which is acceptable since these are optional relationships

-- Add pitch and scope columns to tasks table (H2-compatible syntax)
ALTER TABLE tasks ADD COLUMN pitch_id BIGINT;
ALTER TABLE tasks ADD COLUMN scope_id BIGINT;

-- Add foreign key constraints with indexes for performance
ALTER TABLE tasks
ADD CONSTRAINT fk_task_pitch
    FOREIGN KEY (pitch_id) REFERENCES pitches(id) ON DELETE SET NULL;

ALTER TABLE tasks
ADD CONSTRAINT fk_task_scope
    FOREIGN KEY (scope_id) REFERENCES hill_chart_points(id) ON DELETE SET NULL;

CREATE INDEX idx_task_pitch ON tasks(pitch_id);
CREATE INDEX idx_task_scope ON tasks(scope_id);

-- Add scope and task columns to bug_reports table (H2-compatible syntax)
ALTER TABLE bug_reports ADD COLUMN scope_id BIGINT;
ALTER TABLE bug_reports ADD COLUMN task_id BIGINT;

-- Add foreign key constraints with indexes
ALTER TABLE bug_reports
ADD CONSTRAINT fk_bug_scope
    FOREIGN KEY (scope_id) REFERENCES hill_chart_points(id) ON DELETE SET NULL;

ALTER TABLE bug_reports
ADD CONSTRAINT fk_bug_task
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL;

CREATE INDEX idx_bug_scope ON bug_reports(scope_id);
CREATE INDEX idx_bug_task ON bug_reports(task_id);

-- Add scope and task columns to test_cases table (H2-compatible syntax)
ALTER TABLE test_cases ADD COLUMN scope_id BIGINT;
ALTER TABLE test_cases ADD COLUMN task_id BIGINT;

-- Add foreign key constraints with indexes
ALTER TABLE test_cases
ADD CONSTRAINT fk_testcase_scope
    FOREIGN KEY (scope_id) REFERENCES hill_chart_points(id) ON DELETE SET NULL;

ALTER TABLE test_cases
ADD CONSTRAINT fk_testcase_task
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL;

CREATE INDEX idx_testcase_scope ON test_cases(scope_id);
CREATE INDEX idx_testcase_task ON test_cases(task_id);
