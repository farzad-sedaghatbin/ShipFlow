-- ===========================================
-- Reset Sequences After Acted-On Retro Seed Data
-- ===========================================
-- This ensures auto-generated IDs don't conflict with manually inserted seed data
-- from V2026_02_09_0001 (acted-on retro items example data)

-- Reset pitches sequence (added pitch ID 19)
SELECT setval(pg_get_serial_sequence('pitches', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM pitches), false);

-- Reset tasks sequence (added task IDs 50-51)
SELECT setval(pg_get_serial_sequence('tasks', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM tasks), false);

-- Reset comments sequence (added comment IDs 1000-1001)
SELECT setval(pg_get_serial_sequence('comments', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM comments), false);
