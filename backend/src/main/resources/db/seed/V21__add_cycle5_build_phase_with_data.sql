-- V21: Add Cycle 5 in BUILD phase (after betting) with work logs, hill chart, meetings, and tasks
-- This cycle demonstrates the build phase where development is actively happening

-- ===========================================
-- Cycle 5 - Active BUILD Phase
-- ===========================================

INSERT INTO cycles OVERRIDING SYSTEM VALUE (id, name, start_date, end_date, phase, project_id, is_active) VALUES
(5, 'Cycle 5 - Build Sprint', '2026-01-05', '2026-02-13', 'BUILD', 1, true);

-- ===========================================
-- Teams for Cycle 5
-- ===========================================

INSERT INTO teams OVERRIDING SYSTEM VALUE (id, name, cycle_id) VALUES
(4, 'Phoenix Team', 5),
(5, 'Dragon Team', 5);

-- Add team assignments for Cycle 5 teams
INSERT INTO team_assignments (person_id, team_id, role, start_date, is_active) VALUES
(2, 4, 'FRONTEND', '2026-01-05', true),
(3, 4, 'BACKEND', '2026-01-05', true),
(6, 4, 'QA', '2026-01-05', true),
(4, 5, 'DESIGNER', '2026-01-05', true),
(5, 5, 'FULLSTACK', '2026-01-05', true);

-- ===========================================
-- Pitches for Cycle 5 (IN_PROGRESS status - actively being built)
-- ===========================================

