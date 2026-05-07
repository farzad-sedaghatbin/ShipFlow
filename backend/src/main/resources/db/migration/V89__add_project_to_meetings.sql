-- Add project_id column to meetings table for direct project association
-- This allows meetings to be associated with a project even without a pitch

ALTER TABLE meetings ADD COLUMN project_id BIGINT;
ALTER TABLE meetings ADD CONSTRAINT fk_meetings_project FOREIGN KEY (project_id) REFERENCES projects(id);

-- Populate project_id from existing pitch associations
UPDATE meetings m
SET project_id = (
    SELECT c.project_id
    FROM pitches pit
    JOIN cycles c ON pit.cycle_id = c.id
    WHERE pit.id = m.pitch_id
)
WHERE m.pitch_id IS NOT NULL;

-- Create index for better query performance
CREATE INDEX idx_meetings_project_id ON meetings(project_id);
