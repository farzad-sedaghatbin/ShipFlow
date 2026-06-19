-- Add direct project_id FK to pitches so pre-cycle pitches can be associated
-- with a project without requiring an epic, and to support admin move-to-project.
ALTER TABLE pitches ADD COLUMN project_id BIGINT;
ALTER TABLE pitches ADD CONSTRAINT fk_pitches_project FOREIGN KEY (project_id) REFERENCES projects(id);

-- Backfill from cycle's project (pitches already assigned to a cycle)
UPDATE pitches SET project_id = (
    SELECT c.project_id FROM cycles c WHERE c.id = pitches.cycle_id
) WHERE cycle_id IS NOT NULL AND project_id IS NULL;

-- Backfill from epic's project (pre-cycle pitches with an epic)
UPDATE pitches SET project_id = (
    SELECT e.project_id FROM epics e WHERE e.id = pitches.epic_id
) WHERE cycle_id IS NULL AND epic_id IS NOT NULL AND project_id IS NULL;
