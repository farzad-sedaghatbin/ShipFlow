-- Link tasks to epics so Kanban/Scrum roadmaps can show tasks under epics
-- (Shape Up projects keep linking pitches to epics instead).
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS epic_id BIGINT;

ALTER TABLE tasks ADD CONSTRAINT fk_tasks_epics FOREIGN KEY (epic_id) REFERENCES epics (id);

CREATE INDEX IF NOT EXISTS idx_tasks_epic_id ON tasks (epic_id);
