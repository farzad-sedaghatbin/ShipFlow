-- Add direct project relationship to bug_reports for Kanban projects
-- This allows bugs to be associated with a project without requiring a cycle, pitch, or task
-- Useful for general testing, smoke tests, and exploratory testing

-- Add project_id column to bug_reports
ALTER TABLE bug_reports ADD COLUMN project_id BIGINT;

-- Add foreign key constraint
ALTER TABLE bug_reports 
    ADD CONSTRAINT fk_bug_report_project 
    FOREIGN KEY (project_id) REFERENCES projects(id);

-- Add index for performance
CREATE INDEX idx_bug_report_project ON bug_reports(project_id);

-- Migrate existing data: set project_id from cycle's project where cycle exists
UPDATE bug_reports br 
SET project_id = (
    SELECT c.project_id 
    FROM cycles c 
    WHERE c.id = br.cycle_id
)
WHERE br.cycle_id IS NOT NULL AND br.project_id IS NULL;
