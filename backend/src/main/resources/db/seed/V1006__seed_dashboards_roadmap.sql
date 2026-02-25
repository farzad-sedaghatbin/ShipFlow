-- ============================================================================
-- V1006: Dashboards and Roadmap seed data
-- ============================================================================

-- ===========================================
-- CUSTOM DASHBOARDS
-- ===========================================
INSERT INTO custom_dashboards (id, user_id, name, description, is_default, is_template, template_category, created_by, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
-- Template dashboards
(1, 1, 'Executive Summary', 'High-level overview for leadership', false, true, 'EXECUTIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 'Developer Dashboard', 'Daily work tracking for developers', false, true, 'DEVELOPER', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 'QA Dashboard', 'Test and quality metrics', false, true, 'QA', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1, 'Cycle Progress', 'Cycle-level progress tracking', false, true, 'MANAGER', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- User dashboards
(5, 2, 'Alice''s Work View', 'Personal dashboard for Alice', true, false, 'CUSTOM', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 7, 'PM Overview', 'Frank''s project management view', true, false, 'CUSTOM', 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- DASHBOARD WIDGET CONFIGS
-- ===========================================
INSERT INTO dashboard_widget_configs (id, dashboard_id, widget_type, settings, position_x, position_y, width, height, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
-- Executive Summary widgets
(1, 1, 'STAT', '{"title":"Active Pitches","metric":"pitch_count","filter":"status=IN_PROGRESS"}', 0, 0, 3, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 'STAT', '{"title":"Cycle Progress","metric":"cycle_progress","showPercentage":true}', 3, 0, 3, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 'STAT', '{"title":"Team Velocity","metric":"velocity_avg"}', 6, 0, 3, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1, 'STAT', '{"title":"Open Bugs","metric":"bug_count","filter":"status=NEW,IN_PROGRESS"}', 9, 0, 3, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 1, 'HILL_CHART', '{"title":"Hill Chart Overview","showAllPitches":true}', 0, 2, 6, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 1, 'LINE_CHART', '{"title":"Burndown","metric":"hours_remaining"}', 6, 2, 6, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Developer Dashboard widgets
(7, 2, 'TABLE', '{"title":"My Active Tasks","view":"tasks","filter":"assignee=me,status!=DONE"}', 0, 0, 8, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 2, 'CUSTOM_KPI', '{"title":"Code Review Time","metric":"review_time_avg","target":24}', 8, 0, 2, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 2, 'CUSTOM_KPI', '{"title":"Tech Debt","metric":"tech_debt_hours"}', 10, 0, 2, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 2, 'ACTIVITY_FEED', '{"title":"Recent Activity","limit":10}', 8, 2, 4, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- QA Dashboard widgets
(11, 3, 'PIE_CHART', '{"title":"Test Status","metric":"test_status_distribution"}', 0, 0, 4, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 3, 'BAR_CHART', '{"title":"Bugs by Severity","metric":"bugs_by_severity"}', 4, 0, 4, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 3, 'TABLE', '{"title":"Recent Test Runs","view":"test_runs","limit":10}', 8, 0, 4, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, 3, 'STAT', '{"title":"Pass Rate","metric":"test_pass_rate","format":"percentage"}', 0, 4, 3, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(15, 3, 'STAT', '{"title":"Coverage","metric":"test_coverage","format":"percentage"}', 3, 4, 3, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- ROADMAP: INITIATIVES
-- Status: DRAFT, PLANNED, IN_PROGRESS, COMPLETED, ON_HOLD
-- ===========================================
INSERT INTO initiatives (id, name, description, status, color, target_start_date, target_end_date, project_id, owner_id, sort_order, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Developer Experience 2026', 'Improve developer productivity and satisfaction', 'IN_PROGRESS', '#3B82F6', '2025-10-01', '2026-06-30', 1, 7, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Mobile Platform', 'Launch mobile apps for iOS and Android', 'PLANNED', '#10B981', '2025-12-01', '2026-09-30', 1, 7, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Enterprise Features', 'Enterprise-grade security and compliance', 'DRAFT', '#8B5CF6', '2026-07-01', '2027-06-30', 1, 7, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- ROADMAP: EPICS
-- ===========================================
INSERT INTO epics (id, name, description, status, color, target_start_date, target_end_date, project_id, initiative_id, owner_id, sort_order, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
-- Developer Experience epics
(1, 'Third-Party Integrations', 'GitHub, Slack, and other tool integrations', 'IN_PROGRESS', '#60A5FA', '2025-10-01', '2025-12-31', 1, 1, 7, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Custom Dashboards', 'User-configurable dashboard system', 'IN_PROGRESS', '#34D399', '2025-10-01', '2025-11-30', 1, 1, 7, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'API Improvements', 'Developer API enhancements and docs', 'PLANNED', '#A78BFA', '2026-01-01', '2026-03-31', 1, 1, 7, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Mobile Platform epics
(4, 'Mobile Foundation', 'Core mobile app infrastructure', 'PLANNED', '#10B981', '2025-12-01', '2026-02-28', 1, 2, 7, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'Offline Support', 'Offline data sync capabilities', 'DRAFT', '#22D3EE', '2026-03-01', '2026-05-31', 1, 2, 7, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Link pitches to epics
UPDATE pitches SET epic_id = 1 WHERE id IN (1, 2);  -- GitHub & Slack to Integrations epic
UPDATE pitches SET epic_id = 2 WHERE id IN (3, 4);  -- Dashboard pitches
UPDATE pitches SET epic_id = 4 WHERE id IN (5, 6, 7, 8);  -- Mobile pitches

-- ===========================================
-- ROADMAP: RELEASES
-- Status: PLANNING, IN_PROGRESS, READY, RELEASED, CANCELLED
-- Risk: LOW, MEDIUM, HIGH, CRITICAL
-- ===========================================
INSERT INTO releases (id, name, version, description, status, risk_level, target_date, project_id, sort_order, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Q4 2025 Release', 'v2.0.0', 'Major release with integrations and dashboards', 'IN_PROGRESS', 'MEDIUM', '2025-12-15', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Q1 2026 Mobile', 'v3.0.0', 'Initial mobile app release', 'PLANNING', 'HIGH', '2026-03-31', 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Link releases to cycles
INSERT INTO release_cycles (release_id, cycle_id) VALUES
(1, 4),
(1, 5),
(2, 5);

-- Link pitches to releases
UPDATE pitches SET target_release_id = 1 WHERE id IN (1, 2, 3, 4);
UPDATE pitches SET target_release_id = 2 WHERE id IN (5, 6, 7, 8);

-- Reset sequences
SELECT setval('custom_dashboards_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM custom_dashboards), false);
SELECT setval('dashboard_widget_configs_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM dashboard_widget_configs), false);
SELECT setval('initiatives_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM initiatives), false);
SELECT setval('epics_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM epics), false);
SELECT setval('releases_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM releases), false);
