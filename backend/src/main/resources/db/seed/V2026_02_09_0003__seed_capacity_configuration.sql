-- V2026_02_09_0003__seed_capacity_configuration.sql
-- Add capacity configuration demo data to showcase the feature

-- Update organization settings with default capacity config
UPDATE organization_settings 
SET default_hours_per_day = 8.0,
    default_working_days_per_week = 5
WHERE id = 1;

-- Add team capacity overrides to demonstrate team-level configuration
-- Alpha Team: Standard 8 hours/day, 5 days/week (uses org defaults)
-- Beta Team: Reduced working week (4 days/week for experimentation)
UPDATE teams SET working_days_per_week_override = 4 WHERE name = 'Beta Team';

-- Add person-level capacity overrides to demonstrate inheritance
-- Some team members work part-time or have different capacity
UPDATE persons SET hours_per_day_override = 6.0 WHERE name = 'Alice Johnson'; -- Part-time senior
UPDATE persons SET hours_per_day_override = 4.0 WHERE name = 'Grace Lee'; -- Half-time contractor

-- Add team assignment level overrides for specific pitch work
-- This demonstrates the finest-grained control  
UPDATE team_assignments 
SET hours_per_day_override = 7.0 
WHERE person_id = (SELECT id FROM persons WHERE name = 'Bob Smith') 
  AND team_id = (SELECT id FROM teams WHERE name = 'Alpha Team');

-- Add some comments to explain the capacity configuration setup
COMMENT ON TABLE organization_settings IS 'Organization settings with capacity configuration: default_hours_per_day (8.0) and default_working_days_per_week (5)';
