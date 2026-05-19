-- Drop the existing check constraint that only allows SHAPE_UP and KANBAN,
-- then recreate it to also allow SCRUM (added in v1.1.0).
ALTER TABLE projects DROP CONSTRAINT IF EXISTS chk_project_type;
ALTER TABLE projects ADD CONSTRAINT chk_project_type
    CHECK (project_type IN ('SHAPE_UP', 'KANBAN', 'SCRUM'));
