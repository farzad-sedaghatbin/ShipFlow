-- ============================================================================
-- V1005: Advanced features seed data
-- Betting decisions, cooldown activities, work logs, meetings
-- ============================================================================

-- ===========================================
-- BETTING DECISIONS
-- Decision: COMMITTED, REJECTED, DEFERRED, NEEDS_SHAPING
-- ===========================================
INSERT INTO betting_decisions (id, pitch_id, cycle_id, decision, reason, decided_by_id, decided_at, requested_appetite_days, available_capacity_days, appetite_comparison_notes, considered_team_id, priority_score, strategic_alignment_notes) OVERRIDING SYSTEM VALUE VALUES
-- Cycle 4 betting decisions
(1, 1, 4, 'COMMITTED', 'GitHub integration is high priority for Q4 goals. Strong ROI expected.', 7, '2025-10-14 10:00:00', 14, 28, 'Fits well within Alpha Team capacity', 1, 95, 'Aligns with developer experience initiative'),
(2, 2, 4, 'COMMITTED', 'Slack notifications will improve team visibility and communication.', 7, '2025-10-14 10:15:00', 7, 14, 'Small batch, good fit for remaining capacity', 1, 85, 'Supports async communication goal'),
(3, 3, 4, 'COMMITTED', 'Dashboard customization frequently requested by users.', 7, '2025-10-14 10:30:00', 14, 28, 'Primary focus for Beta Team', 2, 90, 'Improves user engagement metrics'),
(4, 4, 4, 'COMMITTED', 'User preferences needed for better UX. Small scope.', 7, '2025-10-14 10:45:00', 7, 14, 'Good secondary work for Beta Team', 2, 75, 'Part of personalization initiative'),
(5, 20, 4, 'DEFERRED', 'Blockchain integration is too speculative at this stage. Not enough customer demand.', 7, '2025-10-14 11:00:00', 28, 28, 'Would consume full team capacity', NULL, 30, 'Does not align with current roadmap');

