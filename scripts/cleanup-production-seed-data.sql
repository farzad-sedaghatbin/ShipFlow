-- =====================================================
-- CLEANUP SCRIPT: Remove Seed Data from Production
-- =====================================================
-- WARNING: This script will DELETE test/demo data that was
-- accidentally inserted into production by seed migrations.
-- 
-- IMPORTANT: Review this script carefully before running!
-- Make a backup of your production database first!
-- =====================================================

-- Remove seed data in reverse order (respecting foreign keys)

-- Dashboard notifications (V30)
DELETE FROM dashboard_notifications WHERE user_id <= 6;

-- Retrospective data (V25)
DELETE FROM retro_item_votes WHERE retro_item_id IN (SELECT id FROM retro_items WHERE retrospective_id = 6);
DELETE FROM retro_items WHERE retrospective_id = 6;
DELETE FROM retrospectives WHERE id = 6;

-- Screenshots (V47) - Remove if any were added
-- Check your data first: SELECT * FROM uploaded_documents WHERE entity_type = 'screenshot';

-- Hill chart data (V10)
DELETE FROM hill_chart_history WHERE pitch_id IN (SELECT id FROM pitches WHERE cycle_id IN (1, 2, 3, 4, 5, 6));
DELETE FROM hill_chart_points WHERE pitch_id IN (SELECT id FROM pitches WHERE cycle_id IN (1, 2, 3, 4, 5, 6));

-- Work logs and evidence (V21, V25, V56)
DELETE FROM work_logs WHERE pitch_id IN (SELECT id FROM pitches WHERE cycle_id IN (1, 2, 3, 4, 5, 6));
DELETE FROM evidences WHERE pitch_id IN (SELECT id FROM pitches WHERE cycle_id IN (1, 2, 3, 4, 5, 6));

-- QA data
DELETE FROM test_cases WHERE pitch_id IN (SELECT id FROM pitches WHERE cycle_id IN (1, 2, 3, 4, 5, 6));
DELETE FROM bug_reports WHERE pitch_id IN (SELECT id FROM pitches WHERE cycle_id IN (1, 2, 3, 4, 5, 6));

-- Tasks (V17, V21)
DELETE FROM tasks WHERE cycle_id IN (1, 2, 3, 4, 5, 6);

-- Pitches (V20, V21, V25)
DELETE FROM pitches WHERE cycle_id IN (1, 2, 3, 4, 5, 6);

-- Cycles (V20, V21, V25)
DELETE FROM cycles WHERE id IN (1, 2, 3, 4, 5, 6);

-- Teams and team assignments (V11)
DELETE FROM team_assignments WHERE team_id IN (1, 2, 3, 4, 5, 6);
DELETE FROM teams WHERE id IN (1, 2, 3, 4, 5, 6);

-- Projects (V11)
DELETE FROM projects WHERE id IN (1, 2, 3);

-- Users (V11)
DELETE FROM users WHERE id IN (1, 2, 3, 4, 5, 6);

-- Persons (V11)
DELETE FROM persons WHERE id IN (1, 2, 3, 4, 5, 6);

-- =====================================================
-- Verification queries
-- =====================================================
-- Run these to verify cleanup:
-- SELECT COUNT(*) FROM persons;
-- SELECT COUNT(*) FROM users;
-- SELECT COUNT(*) FROM projects;
-- SELECT COUNT(*) FROM cycles;
-- SELECT COUNT(*) FROM pitches;
-- SELECT COUNT(*) FROM tasks;
