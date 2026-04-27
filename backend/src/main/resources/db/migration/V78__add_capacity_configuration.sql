-- V78: Add capacity configuration for flexible work hours and team budgeting
-- Supports org-wide defaults with per-team and per-person-per-team overrides

-- Add capacity configuration to organization_settings
ALTER TABLE organization_settings 
ADD COLUMN default_hours_per_day DECIMAL(4,2) DEFAULT 8.0 NOT NULL,
ADD COLUMN default_working_days_per_week INTEGER DEFAULT 5 NOT NULL;

-- Add capacity overrides to teams
ALTER TABLE teams 
ADD COLUMN hours_per_day_override DECIMAL(4,2) NULL,
ADD COLUMN working_days_per_week_override INTEGER NULL;

-- Add capacity override to persons (person-level default)
ALTER TABLE persons 
ADD COLUMN hours_per_day_override DECIMAL(4,2) NULL;

-- Add capacity override to team_assignments (finest-grained control per team)
ALTER TABLE team_assignments 
ADD COLUMN hours_per_day_override DECIMAL(4,2) NULL;

-- Add comments explaining the inheritance hierarchy
COMMENT ON COLUMN organization_settings.default_hours_per_day IS 'Organization-wide default hours per day (base of inheritance chain)';
COMMENT ON COLUMN organization_settings.default_working_days_per_week IS 'Organization-wide default working days per week (e.g., 5 for standard, 4 for 4-day week)';
COMMENT ON COLUMN teams.hours_per_day_override IS 'Team-level override for hours per day (null = inherit from org)';
COMMENT ON COLUMN teams.working_days_per_week_override IS 'Team-level override for working days per week (null = inherit from org)';
COMMENT ON COLUMN persons.hours_per_day_override IS 'Person-level default hours per day override (null = inherit from org/team)';
COMMENT ON COLUMN team_assignments.hours_per_day_override IS 'Per-assignment hours override - finest control for shared resources (null = inherit from person/team/org)';
