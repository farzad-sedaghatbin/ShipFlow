-- ============================================================================
-- V1008: Release traceability seed data (v0.6.0)
-- Links existing tasks and bug reports to releases so that release filters,
-- cockpit breakdowns, and slipped-bug warnings have demo data.
-- ============================================================================

-- Cycle 4 tasks → Q4 2025 Release (release id=1)
UPDATE tasks SET target_release_id = 1 WHERE id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);

-- Cycle 5 mobile tasks → Q1 2026 Mobile Release (release id=2)
UPDATE tasks SET target_release_id = 2 WHERE id IN (17, 18, 19);

-- Active/recent bug reports → Q4 2025 Release
UPDATE bug_reports SET target_release_id = 1 WHERE id IN (1, 2, 3, 4, 5);

-- Older resolved bugs → Q4 2025 Release
UPDATE bug_reports SET target_release_id = 1 WHERE id IN (6, 7);
