-- Seed data for screenshots
-- This migration adds data needed to generate realistic screenshots for the help guides

-- 1. Create a "Shop App" Project if it doesn't exist
INSERT INTO projects (name, project_key, description, color, owner_id, is_active, created_at, updated_at)
SELECT 'Shop App', 'SHOP', 'E-commerce mobile application redesign', '#8e44ad', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE project_key = 'SHOP');

-- 2. Create the "Cycle 1 - Q1 2026"
INSERT INTO cycles (name, start_date, end_date, phase, project_id, is_active)
SELECT 'Cycle 1 - Q1 2026', CURRENT_DATE, DATEADD('DAY', 42, CURRENT_DATE), 'BUILD', 
       (SELECT id FROM projects WHERE project_key = 'SHOP'), true
WHERE NOT EXISTS (SELECT 1 FROM cycles WHERE name = 'Cycle 1 - Q1 2026');

-- 3. Create a Pitch "User Profile Redesign"
INSERT INTO pitches (title, description, status, appetite_days, cycle_id, created_at, updated_at)
SELECT 'User Profile Redesign', 'Revamp the user profile to include social features and better settings management.', 'IN_PROGRESS', 14, 
       (SELECT id FROM cycles WHERE name = 'Cycle 1 - Q1 2026'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM pitches WHERE title = 'User Profile Redesign');

-- 4. Link Pitch to Cycle (already linked via cycle_id above, so we can remove this update)


