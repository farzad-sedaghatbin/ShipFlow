-- Test cases had no direct project association, so the Test Cases page showed every project's
-- cases regardless of the selected project. Add a direct project_id FK (like bug_reports) and
-- backfill it from the test case's cycle, then its pitch.
ALTER TABLE test_cases ADD COLUMN project_id BIGINT;

ALTER TABLE test_cases
    ADD CONSTRAINT fk_test_cases_project
    FOREIGN KEY (project_id) REFERENCES projects (id);

CREATE INDEX IF NOT EXISTS idx_test_cases_project_id ON test_cases (project_id);

-- Backfill from the linked cycle's project first.
UPDATE test_cases SET project_id = (
    SELECT c.project_id FROM cycles c WHERE c.id = test_cases.cycle_id
) WHERE cycle_id IS NOT NULL AND project_id IS NULL;

-- Then fall back to the linked pitch's project for any still unset.
UPDATE test_cases SET project_id = (
    SELECT p.project_id FROM pitches p WHERE p.id = test_cases.pitch_id
) WHERE pitch_id IS NOT NULL AND project_id IS NULL;
