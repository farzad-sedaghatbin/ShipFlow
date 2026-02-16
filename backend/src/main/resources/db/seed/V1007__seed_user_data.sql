-- ============================================================================
-- V1007: User preferences, notifications, and final cleanup
-- ============================================================================

-- ===========================================
-- USER PREFERENCES
-- Theme: LIGHT, DARK, SYSTEM
-- ===========================================
INSERT INTO user_preferences (user_id, theme_mode, compact_view, enable_animations, created_at, updated_at) 
SELECT id, 'SYSTEM', false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users 
WHERE id NOT IN (SELECT user_id FROM user_preferences)
ON CONFLICT (user_id) DO NOTHING;

-- Update some preferences for demo
UPDATE user_preferences SET theme_mode = 'DARK', compact_view = true WHERE user_id = 2;
UPDATE user_preferences SET theme_mode = 'LIGHT' WHERE user_id = 7;

-- ===========================================
-- NOTIFICATIONS (sample notifications)
-- ===========================================
INSERT INTO notifications (id, user_id, type, title, message, data, is_read, created_at) OVERRIDING SYSTEM VALUE VALUES
-- Recent notifications
(1, 2, 'PITCH_UPDATE', 'Pitch Updated', 'GitHub Integration pitch moved to IN_PROGRESS', '{"pitchId":1}', false, CURRENT_TIMESTAMP - INTERVAL '1 hour'),
(2, 2, 'TASK_ASSIGNED', 'Task Assigned', 'You have been assigned to "Create Webhook Endpoint"', '{"taskId":3}', false, CURRENT_TIMESTAMP - INTERVAL '2 hours'),
(3, 3, 'BUG_ASSIGNED', 'Bug Assigned', 'BUG-002: GitHub webhook timeout assigned to you', '{"bugId":2}', true, CURRENT_TIMESTAMP - INTERVAL '1 day'),
(4, 5, 'MEETING_REMINDER', 'Meeting Reminder', 'Hill Review meeting in 1 hour', '{"meetingId":2}', true, CURRENT_TIMESTAMP - INTERVAL '2 days'),
(5, 6, 'TEST_RUN_FAILED', 'Test Failed', 'TC-005 failed in latest test run', '{"testRunId":5}', true, CURRENT_TIMESTAMP - INTERVAL '3 days'),
(6, 7, 'CYCLE_PHASE_CHANGE', 'Cycle Phase Changed', 'Cycle 4 moved to BUILDING phase', '{"cycleId":4}', true, CURRENT_TIMESTAMP - INTERVAL '7 days'),
(7, 7, 'RETRO_CREATED', 'Retrospective Created', 'Cycle 4 Retrospective is now open', '{"retroId":4}', false, CURRENT_TIMESTAMP - INTERVAL '1 day');

-- ===========================================
-- HILL CHART HISTORY (position change tracking)
-- ===========================================
INSERT INTO hill_chart_history (hill_chart_point_id, pitch_id, user_id, previous_position, new_position, confidence_level, note, created_at) VALUES
(1, 1, 2, 0, 30, 3, 'Started OAuth setup', CURRENT_TIMESTAMP - INTERVAL '7 days'),
(1, 1, 2, 30, 60, 4, 'OAuth working, moving to test', CURRENT_TIMESTAMP - INTERVAL '5 days'),
(1, 1, 2, 60, 85, 5, 'OAuth complete, minor refinements remaining', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(2, 1, 3, 0, 25, 3, 'Research complete, starting implementation', CURRENT_TIMESTAMP - INTERVAL '6 days'),
(2, 1, 3, 25, 45, 4, 'Basic handler working', CURRENT_TIMESTAMP - INTERVAL '4 days'),
(2, 1, 3, 45, 65, 4, 'Event parsing implemented', CURRENT_TIMESTAMP - INTERVAL '1 day'),
(6, 3, 5, 0, 40, 3, 'DnD framework selected and POC done', CURRENT_TIMESTAMP - INTERVAL '5 days'),
(6, 3, 5, 40, 75, 4, 'Grid system working well', CURRENT_TIMESTAMP - INTERVAL '2 days');

-- ===========================================
-- COMMENTS (on pitches, tasks, bugs)
-- ===========================================
INSERT INTO comments (id, content, entity_type, entity_id, author_id, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
-- Pitch comments
(1, 'Great progress on the OAuth flow! Token refresh is working smoothly now.', 'PITCH', 1, 3, CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),
(2, 'Should we add rate limiting to the webhook endpoint? Getting a lot of events.', 'PITCH', 1, 2, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(3, 'Good idea @alice. Added task for rate limiting in cooldown.', 'PITCH', 1, 3, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
-- Bug comments
(4, 'This is related to Safari''s handling of disabled attribute on buttons.', 'BUG', 1, 2, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'),
(5, 'Fixed in PR #234. Please verify on your device.', 'BUG', 1, 2, CURRENT_TIMESTAMP - INTERVAL '6 hours', CURRENT_TIMESTAMP - INTERVAL '6 hours'),
-- Task comments
(6, 'Blocked waiting for API access. Requested access from admin.', 'TASK', 3, 3, CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '4 days'),
(7, 'Access granted, unblocked now.', 'TASK', 3, 1, CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '4 days');

-- ===========================================
-- COMMENT REACTIONS
-- ===========================================
INSERT INTO comment_reactions (comment_id, user_id, reaction_type, created_at) VALUES
(1, 2, 'THUMBS_UP', CURRENT_TIMESTAMP - INTERVAL '3 days'),
(1, 7, 'THUMBS_UP', CURRENT_TIMESTAMP - INTERVAL '3 days'),
(2, 3, 'THINKING', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(3, 2, 'THUMBS_UP', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(5, 6, 'EYES', CURRENT_TIMESTAMP - INTERVAL '5 hours');

-- ===========================================
-- FINAL SEQUENCE RESETS (ensure all sequences are correct)
-- ===========================================
SELECT setval('notifications_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM notifications), false);
SELECT setval('comments_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM comments), false);
