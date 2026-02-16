-- ============================================================================
-- V1002: Tasks seed data
-- Status: BACKLOG, TODO, IN_PROGRESS, BLOCKED, IN_REVIEW, DONE, CANCELLED
-- Priority: LOW, MEDIUM, HIGH, CRITICAL
-- Category: PITCH_SCOPE, DEBT_IMPROVEMENT
-- ============================================================================

-- ===========================================
-- TASKS for Cycle 4 (active cycle)
-- ===========================================
INSERT INTO tasks (id, title, description, status, priority, estimate_hours, cycle_id, assignee_id, created_by_id, due_date, category, tags, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
-- GitHub Integration tasks
(1, 'Setup OAuth App', 'Create OAuth application in GitHub settings', 'DONE', 'HIGH', 4, 4, 2, 7, '2025-10-20', 'PITCH_SCOPE', 'github,oauth', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Implement OAuth Flow', 'Handle OAuth callback and token storage', 'DONE', 'HIGH', 8, 4, 2, 7, '2025-10-25', 'PITCH_SCOPE', 'github,oauth', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Create Webhook Endpoint', 'Build webhook receiver for GitHub events', 'IN_PROGRESS', 'HIGH', 6, 4, 3, 7, '2025-11-01', 'PITCH_SCOPE', 'github,webhook', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Parse Webhook Events', 'Handle PR, issue, and commit events', 'TODO', 'MEDIUM', 8, 4, 3, 7, '2025-11-05', 'PITCH_SCOPE', 'github,webhook', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'Build Issue Sync Service', 'Two-way issue synchronization', 'BACKLOG', 'MEDIUM', 12, 4, NULL, 7, '2025-11-15', 'PITCH_SCOPE', 'github,sync', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Slack Notifications tasks
(6, 'Design Message Templates', 'Create Slack message block templates', 'DONE', 'MEDIUM', 4, 4, 4, 7, '2025-10-22', 'PITCH_SCOPE', 'slack,design', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'Slack API Integration', 'Integrate with Slack Web API', 'IN_REVIEW', 'HIGH', 6, 4, 2, 7, '2025-10-28', 'PITCH_SCOPE', 'slack,api', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'Channel Configuration UI', 'UI for configuring notification channels', 'IN_PROGRESS', 'MEDIUM', 8, 4, 2, 7, '2025-11-05', 'PITCH_SCOPE', 'slack,frontend', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Dashboard Widget tasks
(9, 'Widget Drag-Drop System', 'Implement drag-drop framework', 'IN_PROGRESS', 'HIGH', 12, 4, 5, 7, '2025-11-01', 'PITCH_SCOPE', 'dashboard,dnd', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'Chart.js Integration', 'Add Chart.js for visualizations', 'TODO', 'MEDIUM', 6, 4, 5, 7, '2025-11-08', 'PITCH_SCOPE', 'dashboard,charts', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 'Widget Save/Load', 'Persist widget layouts to database', 'BACKLOG', 'HIGH', 8, 4, NULL, 7, '2025-11-12', 'PITCH_SCOPE', 'dashboard,persistence', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Tech debt tasks
(12, 'Refactor Auth Service', 'Clean up authentication code', 'TODO', 'LOW', 6, 4, 3, 3, NULL, 'DEBT_IMPROVEMENT', 'tech-debt,refactor', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(13, 'Add API Rate Limiting', 'Implement rate limiting middleware', 'BACKLOG', 'MEDIUM', 8, 4, NULL, 8, NULL, 'DEBT_IMPROVEMENT', 'tech-debt,security', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(14, 'Database Index Optimization', 'Add missing indexes for slow queries', 'DONE', 'HIGH', 4, 4, 8, 8, '2025-10-25', 'DEBT_IMPROVEMENT', 'tech-debt,performance', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Bug fix tasks
(15, 'Fix Login Redirect Loop', 'Users sometimes stuck in redirect loop', 'DONE', 'CRITICAL', 3, 4, 2, 6, '2025-10-18', 'DEBT_IMPROVEMENT', 'bug-fix,auth', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(16, 'Fix Chart Tooltip Overlap', 'Tooltips overlapping on small screens', 'IN_PROGRESS', 'LOW', 2, 4, 5, 6, '2025-11-10', 'DEBT_IMPROVEMENT', 'bug-fix,ui', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- TASKS for Cycle 5 (shaping phase - planning)
-- ===========================================
INSERT INTO tasks (id, title, description, status, priority, estimate_hours, cycle_id, assignee_id, created_by_id, due_date, category, tags, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(17, 'Research Offline Storage', 'Evaluate IndexedDB vs SQLite for mobile', 'IN_PROGRESS', 'MEDIUM', 8, 5, 5, 7, '2025-11-20', 'PITCH_SCOPE', 'research,mobile', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(18, 'Push Notification POC', 'Prototype push notifications with FCM', 'TODO', 'MEDIUM', 6, 5, 3, 7, '2025-11-25', 'PITCH_SCOPE', 'research,mobile', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(19, 'Design Mobile Auth Flow', 'UX design for biometric login', 'BACKLOG', 'LOW', 4, 5, 4, 7, '2025-12-01', 'PITCH_SCOPE', 'design,mobile', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- TASK DEPENDENCIES
-- dependency_type: BLOCKS, DEPENDS_ON, RELATED_TO
-- ===========================================
INSERT INTO task_dependencies (source_task_id, target_task_id, dependency_type, created_at) VALUES
(4, 3, 'DEPENDS_ON', CURRENT_TIMESTAMP),  -- Parse events depends on webhook endpoint
(5, 4, 'DEPENDS_ON', CURRENT_TIMESTAMP),  -- Issue sync depends on event parsing
(8, 7, 'DEPENDS_ON', CURRENT_TIMESTAMP),  -- Channel UI depends on Slack API
(11, 9, 'DEPENDS_ON', CURRENT_TIMESTAMP); -- Widget save depends on drag-drop

-- Reset sequences
SELECT setval('tasks_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM tasks), false);