INSERT INTO pitches OVERRIDING SYSTEM VALUE (id, title, description, appetite_days, cycle_id, team_id, status, created_at, updated_at) VALUES
(11, 'Real-time Notifications', 'Implement WebSocket-based real-time notifications for user activities, mentions, and updates', 14, 5, 4, 'IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 'Advanced Search', 'Full-text search with filters, autocomplete, and recent searches', 10, 5, 4, 'IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 'Data Export Feature', 'Allow users to export their data in CSV, JSON, and PDF formats', 7, 5, 5, 'IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, 'Team Collaboration Hub', 'Shared workspace for team collaboration with comments and @mentions', 14, 5, 5, 'STARTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- Hill Chart Points for Cycle 5 Pitches
-- ===========================================

-- Hill chart points for "Real-time Notifications" (Pitch 11)
INSERT INTO hill_chart_points (pitch_id, scope, description, position, created_at, updated_at) VALUES
(11, 'WebSocket Infrastructure', 'Set up WebSocket server and connection management', 85, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 'Notification Service', 'Backend service for creating and dispatching notifications', 70, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 'Notification UI Components', 'Toast notifications, notification center, and badge indicators', 55, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 'Notification Preferences', 'User settings for notification types and delivery methods', 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Hill chart points for "Advanced Search" (Pitch 12)
INSERT INTO hill_chart_points (pitch_id, scope, description, position, created_at, updated_at) VALUES
(12, 'Search Index Setup', 'Configure Elasticsearch/full-text search engine', 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 'Search API', 'RESTful endpoints for search queries and filters', 75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 'Search UI', 'Search bar with autocomplete and filter panels', 45, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 'Recent Searches', 'Store and display user recent search history', 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Hill chart points for "Data Export Feature" (Pitch 13)
INSERT INTO hill_chart_points (pitch_id, scope, description, position, created_at, updated_at) VALUES
(13, 'Export Service Backend', 'Service layer for generating export files', 80, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 'CSV Export', 'Export data to CSV format with proper formatting', 95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 'JSON Export', 'Export data to JSON format', 85, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 'PDF Report Generation', 'Generate formatted PDF reports', 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 'Export UI & Progress', 'UI for selecting export options and showing progress', 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Hill chart points for "Team Collaboration Hub" (Pitch 14)
INSERT INTO hill_chart_points (pitch_id, scope, description, position, created_at, updated_at) VALUES
(14, 'Collaboration Data Model', 'Database schema for workspaces, comments, mentions', 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, 'Comments System', 'Add, edit, delete comments with threading', 35, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, 'Mentions & Linking', '@mentions for users and linking to items', 25, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, 'Activity Feed', 'Real-time activity feed for workspace', 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- Work Logs for Cycle 5 Pitches
-- ===========================================

-- Work logs for Alice (Frontend Developer) on Pitch 11 & 12
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(2, 11, '2026-01-06', 6.5, 'Set up WebSocket client infrastructure and connection handling'),
(2, 11, '2026-01-07', 7.0, 'Implemented notification toast component and animation'),
(2, 11, '2026-01-08', 5.5, 'Built notification center dropdown with unread indicators'),
(2, 12, '2026-01-09', 8.0, 'Created search bar component with debounced autocomplete'),
(2, 12, '2026-01-10', 6.0, 'Implemented filter panel with checkboxes and date range picker');

-- Work logs for Bob (Backend Developer) on Pitch 11 & 12
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(3, 11, '2026-01-06', 8.0, 'Configured WebSocket server with STOMP protocol'),
(3, 11, '2026-01-07', 7.5, 'Built notification service with event-driven architecture'),
(3, 11, '2026-01-08', 6.0, 'Added notification persistence and delivery tracking'),
(3, 12, '2026-01-09', 7.0, 'Set up Elasticsearch integration and indexing pipeline'),
(3, 12, '2026-01-10', 8.0, 'Implemented search API with pagination and relevance scoring');

-- Work logs for Emma (QA) on Pitch 11
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(6, 11, '2026-01-08', 4.0, 'Wrote test cases for WebSocket connection scenarios'),
(6, 11, '2026-01-09', 5.0, 'Tested notification delivery and preferences'),
(6, 12, '2026-01-10', 3.5, 'Started search functionality testing');

-- Work logs for Carol (Designer) on Pitch 14
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(4, 14, '2026-01-06', 6.0, 'Created wireframes for collaboration hub layout'),
(4, 14, '2026-01-07', 7.0, 'Designed comment thread UI and interaction patterns'),
(4, 14, '2026-01-08', 5.0, 'Finalized mention popup and activity feed design');

-- Work logs for David (Fullstack) on Pitch 13 & 14
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(5, 13, '2026-01-06', 7.0, 'Built export service foundation and file generation'),
(5, 13, '2026-01-07', 8.0, 'Implemented CSV and JSON export with proper data formatting'),
(5, 13, '2026-01-08', 6.5, 'Added PDF generation using Apache PDFBox'),
(5, 14, '2026-01-09', 7.0, 'Created database schema for collaboration features'),
(5, 14, '2026-01-10', 6.0, 'Started implementing comments API');

-- ===========================================
-- Meetings for Cycle 5
-- ===========================================

-- Kickoff meeting for Cycle 5
INSERT INTO meetings (pitch_id, type, date_held, dor_ready, dod_ready, notes) VALUES
(11, 'KICKOFF', '2026-01-05', true, false, 'Kickoff meeting for Real-time Notifications. Team reviewed requirements, identified technical risks with WebSocket scaling. DOR checklist completed.'),
(12, 'KICKOFF', '2026-01-05', true, false, 'Kickoff for Advanced Search. Discussed Elasticsearch vs PostgreSQL full-text search. Decided on Elasticsearch for better relevance scoring.'),
(13, 'KICKOFF', '2026-01-05', true, false, 'Data Export kickoff. Reviewed export format requirements and agreed on async generation for large datasets.'),
(14, 'KICKOFF', '2026-01-05', true, false, 'Team Collaboration Hub kickoff. Discussed real-time sync requirements and integration with notifications system.');

-- Standup meetings
INSERT INTO meetings (pitch_id, type, date_held, dor_ready, dod_ready, notes) VALUES
(11, 'STANDUP', '2026-01-07', true, false, 'Day 2 standup: WebSocket server up and running. Frontend connection handling in progress. No blockers.'),
(11, 'STANDUP', '2026-01-08', true, false, 'Day 3 standup: Notification service completed. UI components making good progress. Need to discuss preference storage.'),
(12, 'STANDUP', '2026-01-10', true, false, 'Search API complete with basic functionality. Working on advanced filters and UI polish.');

-- Hill Chart Review meetings
INSERT INTO meetings (pitch_id, type, date_held, dor_ready, dod_ready, notes) VALUES
(11, 'HILL_CHART_REVIEW', '2026-01-08', true, false, 'Hill chart review: Most items past the hill. WebSocket infrastructure at 85%, Notification Service at 70%. Good trajectory.'),
(12, 'HILL_CHART_REVIEW', '2026-01-10', true, false, 'Search feature review: Backend work solid, frontend needs acceleration. Search Index at 90%, UI at 45%.'),
(13, 'HILL_CHART_REVIEW', '2026-01-09', true, false, 'Export feature well ahead of schedule. CSV and JSON exports complete, PDF in progress.');

-- Demo meeting scheduled
INSERT INTO meetings (pitch_id, type, date_held, dor_ready, dod_ready, notes) VALUES
(11, 'DEMO', '2026-01-15', false, false, 'Scheduled demo for Real-time Notifications feature. Stakeholders invited.');

-- ===========================================
-- Tasks for Cycle 5 (Independent work items)
-- ===========================================

INSERT INTO tasks (title, description, status, priority, estimate_hours, actual_hours, cycle_id, assignee_id, pair_assignee_id, created_by_id, due_date, tags, created_at, updated_at) VALUES
-- Technical debt tasks
('Refactor authentication middleware', 'Clean up auth middleware code and add better error handling', 'IN_PROGRESS', 'HIGH', 8.0, 4.5, 5, 3, NULL, 3, '2026-01-20', 'tech-debt,backend,security', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Update deprecated dependencies', 'Update all npm packages with security vulnerabilities', 'TODO', 'HIGH', 4.0, NULL, 5, 2, NULL, 2, '2026-01-15', 'tech-debt,frontend,security', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Database query optimization', 'Optimize slow queries identified in APM dashboard', 'TODO', 'MEDIUM', 6.0, NULL, 5, 3, NULL, 3, '2026-01-18', 'tech-debt,backend,performance', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Bug fixes
('Fix notification sound on Safari', 'Notification sound not playing on Safari mobile', 'IN_PROGRESS', 'MEDIUM', 3.0, 1.5, 5, 2, NULL, 6, '2026-01-12', 'bug,frontend,mobile', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Handle WebSocket reconnection', 'Add automatic reconnection logic for dropped WebSocket connections', 'TODO', 'HIGH', 4.0, NULL, 5, 3, 2, 3, '2026-01-14', 'bug,backend,reliability', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Documentation tasks
('API documentation update', 'Update Swagger docs for new notification endpoints', 'IN_PROGRESS', 'MEDIUM', 3.0, 2.0, 5, 3, NULL, 4, '2026-01-16', 'documentation,backend,api', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Write user guide for export feature', 'Create help documentation for data export functionality', 'BACKLOG', 'LOW', 4.0, NULL, 5, 4, NULL, 4, '2026-01-25', 'documentation,user-guide', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Infrastructure tasks
('Set up staging environment', 'Configure new staging environment for QA testing', 'DONE', 'HIGH', 6.0, 5.5, 5, 5, NULL, 5, '2026-01-08', 'infrastructure,devops', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Configure monitoring alerts', 'Set up PagerDuty alerts for critical metrics', 'IN_PROGRESS', 'MEDIUM', 3.0, 1.0, 5, 5, NULL, 5, '2026-01-20', 'infrastructure,monitoring', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Testing tasks
('Write E2E tests for notifications', 'Cypress tests for notification flow', 'TODO', 'MEDIUM', 8.0, NULL, 5, 6, 2, 6, '2026-01-22', 'testing,e2e,frontend', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Performance testing for search', 'Load test search API with realistic data volumes', 'BACKLOG', 'MEDIUM', 5.0, NULL, 5, 6, NULL, 6, '2026-01-28', 'testing,performance,backend', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Feature enhancement tasks
('Add keyboard shortcuts', 'Implement keyboard navigation for power users', 'TODO', 'LOW', 6.0, NULL, 5, 2, NULL, 4, '2026-01-30', 'enhancement,frontend,ux', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Implement dark mode for exports', 'Support dark mode styling in PDF exports', 'BACKLOG', 'LOW', 3.0, NULL, 5, 5, NULL, 4, '2026-02-05', 'enhancement,frontend,ux', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- Reset sequences to avoid primary key conflicts
-- ===========================================

ALTER TABLE cycles ALTER COLUMN id RESTART WITH 200;
ALTER TABLE teams ALTER COLUMN id RESTART WITH 200;
ALTER TABLE pitches ALTER COLUMN id RESTART WITH 200;
ALTER TABLE hill_chart_points ALTER COLUMN id RESTART WITH 200;
ALTER TABLE work_logs ALTER COLUMN id RESTART WITH 200;
ALTER TABLE meetings ALTER COLUMN id RESTART WITH 200;
ALTER TABLE tasks ALTER COLUMN id RESTART WITH 200;
