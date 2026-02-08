-- V25: Add a closed cycle (Cycle 6) with completed retrospective
-- This demonstrates a fully completed Shape Up cycle with retro done

-- ===========================================
-- Cycle 6 - Completed Cycle in COOLDOWN (closed)
-- ===========================================

INSERT INTO cycles (id, name, start_date, end_date, phase, project_id, is_active) OVERRIDING SYSTEM VALUE VALUES
(6, 'Cycle 6 - Completed Sprint', '2025-10-01', '2025-11-15', 'COOLDOWN', 1, false);

-- ===========================================
-- Teams for Cycle 6
-- ===========================================

INSERT INTO teams (id, name, cycle_id) OVERRIDING SYSTEM VALUE VALUES
(6, 'Omega Team', 6),
(7, 'Sigma Team', 6);

-- Add team assignments for Cycle 6 teams
INSERT INTO team_assignments (person_id, team_id, role, start_date, end_date, is_active) VALUES
(2, 6, 'FRONTEND', '2025-10-01', '2025-11-15', false),
(3, 6, 'BACKEND', '2025-10-01', '2025-11-15', false),
(4, 7, 'DESIGNER', '2025-10-01', '2025-11-15', false),
(5, 7, 'FULLSTACK', '2025-10-01', '2025-11-15', false),
(6, 6, 'QA', '2025-10-01', '2025-11-15', false);

-- ===========================================
-- Completed Pitches for Cycle 6 (all DONE)
-- ===========================================

