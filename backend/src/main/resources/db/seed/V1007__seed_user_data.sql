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
-- Bug comments (BUG_REPORT not BUG)
(1, 'This is related to Safari''s handling of disabled attribute on buttons.', 'BUG_REPORT', 1, 2, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'),
(2, 'Fixed in PR #234. Please verify on your device.', 'BUG_REPORT', 1, 2, CURRENT_TIMESTAMP - INTERVAL '6 hours', CURRENT_TIMESTAMP - INTERVAL '6 hours'),
-- Task comments
(3, 'Blocked waiting for API access. Requested access from admin.', 'TASK', 3, 3, CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '4 days'),
(4, 'Access granted, unblocked now.', 'TASK', 3, 1, CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '4 days');

-- ===========================================
-- COMMENT REACTIONS
-- ===========================================
INSERT INTO comment_reactions (comment_id, user_id, reaction_type, created_at) VALUES
(1, 2, 'THUMBS_UP', CURRENT_TIMESTAMP - INTERVAL '1 day'),
(2, 6, 'EYES', CURRENT_TIMESTAMP - INTERVAL '5 hours');

-- ===========================================
-- FINAL SEQUENCE RESETS (ensure all sequences are correct)
-- ===========================================
SELECT setval('comments_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM comments), false);
