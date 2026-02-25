-- ============================================================================
-- V1001: Pitches seed data
-- Status values: IDEA, DRAFT, SHAPED, PENDING, STARTED, IN_PROGRESS, TESTING, DONE, COOLDOWN, CANCELLED, CIRCUIT_BREAKER
-- Priority values: HIGH, MEDIUM, LOW (null = not set)
-- ============================================================================

-- ===========================================
-- PITCHES for Cycle 4 (BUILD phase - committed work)
-- ===========================================
INSERT INTO pitches (id, title, description, appetite_days, cycle_id, team_id, status, sort_order, priority, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
-- Alpha Team pitches (committed and in progress)
(1, 'GitHub Integration', 'Integrate with GitHub for issue tracking and PR linking', 14, 4, 1, 'IN_PROGRESS', 1, 'HIGH', '2025-10-15', CURRENT_TIMESTAMP),
(2, 'Slack Notifications', 'Send real-time notifications to Slack channels', 7, 4, 1, 'IN_PROGRESS', 2, 'MEDIUM', '2025-10-15', CURRENT_TIMESTAMP),
-- Beta Team pitches
(3, 'Dashboard Widgets', 'Customizable dashboard with drag-drop widgets', 14, 4, 2, 'IN_PROGRESS', 1, 'HIGH', '2025-10-15', CURRENT_TIMESTAMP),
(4, 'User Preferences', 'Theme settings, notifications, and display preferences', 7, 4, 2, 'DONE', 2, 'LOW', '2025-10-15', CURRENT_TIMESTAMP);

-- ===========================================
-- SHAPED pitches for Cycle 5 (ready for betting)
-- ===========================================
INSERT INTO pitches (id, title, description, appetite_days, cycle_id, team_id, status, sort_order, priority, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(5, 'Mobile Offline Mode', 'Enable offline data caching and sync', 14, 5, NULL, 'SHAPED', 1, 'HIGH', '2025-11-01', CURRENT_TIMESTAMP),
(6, 'Push Notifications', 'Mobile push notifications for key events', 7, 5, NULL, 'SHAPED', 2, 'HIGH', '2025-11-01', CURRENT_TIMESTAMP),
(7, 'Biometric Login', 'Face ID and fingerprint authentication', 7, 5, NULL, 'SHAPED', 3, 'MEDIUM', '2025-11-01', CURRENT_TIMESTAMP),
(8, 'Activity Timeline', 'Show user activity history with detailed timeline', 10, 5, NULL, 'SHAPED', 4, 'MEDIUM', '2025-11-01', CURRENT_TIMESTAMP),
(9, 'Two-Factor Auth', 'Add 2FA support with TOTP apps', 14, 5, NULL, 'SHAPED', 5, 'HIGH', '2025-11-02', CURRENT_TIMESTAMP),
(10, 'Team Capacity View', 'Visual capacity planning for teams', 10, 5, NULL, 'SHAPED', 6, 'LOW', '2025-11-02', CURRENT_TIMESTAMP);

-- ===========================================
-- IDEA pitches (not yet shaped - in backlog for Cycle 5)
-- ===========================================
INSERT INTO pitches (id, title, description, appetite_days, cycle_id, team_id, status, sort_order, priority, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(11, 'AI Writing Assistant', 'AI-powered pitch description generator', 21, 5, NULL, 'IDEA', 1, 'HIGH', '2025-11-05', CURRENT_TIMESTAMP),
(12, 'Custom Reports', 'Build custom reports with drag-drop fields', 14, 5, NULL, 'IDEA', 2, 'MEDIUM', '2025-11-06', CURRENT_TIMESTAMP),
(13, 'Calendar Integration', 'Sync with Google Calendar and Outlook', 10, 5, NULL, 'IDEA', 3, 'LOW', '2025-11-07', CURRENT_TIMESTAMP);

-- ===========================================
-- Completed pitches from previous cycles
-- ===========================================
INSERT INTO pitches (id, title, description, appetite_days, cycle_id, team_id, status, sort_order, priority, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(14, 'User Authentication', 'Login, registration, and password reset', 14, 1, NULL, 'DONE', 1, 'HIGH', '2025-06-01', '2025-07-10'),
(15, 'Project Management', 'Create and manage projects', 7, 1, NULL, 'DONE', 2, 'HIGH', '2025-06-01', '2025-07-08'),
(16, 'Hill Chart Tracking', 'Visual progress tracking with hill charts', 14, 2, NULL, 'DONE', 1, 'MEDIUM', '2025-07-15', '2025-08-20'),
(17, 'Cycle Management', 'Create and manage Shape Up cycles', 10, 2, NULL, 'DONE', 2, 'MEDIUM', '2025-07-15', '2025-08-22'),
(18, 'Retrospectives', 'End-of-cycle retrospective boards', 7, 3, NULL, 'DONE', 1, 'LOW', '2025-09-01', '2025-09-28'),
(19, 'Bug Tracking', 'Bug reporting and tracking system', 10, 3, NULL, 'DONE', 2, 'MEDIUM', '2025-09-01', '2025-10-05');

-- ===========================================
-- CANCELLED pitches (for demo data)
-- ===========================================
INSERT INTO pitches (id, title, description, appetite_days, cycle_id, team_id, status, sort_order, priority, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(20, 'Blockchain Integration', 'Add blockchain-based audit trail', 28, 4, NULL, 'CANCELLED', 1, NULL, '2025-10-10', '2025-10-12');

-- ===========================================
-- HILL CHART POINTS (progress tracking)
-- Position: 0-50 = figuring out, 51-100 = making it happen
-- ===========================================
INSERT INTO hill_chart_points (id, pitch_id, scope, description, position, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
-- GitHub Integration scopes
(1, 1, 'OAuth Setup', 'Configure GitHub OAuth application', 85, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 'Webhook Handler', 'Handle GitHub webhook events', 65, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 'Issue Sync', 'Sync issues between systems', 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Slack Notifications scopes
(4, 2, 'Message Templates', 'Design notification message formats', 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 2, 'Channel Config', 'Configure target Slack channels', 70, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Dashboard Widgets scopes
(6, 3, 'Widget Framework', 'Core drag-drop widget system', 75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 3, 'Chart Widgets', 'Line, bar, pie chart widgets', 55, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 3, 'KPI Widgets', 'Key metric display widgets', 45, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- User Preferences (completed)
(9, 4, 'Theme Settings', 'Dark/light mode toggle', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 4, 'Notification Prefs', 'Email and push preferences', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- BETTING SLOTS
-- ===========================================
INSERT INTO betting_slots (id, cycle_id, team_id, position, start_date, end_date, pitch_id, notes, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 4, 1, 0, '2025-10-15', '2025-11-26', 1, 'Primary work for Alpha Team', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 4, 1, 1, '2025-10-15', '2025-11-26', 2, 'Secondary work for Alpha Team', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 4, 2, 0, '2025-10-15', '2025-11-26', 3, 'Primary work for Beta Team', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 4, 2, 1, '2025-10-15', '2025-11-26', 4, 'Secondary work for Beta Team', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- Empty slots for Cycle 5 betting
(5, 5, 3, 0, '2025-12-01', '2026-01-12', NULL, 'Gamma Team capacity slot 1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 5, 3, 1, '2025-12-01', '2026-01-12', NULL, 'Gamma Team capacity slot 2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Reset sequences
SELECT setval('pitches_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM pitches), false);
SELECT setval('hill_chart_points_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM hill_chart_points), false);
SELECT setval('betting_slots_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM betting_slots), false);
