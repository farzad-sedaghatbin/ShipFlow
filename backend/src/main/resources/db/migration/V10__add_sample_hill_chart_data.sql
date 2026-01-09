-- Sample Hill Chart Data for Testing
-- This migration adds sample hill chart points to existing pitches

-- Insert sample hill chart points for existing pitches
-- These will only be inserted if the corresponding pitch exists

-- Sample data for various pitches to demonstrate hill chart functionality
-- Position ranges: 0-50 (figuring things out), 50-100 (making it happen)

-- Add hill chart points for any existing pitch
INSERT INTO hill_chart_points (pitch_id, scope, description, position, created_at, updated_at)
SELECT 
    p.id,
    'Backend API Development',
    'Build RESTful API endpoints and business logic',
    75,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM pitches p
WHERE p.id = (SELECT MIN(id) FROM pitches)
AND NOT EXISTS (SELECT 1 FROM hill_chart_points WHERE pitch_id = p.id AND scope = 'Backend API Development');

INSERT INTO hill_chart_points (pitch_id, scope, description, position, created_at, updated_at)
SELECT 
    p.id,
    'Database Schema',
    'Design and implement database tables and relationships',
    95,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM pitches p
WHERE p.id = (SELECT MIN(id) FROM pitches)
AND NOT EXISTS (SELECT 1 FROM hill_chart_points WHERE pitch_id = p.id AND scope = 'Database Schema');

INSERT INTO hill_chart_points (pitch_id, scope, description, position, created_at, updated_at)
SELECT 
    p.id,
    'Frontend Components',
    'Create reusable UI components and pages',
    45,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM pitches p
WHERE p.id = (SELECT MIN(id) FROM pitches)
AND NOT EXISTS (SELECT 1 FROM hill_chart_points WHERE pitch_id = p.id AND scope = 'Frontend Components');

INSERT INTO hill_chart_points (pitch_id, scope, description, position, created_at, updated_at)
SELECT 
    p.id,
    'Authentication & Security',
    'Implement JWT auth and role-based access control',
    60,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM pitches p
WHERE p.id = (SELECT MIN(id) FROM pitches)
AND NOT EXISTS (SELECT 1 FROM hill_chart_points WHERE pitch_id = p.id AND scope = 'Authentication & Security');

INSERT INTO hill_chart_points (pitch_id, scope, description, position, created_at, updated_at)
SELECT 
    p.id,
    'Testing & Documentation',
    'Write unit tests and API documentation',
    25,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM pitches p
WHERE p.id = (SELECT MIN(id) FROM pitches)
AND NOT EXISTS (SELECT 1 FROM hill_chart_points WHERE pitch_id = p.id AND scope = 'Testing & Documentation');