-- ===========================================
-- COOLDOWN ACTIVITIES
-- Type: TECH_DEBT, BUG_FIX, REFACTORING, KICKOFF_PREP, LEARNING, QA, DOCUMENTATION, etc.
-- Status: PLANNED, IN_PROGRESS, COMPLETED, SKIPPED, BLOCKED
-- ===========================================
INSERT INTO cooldown_activities (id, cycle_id, activity_type, title, description, status, assignee_id, created_by_id, estimated_hours, actual_hours, priority, notes, started_at, completed_at, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
-- Cycle 3 cooldown (completed)
(1, 3, 'TECH_DEBT', 'Clean up deprecated API endpoints', 'Remove v1 API endpoints no longer in use', 'COMPLETED', 3, 7, 8, 6, 1, 'Cleaned up 12 deprecated endpoints', '2025-10-13', '2025-10-14', '2025-10-12', '2025-10-14'),
(2, 3, 'DOCUMENTATION', 'Update API documentation', 'Sync OpenAPI spec with current implementation', 'COMPLETED', 3, 7, 4, 5, 2, 'Docs now auto-generated from code', '2025-10-14', '2025-10-14', '2025-10-12', '2025-10-14'),
(3, 3, 'KICKOFF_PREP', 'Cycle 4 kickoff preparation', 'Prepare materials for cycle 4 kickoff meeting', 'COMPLETED', 7, 7, 4, 3, 1, 'Kickoff successful', '2025-10-14', '2025-10-15', '2025-10-12', '2025-10-15'),
(4, 3, 'LEARNING', 'GitHub API exploration', 'Research GitHub API for upcoming integration', 'COMPLETED', 2, 7, 6, 8, 3, 'Good preparation for cycle 4 work', '2025-10-13', '2025-10-14', '2025-10-12', '2025-10-14'),

-- Cycle 4 cooldown (in progress - future)
(5, 4, 'BUG_FIX', 'Address critical bug backlog', 'Fix P1 bugs from cycle 4', 'PLANNED', 6, 7, 12, NULL, 1, 'Review bug list at cycle end', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 4, 'REFACTORING', 'Extract notification service', 'Refactor notification code into separate service', 'PLANNED', 3, 7, 8, NULL, 2, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 4, 'QA', 'Regression test suite review', 'Update and run full regression tests', 'PLANNED', 6, 7, 6, NULL, 2, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- WORK LOGS
-- ===========================================
INSERT INTO work_logs (id, person_id, pitch_id, date, hours_spent, note) OVERRIDING SYSTEM VALUE VALUES
-- GitHub Integration work logs
(1, 2, 1, '2025-10-16', 6.0, 'Setup OAuth app and initial configuration'),
(2, 2, 1, '2025-10-17', 7.5, 'Implemented OAuth callback flow'),
(3, 2, 1, '2025-10-18', 4.0, 'Fixed token refresh logic'),
(4, 3, 1, '2025-10-19', 8.0, 'Started webhook endpoint implementation'),
(5, 3, 1, '2025-10-20', 6.0, 'Webhook signature verification'),
(6, 2, 1, '2025-10-21', 5.5, 'OAuth error handling improvements'),
(7, 3, 1, '2025-10-22', 7.0, 'PR event parsing'),

-- Slack Notifications work logs
(8, 2, 2, '2025-10-20', 4.0, 'Slack API client setup'),
(9, 2, 2, '2025-10-21', 5.0, 'Message block templates'),
(10, 4, 2, '2025-10-22', 3.0, 'Notification message designs'),

-- Dashboard Widgets work logs
(11, 5, 3, '2025-10-18', 8.0, 'React DnD setup and basic grid'),
(12, 5, 3, '2025-10-19', 7.0, 'Widget component architecture'),
(13, 5, 3, '2025-10-20', 6.5, 'Drag-drop interactions'),
(14, 4, 3, '2025-10-21', 4.0, 'Widget styling and themes'),

-- User Preferences work logs
(15, 5, 4, '2025-10-16', 4.0, 'Theme toggle implementation'),
(16, 5, 4, '2025-10-17', 5.0, 'Notification preferences UI'),
(17, 5, 4, '2025-10-18', 3.5, 'Settings persistence');

-- ===========================================
-- MEETINGS
-- Type: KICKOFF, DAILY_STANDUP, HILL_REVIEW, SHAPING, BETTING, RETROSPECTIVE, AD_HOC
-- ===========================================
INSERT INTO meetings (id, pitch_id, type, date_held, dor_ready, dod_ready, notes, project_id) OVERRIDING SYSTEM VALUE VALUES
(1, 1, 'KICKOFF', '2025-10-16', true, false, 'Kicked off GitHub integration work. Team aligned on scope.', 1),
(2, 1, 'HILL_REVIEW', '2025-10-20', true, false, 'OAuth at 85%, webhooks at 40%. Good progress.', 1),
(3, 2, 'KICKOFF', '2025-10-20', true, false, 'Slack notifications kickoff. Design review passed.', 1),
(4, 3, 'KICKOFF', '2025-10-18', true, false, 'Dashboard widgets kickoff with Beta team.', 1),
(5, 3, 'HILL_REVIEW', '2025-10-25', true, false, 'Widget framework coming along well.', 1),
(6, NULL, 'BETTING', '2025-10-14', true, true, 'Cycle 4 betting ceremony. 4 pitches committed.', 1),
(7, NULL, 'RETROSPECTIVE', '2025-10-12', true, true, 'Cycle 3 retrospective completed.', 1),
(8, NULL, 'SHAPING', '2025-11-01', false, false, 'Cycle 5 shaping session. Mobile features discussed.', 1);

-- Reset sequences
SELECT setval('betting_decisions_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM betting_decisions), false);
SELECT setval('cooldown_activities_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM cooldown_activities), false);
SELECT setval('work_logs_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM work_logs), false);
SELECT setval('meetings_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM meetings), false);
