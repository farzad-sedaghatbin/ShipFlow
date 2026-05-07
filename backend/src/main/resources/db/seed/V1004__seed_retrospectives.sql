-- ============================================================================
-- V1004: Retrospectives seed data
-- Status: OPEN, CLOSED
-- Valid RetroColumnType values: WENT_WELL, DID_NOT_GO_WELL, TRY_NEXT, ACTIONS
-- ============================================================================

-- ===========================================
-- RETROSPECTIVES
-- ===========================================
INSERT INTO retrospectives (id, title, notes, status, cycle_id, project_id, created_by_id, created_at, updated_at, closed_at) OVERRIDING SYSTEM VALUE VALUES
-- Completed retrospectives from past cycles
(1, 'Cycle 1 Retrospective - Foundation', 'First cycle retro - learning lessons from initial development', 'CLOSED', 1, 1, 7, '2025-07-10', '2025-07-12', '2025-07-12'),
(2, 'Cycle 2 Retrospective - Core Features', 'Good velocity achieved, some integration challenges', 'CLOSED', 2, 1, 7, '2025-08-24', '2025-08-26', '2025-08-26'),
(3, 'Cycle 3 Retrospective - Polish', 'Focus on quality paid off', 'CLOSED', 3, 1, 7, '2025-10-10', '2025-10-12', '2025-10-12'),
-- Open retrospective for current cycle
(4, 'Cycle 4 Retrospective - Integrations', 'In progress - collecting feedback', 'OPEN', 4, 1, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL);

-- ===========================================
-- RETRO ITEMS
-- ===========================================
INSERT INTO retro_items (id, content, column_type, retrospective_id, author_id, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
-- Cycle 1 retro items
(1, 'Team communication was excellent', 'WENT_WELL', 1, 2, '2025-07-10', '2025-07-10'),
(2, 'Good documentation practices established', 'WENT_WELL', 1, 3, '2025-07-10', '2025-07-10'),
(3, 'Need better estimation skills', 'DID_NOT_GO_WELL', 1, 5, '2025-07-10', '2025-07-10'),
(4, 'Code review process was slow', 'DID_NOT_GO_WELL', 1, 2, '2025-07-10', '2025-07-10'),
(5, 'Set up automated code review reminders', 'ACTIONS', 1, 7, '2025-07-10', '2025-07-10'),

-- Cycle 2 retro items
(6, 'Sprint planning improved significantly', 'WENT_WELL', 2, 7, '2025-08-24', '2025-08-24'),
(7, 'Database migrations went smoothly', 'WENT_WELL', 2, 3, '2025-08-24', '2025-08-24'),
(8, 'Testing coverage could be better', 'DID_NOT_GO_WELL', 2, 6, '2025-08-24', '2025-08-24'),
(9, 'Need more pairing sessions', 'DID_NOT_GO_WELL', 2, 5, '2025-08-24', '2025-08-24'),
(10, 'Implement minimum test coverage requirements', 'ACTIONS', 2, 6, '2025-08-24', '2025-08-24'),
(11, 'Schedule weekly pairing sessions', 'ACTIONS', 2, 7, '2025-08-24', '2025-08-24'),

-- Cycle 3 retro items
(12, 'Quality focus resulted in fewer bugs', 'WENT_WELL', 3, 6, '2025-10-10', '2025-10-10'),
(13, 'User feedback integration improved', 'WENT_WELL', 3, 4, '2025-10-10', '2025-10-10'),
(14, 'Deployment pipeline needs work', 'DID_NOT_GO_WELL', 3, 8, '2025-10-10', '2025-10-10'),
(15, 'Automate deployment to staging', 'ACTIONS', 3, 8, '2025-10-10', '2025-10-10'),

-- Current cycle (Cycle 4) - open retro
(16, 'GitHub integration working well', 'WENT_WELL', 4, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(17, 'Slack notifications helpful for visibility', 'WENT_WELL', 4, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(18, 'Need better error handling patterns', 'DID_NOT_GO_WELL', 4, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- RETRO ITEM VOTES
-- ===========================================
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(3, 2, CURRENT_TIMESTAMP),
(3, 3, CURRENT_TIMESTAMP),
(3, 5, CURRENT_TIMESTAMP),
(4, 2, CURRENT_TIMESTAMP),
(4, 5, CURRENT_TIMESTAMP),
(8, 2, CURRENT_TIMESTAMP),
(8, 3, CURRENT_TIMESTAMP),
(8, 5, CURRENT_TIMESTAMP),
(8, 6, CURRENT_TIMESTAMP),
(16, 3, CURRENT_TIMESTAMP),
(16, 7, CURRENT_TIMESTAMP),
(18, 2, CURRENT_TIMESTAMP),
(18, 5, CURRENT_TIMESTAMP),
(18, 6, CURRENT_TIMESTAMP);

-- Reset sequences
SELECT setval('retrospectives_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM retrospectives), false);
SELECT setval('retro_items_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM retro_items), false);
