-- Soft-archive support for teams: teams with history cannot be hard-deleted.
ALTER TABLE teams ADD COLUMN IF NOT EXISTS is_archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_teams_is_archived ON teams(is_archived);
