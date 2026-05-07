-- V99: Remove cycle_id from teams
--
-- Teams are org-level entities that persist across cycles.
-- Cycle membership is derived from pitches (pitches.cycle_id + pitches.team_id).
-- The teams.cycle_id column was a design flaw — it locked a team to a single cycle.

ALTER TABLE teams DROP CONSTRAINT IF EXISTS fk_teams_cycle;
DROP INDEX IF EXISTS idx_teams_cycle_id;
ALTER TABLE teams DROP COLUMN IF EXISTS cycle_id;