INSERT INTO pitches (id, title, description, appetite_days, cycle_id, team_id, status, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(15, 'Email Notification System', 'Comprehensive email notification system with templates and scheduling', 14, 6, 6, 'DONE', '2025-10-01 09:00:00', '2025-11-10 17:00:00'),
(16, 'User Dashboard Redesign', 'Complete redesign of the main user dashboard with improved UX', 10, 6, 6, 'DONE', '2025-10-01 09:00:00', '2025-11-08 16:00:00'),
(17, 'API Rate Limiting', 'Implement API rate limiting to protect against abuse', 7, 6, 7, 'DONE', '2025-10-01 09:00:00', '2025-10-20 14:00:00'),
(18, 'Audit Logging System', 'Track all user actions for compliance and debugging', 14, 6, 7, 'DONE', '2025-10-01 09:00:00', '2025-11-12 11:00:00');

-- ===========================================
-- Hill Chart Points for Cycle 6 Pitches (all at 100%)
-- ===========================================

-- Hill chart points for "Email Notification System" (Pitch 15)
INSERT INTO hill_chart_points (pitch_id, scope, description, position, created_at, updated_at) VALUES
(15, 'Email Templates', 'Design and implement email templates', 100, '2025-10-05 10:00:00', '2025-11-08 15:00:00'),
(15, 'Notification Queue', 'Background job queue for sending emails', 100, '2025-10-05 10:00:00', '2025-11-06 17:00:00'),
(15, 'User Preferences', 'Email preference settings per user', 100, '2025-10-05 10:00:00', '2025-11-09 14:00:00'),
(15, 'Email Analytics', 'Track open rates and click-through rates', 100, '2025-10-05 10:00:00', '2025-11-10 16:00:00');

-- Hill chart points for "User Dashboard Redesign" (Pitch 16)
INSERT INTO hill_chart_points (pitch_id, scope, description, position, created_at, updated_at) VALUES
(16, 'Dashboard Layout', 'New grid-based responsive layout', 100, '2025-10-03 09:00:00', '2025-11-03 17:00:00'),
(16, 'Widget System', 'Customizable dashboard widgets', 100, '2025-10-03 09:00:00', '2025-11-05 16:00:00'),
(16, 'Performance Optimization', 'Lazy loading and caching', 100, '2025-10-03 09:00:00', '2025-11-07 15:00:00');

-- Hill chart points for "API Rate Limiting" (Pitch 17)
INSERT INTO hill_chart_points (pitch_id, scope, description, position, created_at, updated_at) VALUES
(17, 'Rate Limiter Core', 'Token bucket algorithm implementation', 100, '2025-10-02 10:00:00', '2025-10-15 17:00:00'),
(17, 'Redis Integration', 'Distributed rate limiting with Redis', 100, '2025-10-02 10:00:00', '2025-10-18 16:00:00'),
(17, 'Admin Dashboard', 'Rate limit monitoring and configuration', 100, '2025-10-02 10:00:00', '2025-10-20 14:00:00');

-- Hill chart points for "Audit Logging System" (Pitch 18)
INSERT INTO hill_chart_points (pitch_id, scope, description, position, created_at, updated_at) VALUES
(18, 'Event Capture', 'Capture all user actions as events', 100, '2025-10-04 09:00:00', '2025-10-25 17:00:00'),
(18, 'Log Storage', 'Efficient storage and indexing of logs', 100, '2025-10-04 09:00:00', '2025-11-01 15:00:00'),
(18, 'Search & Filter', 'Advanced search and filtering UI', 100, '2025-10-04 09:00:00', '2025-11-08 16:00:00'),
(18, 'Export & Reporting', 'Generate audit reports', 100, '2025-10-04 09:00:00', '2025-11-12 11:00:00');

-- ===========================================
-- Work Logs for Cycle 6 Pitches
-- ===========================================

-- Work logs for Alice (Frontend Developer)
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(2, 15, '2025-10-06', 7.0, 'Built email template preview component'),
(2, 15, '2025-10-07', 6.5, 'Implemented template selection UI'),
(2, 16, '2025-10-14', 8.0, 'Started dashboard layout implementation'),
(2, 16, '2025-10-15', 7.5, 'Completed responsive grid system'),
(2, 16, '2025-10-21', 6.0, 'Widget drag-and-drop functionality'),
(2, 16, '2025-10-28', 7.0, 'Final polish and animations');

-- Work logs for Bob (Backend Developer)
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(3, 15, '2025-10-06', 8.0, 'Set up email service with SendGrid integration'),
(3, 15, '2025-10-08', 7.5, 'Implemented background job queue for emails'),
(3, 15, '2025-10-13', 6.5, 'Added email tracking with webhooks'),
(3, 17, '2025-10-16', 8.0, 'Implemented token bucket rate limiter'),
(3, 17, '2025-10-17', 7.0, 'Redis integration for distributed limiting');

-- Work logs for Carol (Designer)
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(4, 16, '2025-10-02', 6.0, 'Created dashboard wireframes and mockups'),
(4, 16, '2025-10-03', 7.0, 'Designed widget components and interactions'),
(4, 18, '2025-10-22', 5.5, 'Designed audit log viewer UI');

-- Work logs for David (Fullstack)
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(5, 17, '2025-10-10', 7.0, 'Rate limit configuration admin panel'),
(5, 18, '2025-10-23', 8.0, 'Built event capture middleware'),
(5, 18, '2025-10-30', 7.5, 'Implemented log search and filtering'),
(5, 18, '2025-11-05', 6.5, 'Export functionality and reports');

-- Work logs for Emma (QA)
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(6, 15, '2025-10-20', 5.0, 'Tested email delivery and templates'),
(6, 16, '2025-11-01', 6.0, 'Dashboard regression testing'),
(6, 17, '2025-10-19', 4.5, 'Rate limiting stress tests'),
(6, 18, '2025-11-10', 5.5, 'Audit log accuracy verification');

-- ===========================================
-- Meetings for Cycle 6
-- ===========================================

-- Kickoff meetings
INSERT INTO meetings (pitch_id, type, date_held, dor_ready, dod_ready, notes) VALUES
(15, 'KICKOFF', '2025-10-01', true, false, 'Kickoff for Email Notification System. Decided on SendGrid for email delivery.'),
(16, 'KICKOFF', '2025-10-01', true, false, 'Dashboard Redesign kickoff. Reviewed design mockups and agreed on implementation approach.'),
(17, 'KICKOFF', '2025-10-01', true, false, 'API Rate Limiting kickoff. Discussed token bucket vs leaky bucket algorithms.'),
(18, 'KICKOFF', '2025-10-01', true, false, 'Audit Logging kickoff. Defined event schema and storage strategy.');

-- Standup meetings
INSERT INTO meetings (pitch_id, type, date_held, dor_ready, dod_ready, notes) VALUES
(15, 'STANDUP', '2025-10-08', true, false, 'Email service integration complete. Moving to template system.'),
(16, 'STANDUP', '2025-10-16', true, false, 'Grid layout done. Starting widget implementation.'),
(17, 'STANDUP', '2025-10-18', true, false, 'Rate limiter working. Redis integration in progress.'),
(18, 'STANDUP', '2025-10-25', true, false, 'Event capture complete. Working on storage optimization.');

-- Hill Chart Review meetings
INSERT INTO meetings (pitch_id, type, date_held, dor_ready, dod_ready, notes) VALUES
(15, 'HILL_CHART_REVIEW', '2025-10-15', true, false, 'All scopes past the hill. On track for delivery.'),
(16, 'HILL_CHART_REVIEW', '2025-10-22', true, false, 'Good progress. Widget system at 75%, needs acceleration.'),
(17, 'HILL_CHART_REVIEW', '2025-10-19', true, true, 'Feature complete. Ready for final QA.'),
(18, 'HILL_CHART_REVIEW', '2025-11-05', true, false, 'Search & Filter behind. Team rallying to complete.');

-- Demo/Closeout meetings
INSERT INTO meetings (pitch_id, type, date_held, dor_ready, dod_ready, notes) VALUES
(15, 'DEMO', '2025-11-10', true, true, 'Successful demo to stakeholders. Feature approved and shipped.'),
(16, 'DEMO', '2025-11-08', true, true, 'Dashboard redesign demo. Great feedback from users.'),
(17, 'DEMO', '2025-10-20', true, true, 'Rate limiting demo. Performed well under load testing.'),
(18, 'DEMO', '2025-11-12', true, true, 'Audit logging complete. Compliance team approved.');

-- ===========================================
-- Tasks for Cycle 6 (all completed)
-- ===========================================

INSERT INTO tasks (title, description, status, priority, estimate_hours, actual_hours, cycle_id, assignee_id, pair_assignee_id, created_by_id, due_date, tags, created_at, updated_at) VALUES
('Update email service documentation', 'Document SendGrid integration and template system', 'DONE', 'MEDIUM', 4.0, 3.5, 6, 3, NULL, 3, '2025-11-08', 'documentation,backend', '2025-10-01 09:00:00', '2025-11-08 16:00:00'),
('Performance benchmarks for rate limiter', 'Run load tests and document results', 'DONE', 'HIGH', 6.0, 5.5, 6, 6, 3, 6, '2025-10-19', 'testing,performance', '2025-10-01 09:00:00', '2025-10-19 17:00:00'),
('Accessibility audit for dashboard', 'Ensure dashboard meets WCAG 2.1 AA standards', 'DONE', 'HIGH', 8.0, 9.0, 6, 2, NULL, 4, '2025-11-05', 'accessibility,frontend', '2025-10-01 09:00:00', '2025-11-05 18:00:00'),
('Audit log retention policy', 'Implement log rotation and archival', 'DONE', 'MEDIUM', 4.0, 4.5, 6, 5, NULL, 5, '2025-11-10', 'backend,infrastructure', '2025-10-01 09:00:00', '2025-11-10 15:00:00');

-- ===========================================
-- Completed Retrospective for Cycle 6
-- ===========================================

INSERT INTO retrospectives (id, title, notes, status, cycle_id, project_id, created_by_id, created_at, updated_at, closed_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Cycle 6 Retrospective', 'End of cycle retrospective to discuss what went well and areas for improvement in our completed sprint.', 'CLOSED', 6, 1, 2, '2025-11-13 10:00:00', '2025-11-14 16:00:00', '2025-11-14 16:00:00');

-- ===========================================
-- Retro Items for the Retrospective
-- ===========================================

-- WENT_WELL items
INSERT INTO retro_items (id, content, column_type, retrospective_id, author_id, vote_count, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Great team collaboration - everyone helped each other when stuck', 'WENT_WELL', 1, 2, 5, '2025-11-13 10:15:00', '2025-11-14 15:00:00'),
(2, 'Email notification system shipped ahead of schedule', 'WENT_WELL', 1, 3, 4, '2025-11-13 10:18:00', '2025-11-14 15:00:00'),
(3, 'Daily standups were efficient and focused', 'WENT_WELL', 1, 4, 3, '2025-11-13 10:22:00', '2025-11-14 15:00:00'),
(4, 'Hill chart helped us visualize progress clearly', 'WENT_WELL', 1, 5, 4, '2025-11-13 10:25:00', '2025-11-14 15:00:00'),
(5, 'QA caught critical bugs early in the process', 'WENT_WELL', 1, 6, 3, '2025-11-13 10:30:00', '2025-11-14 15:00:00');

-- DID_NOT_GO_WELL items
INSERT INTO retro_items (id, content, column_type, retrospective_id, author_id, vote_count, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(6, 'Audit logging feature scope creep - took longer than expected', 'DID_NOT_GO_WELL', 1, 5, 4, '2025-11-13 10:35:00', '2025-11-14 15:00:00'),
(7, 'Lack of documentation slowed down onboarding new team member', 'DID_NOT_GO_WELL', 1, 3, 3, '2025-11-13 10:40:00', '2025-11-14 15:00:00'),
(8, 'Dashboard performance issues discovered late in the cycle', 'DID_NOT_GO_WELL', 1, 2, 2, '2025-11-13 10:45:00', '2025-11-14 15:00:00'),
(9, 'Too many meetings interrupted deep work sessions', 'DID_NOT_GO_WELL', 1, 4, 5, '2025-11-13 10:50:00', '2025-11-14 15:00:00');

-- TRY_NEXT items
INSERT INTO retro_items (id, content, column_type, retrospective_id, author_id, vote_count, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(10, 'Implement feature flags for safer deployments', 'TRY_NEXT', 1, 3, 4, '2025-11-13 11:00:00', '2025-11-14 15:00:00'),
(11, 'Schedule focus blocks - no meetings during morning hours', 'TRY_NEXT', 1, 4, 5, '2025-11-13 11:05:00', '2025-11-14 15:00:00'),
(12, 'Add performance testing earlier in the cycle', 'TRY_NEXT', 1, 6, 3, '2025-11-13 11:10:00', '2025-11-14 15:00:00'),
(13, 'Create technical design docs before implementation', 'TRY_NEXT', 1, 5, 4, '2025-11-13 11:15:00', '2025-11-14 15:00:00');

-- ACTIONS items
INSERT INTO retro_items (id, content, column_type, retrospective_id, author_id, vote_count, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(14, 'ACTION: Bob to set up feature flag infrastructure by next cycle', 'ACTIONS', 1, 3, 0, '2025-11-13 11:30:00', '2025-11-14 15:00:00'),
(15, 'ACTION: Carol to propose no-meeting mornings policy to management', 'ACTIONS', 1, 4, 0, '2025-11-13 11:35:00', '2025-11-14 15:00:00'),
(16, 'ACTION: Emma to integrate performance tests into CI pipeline', 'ACTIONS', 1, 6, 0, '2025-11-13 11:40:00', '2025-11-14 15:00:00'),
(17, 'ACTION: David to create ADR template for technical decisions', 'ACTIONS', 1, 5, 0, '2025-11-13 11:45:00', '2025-11-14 15:00:00');

-- ===========================================
-- Retro Item Votes (who voted for what)
-- ===========================================

-- Votes for "Great team collaboration" (item 1)
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(1, 2, '2025-11-14 14:00:00'),
(1, 3, '2025-11-14 14:02:00'),
(1, 4, '2025-11-14 14:05:00'),
(1, 5, '2025-11-14 14:08:00'),
(1, 6, '2025-11-14 14:10:00');

-- Votes for "Email notification shipped early" (item 2)
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(2, 2, '2025-11-14 14:01:00'),
(2, 3, '2025-11-14 14:03:00'),
(2, 5, '2025-11-14 14:07:00'),
(2, 6, '2025-11-14 14:11:00');

-- Votes for "Daily standups efficient" (item 3)
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(3, 2, '2025-11-14 14:02:00'),
(3, 4, '2025-11-14 14:06:00'),
(3, 6, '2025-11-14 14:12:00');

-- Votes for "Hill chart visualization" (item 4)
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(4, 2, '2025-11-14 14:03:00'),
(4, 3, '2025-11-14 14:04:00'),
(4, 4, '2025-11-14 14:07:00'),
(4, 5, '2025-11-14 14:09:00');

-- Votes for "QA caught bugs early" (item 5)
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(5, 3, '2025-11-14 14:05:00'),
(5, 4, '2025-11-14 14:08:00'),
(5, 5, '2025-11-14 14:10:00');

-- Votes for "Scope creep in audit logging" (item 6)
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(6, 2, '2025-11-14 14:04:00'),
(6, 3, '2025-11-14 14:06:00'),
(6, 4, '2025-11-14 14:09:00'),
(6, 6, '2025-11-14 14:13:00');

-- Votes for "Lack of documentation" (item 7)
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(7, 2, '2025-11-14 14:05:00'),
(7, 5, '2025-11-14 14:11:00'),
(7, 6, '2025-11-14 14:14:00');

-- Votes for "Dashboard performance issues" (item 8)
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(8, 3, '2025-11-14 14:07:00'),
(8, 6, '2025-11-14 14:15:00');

-- Votes for "Too many meetings" (item 9)
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(9, 2, '2025-11-14 14:06:00'),
(9, 3, '2025-11-14 14:08:00'),
(9, 4, '2025-11-14 14:10:00'),
(9, 5, '2025-11-14 14:12:00'),
(9, 6, '2025-11-14 14:16:00');

-- Votes for "Feature flags" (item 10)
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(10, 2, '2025-11-14 14:07:00'),
(10, 3, '2025-11-14 14:09:00'),
(10, 5, '2025-11-14 14:13:00'),
(10, 6, '2025-11-14 14:17:00');

-- Votes for "No-meeting mornings" (item 11)
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(11, 2, '2025-11-14 14:08:00'),
(11, 3, '2025-11-14 14:10:00'),
(11, 4, '2025-11-14 14:11:00'),
(11, 5, '2025-11-14 14:14:00'),
(11, 6, '2025-11-14 14:18:00');

-- Votes for "Earlier performance testing" (item 12)
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(12, 2, '2025-11-14 14:09:00'),
(12, 3, '2025-11-14 14:11:00'),
(12, 6, '2025-11-14 14:19:00');

-- Votes for "Technical design docs" (item 13)
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(13, 3, '2025-11-14 14:12:00'),
(13, 4, '2025-11-14 14:13:00'),
(13, 5, '2025-11-14 14:15:00'),
(13, 6, '2025-11-14 14:20:00');
