-- Add story points to tasks for Scrum mode
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS story_points INTEGER;

-- Add sprint fields to cycles for Scrum mode
ALTER TABLE cycles ADD COLUMN IF NOT EXISTS sprint_goal TEXT;
ALTER TABLE cycles ADD COLUMN IF NOT EXISTS velocity_actual INTEGER;

-- Note: project_type is stored as VARCHAR so no ALTER TYPE needed for the SCRUM enum value
