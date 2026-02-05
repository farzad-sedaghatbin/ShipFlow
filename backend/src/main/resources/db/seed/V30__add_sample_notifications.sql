-- Add sample dashboard notifications for demo/testing purposes

-- Sample overdue task notification
INSERT INTO dashboard_notifications (user_id, type, title, message, severity, action_url, entity_type, entity_id, is_read, created_at, expires_at)
VALUES 
(1, 'OVERDUE_TASK', 'Task Overdue: Fix Login Bug', 'Task ''Fix Login Bug'' is overdue by 3 days', 'WARNING', '/tasks/1', 'TASK', 1, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days');

-- Sample blocked task notification
INSERT INTO dashboard_notifications (user_id, type, title, message, severity, action_url, entity_type, entity_id, is_read, created_at, expires_at)
VALUES 
(1, 'BLOCKED_TASK', 'Task Blocked: API Integration', 'Task ''API Integration'' is blocked and needs attention', 'ERROR', '/tasks/2', 'TASK', 2, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days');

-- Sample cycle deadline notification
INSERT INTO dashboard_notifications (user_id, type, title, message, severity, action_url, entity_type, entity_id, is_read, created_at, expires_at)
VALUES 
(1, 'CYCLE_DEADLINE', 'Cycle Ending Soon: Sprint 5', 'Cycle ''Sprint 5'' ends in 5 days', 'WARNING', '/cycles/1', 'CYCLE', 1, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days');

-- Sample task assignment notification
INSERT INTO dashboard_notifications (user_id, type, title, message, severity, action_url, entity_type, entity_id, is_read, created_at, expires_at)
VALUES 
(1, 'TASK_ASSIGNED', 'New Task Assigned: Database Migration', 'You have been assigned to task ''Database Migration''', 'INFO', '/tasks/3', 'TASK', 3, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days');

-- Sample high priority task notification
INSERT INTO dashboard_notifications (user_id, type, title, message, severity, action_url, entity_type, entity_id, is_read, created_at, expires_at)
VALUES 
(1, 'TASK_PRIORITY_CHANGED', 'High Priority Task: Security Patch', 'Task ''Security Patch'' priority has been set to URGENT', 'WARNING', '/tasks/4', 'TASK', 4, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days');

-- Sample stalled hill chart notification
INSERT INTO dashboard_notifications (user_id, type, title, message, severity, action_url, entity_type, entity_id, is_read, created_at, expires_at)
VALUES 
(1, 'STALLED_HILL_CHART', 'Scope Not Moving: User Authentication', 'Hill chart scope ''User Authentication'' hasn''t been updated in 14 days', 'WARNING', '/pitches/1/hill-chart', 'PITCH', 1, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days');

-- Sample task status change notification
INSERT INTO dashboard_notifications (user_id, type, title, message, severity, action_url, entity_type, entity_id, is_read, created_at, expires_at)
VALUES 
(1, 'TASK_STATUS_CHANGED', 'Task Status Changed: Payment Gateway', 'Task ''Payment Gateway'' is now BLOCKED', 'WARNING', '/tasks/5', 'TASK', 5, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days');

-- Sample read notification (older)
INSERT INTO dashboard_notifications (user_id, type, title, message, severity, action_url, entity_type, entity_id, is_read, read_at, created_at, expires_at)
VALUES 
(1, 'TASK_ASSIGNED', 'Task Completed: Email Templates', 'Task ''Email Templates'' has been marked as done', 'INFO', '/tasks/6', 'TASK', 6, true, CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '29 days');

-- Sample notifications for user 2 (if exists)
INSERT INTO dashboard_notifications (user_id, type, title, message, severity, action_url, entity_type, entity_id, is_read, created_at, expires_at)
SELECT 2, 'OVERDUE_TASK', 'Task Overdue: Update Documentation', 'Task ''Update Documentation'' is overdue by 2 days', 'WARNING', '/tasks/7', 'TASK', 7, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days'
WHERE EXISTS (SELECT 1 FROM users WHERE id = 2);

INSERT INTO dashboard_notifications (user_id, type, title, message, severity, action_url, entity_type, entity_id, is_read, created_at, expires_at)
SELECT 2, 'TASK_ASSIGNED', 'New Task Assigned: Code Review', 'You have been assigned to task ''Code Review''', 'INFO', '/tasks/8', 'TASK', 8, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days'
WHERE EXISTS (SELECT 1 FROM users WHERE id = 2);
