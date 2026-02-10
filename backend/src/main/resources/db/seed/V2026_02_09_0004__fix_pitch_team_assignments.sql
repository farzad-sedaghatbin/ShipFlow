-- V2026_02_09_0004: Fix team assignments for pitches with work logs
-- =============================================================================
-- Some pitches in Cycle 4 have work logs but no team assigned because they
-- were not properly assigned through the betting table. This fixes those
-- assignments so capacity calculations work correctly.
-- =============================================================================

-- Assign pitches with Alpha Team work logs to Alpha Team (team_id = 1)
-- These pitches have work logs from Alice Johnson (id=2), Bob Smith (id=3), 
-- and Carol Williams (id=6) who are all members of Alpha Team

-- Pitch #2: User Profile Settings
UPDATE pitches 
SET team_id = 1, updated_at = CURRENT_TIMESTAMP 
WHERE id = 2 AND cycle_id = 4 AND team_id IS NULL;

-- Pitch #3: Biometric Login
UPDATE pitches 
SET team_id = 1, updated_at = CURRENT_TIMESTAMP 
WHERE id = 3 AND cycle_id = 4 AND team_id IS NULL;

-- Pitch #5: Activity History Timeline
UPDATE pitches 
SET team_id = 1, updated_at = CURRENT_TIMESTAMP 
WHERE id = 5 AND cycle_id = 4 AND team_id IS NULL;
