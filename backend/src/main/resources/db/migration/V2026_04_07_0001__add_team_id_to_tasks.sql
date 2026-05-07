-- Add optional team assignment to tasks
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS team_id BIGINT;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_team_id ON tasks(team_id) WHERE team_id IS NOT NULL;
