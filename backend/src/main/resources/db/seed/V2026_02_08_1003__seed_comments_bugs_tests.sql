-- =============================================================================
-- V2026_02_08_1003: Seed Comments, Bug Reports, and Test Cases
-- =============================================================================
-- This seed data demonstrates:
-- 1. Comments on tasks with @mentions
-- 2. Comment reactions (thumbs up, heart, etc.)
-- 3. Bug reports with various severities and statuses
-- 4. Test cases with different types and priorities
-- 5. Test runs with pass/fail results
-- =============================================================================

-- =============================================================================
-- COMMENTS on Tasks (referencing tasks from V21 and V25)
-- Note: Tasks from V21 start at ~ID 1-13 depending on existing data
-- We'll use a subquery approach to be safe
-- =============================================================================

-- Comments on task "Refactor authentication middleware" (assumed task ID around 1-5 range from V21)
-- Using direct ID 1 assuming it's the first task created
INSERT INTO comments (content, entity_type, entity_id, author_id, is_edited, created_at, updated_at)
VALUES
-- Thread 1 - discussion about the refactoring approach
('I started looking at the auth middleware. The current implementation has a lot of nested callbacks. I think we should convert to async/await for better readability. @bob what do you think?', 
 'TASK', 1, 2, false, '2026-01-07 09:15:00', NULL),

('Good call @alice! Async/await will definitely clean this up. I also noticed we''re not properly handling token refresh edge cases. Let me document those scenarios.', 
 'TASK', 1, 3, false, '2026-01-07 09:45:00', NULL),

('Found 3 edge cases we need to handle:
1. Token expired during long-running request
2. Concurrent refresh token requests
3. Clock skew between client and server

I''ll create subtasks for each.', 
 'TASK', 1, 3, false, '2026-01-07 10:30:00', NULL),

('@emma can you add test cases for these edge cases once Bob finishes the implementation?', 
 'TASK', 1, 2, false, '2026-01-07 11:00:00', NULL),

('Sure! I''ll add them to the test plan. Should have comprehensive coverage for all auth flows.', 
 'TASK', 1, 6, false, '2026-01-07 11:15:00', NULL),

-- Comments on task "Fix notification sound on Safari" (task ID 4 from V21)
('This is a known Safari limitation - AudioContext requires user interaction before playing sounds. We need to initialize audio on first user click.', 
 'TASK', 4, 2, false, '2026-01-08 14:00:00', NULL),

('I found a workaround using the Web Audio API. Testing now on different Safari versions.', 
 'TASK', 4, 2, true, '2026-01-08 16:30:00', '2026-01-08 16:45:00'),

('@david FYI this might affect the mobile collaboration features too. Can you check if we have similar audio usage there?', 
 'TASK', 4, 2, false, '2026-01-08 17:00:00', NULL),

('Checked - we don''t use audio in the collab hub, but good to know for future reference. Thanks for the heads up!', 
 'TASK', 4, 5, false, '2026-01-09 09:30:00', NULL),

-- Comments on task "Handle WebSocket reconnection" (task ID 5 from V21)
('Implementing exponential backoff with jitter for reconnection. Starting at 1s, max 30s, with random jitter ±20%.', 
 'TASK', 5, 3, false, '2026-01-10 10:00:00', NULL),

('Good approach! Don''t forget to emit reconnection status events so the UI can show a "Reconnecting..." indicator.', 
 'TASK', 5, 2, false, '2026-01-10 10:30:00', NULL),

('Added reconnection status events. Also added a max retry limit of 10 before giving up and showing an error to the user.', 
 'TASK', 5, 3, false, '2026-01-10 15:00:00', NULL);

-- =============================================================================
-- COMMENT REACTIONS
-- =============================================================================

-- Reactions on the first comment (async/await suggestion)
INSERT INTO comment_reactions (comment_id, user_id, reaction_type, created_at)
VALUES
(1, 3, 'THUMBS_UP', '2026-01-07 09:20:00'),
(1, 5, 'THUMBS_UP', '2026-01-07 09:25:00'),
(1, 6, 'ROCKET', '2026-01-07 09:30:00');

-- Reactions on the edge cases documentation
INSERT INTO comment_reactions (comment_id, user_id, reaction_type, created_at)
VALUES
(3, 2, 'THUMBS_UP', '2026-01-07 10:35:00'),
(3, 4, 'EYES', '2026-01-07 10:40:00'),
(3, 6, 'THUMBS_UP', '2026-01-07 10:45:00');

-- Reactions on Emma's test plan confirmation
INSERT INTO comment_reactions (comment_id, user_id, reaction_type, created_at)
VALUES
(5, 2, 'HEART', '2026-01-07 11:20:00'),
(5, 3, 'THUMBS_UP', '2026-01-07 11:25:00');

-- Reactions on Safari workaround
INSERT INTO comment_reactions (comment_id, user_id, reaction_type, created_at)
VALUES
(7, 3, 'ROCKET', '2026-01-08 16:50:00'),
(7, 6, 'THUMBS_UP', '2026-01-08 17:00:00');

-- Reactions on WebSocket reconnection approach
INSERT INTO comment_reactions (comment_id, user_id, reaction_type, created_at)
VALUES
(10, 2, 'THUMBS_UP', '2026-01-10 10:05:00'),
(10, 6, 'EYES', '2026-01-10 10:10:00');

-- =============================================================================
-- BUG REPORTS
-- =============================================================================

INSERT INTO bug_reports (bug_key, title, description, steps_to_reproduce, expected_behavior, actual_behavior, 
    environment, pitch_id, cycle_id, team_id, test_run_id, severity, status, tags, 
    reporter_id, assignee_id, resolution, resolved_at, created_at, updated_at)
VALUES
-- Critical bugs
('BUG-001', 'Email notifications not delivered to Gmail users', 
 'Users with Gmail accounts are not receiving email notifications. Started happening after the SendGrid migration.',
 '1. Create account with Gmail address
2. Enable email notifications in settings
3. Trigger a notification event (e.g., new comment)
4. Check Gmail inbox and spam folder',
 'User should receive email notification within 1-2 minutes',
 'No email received even after 30 minutes. Not in spam folder.',
 'Production environment, SendGrid API v3',
 15, 6, 6, NULL, 'CRITICAL', 'RESOLVED', 'email,production,sendgrid',
 6, 3, 'SPF record was missing for our domain. Added SPF record and verified DKIM signing. Delivery now working.', 
 '2025-11-09 14:00:00', '2025-11-08 10:00:00', '2025-11-09 14:00:00'),

('BUG-002', 'Dashboard widgets freeze when resizing browser window', 
 'Dashboard becomes unresponsive when rapidly resizing the browser window. Requires page refresh to recover.',
 '1. Open dashboard with multiple widgets
2. Rapidly resize browser window multiple times
3. Observe dashboard becoming unresponsive',
 'Dashboard should remain responsive during window resize',
 'Dashboard freezes, CPU usage spikes to 100%. Console shows "Maximum call stack size exceeded"',
 'Chrome 120, Firefox 121, Edge 120 - all affected',
 16, 6, 6, NULL, 'MAJOR', 'RESOLVED', 'dashboard,performance,frontend',
 2, 2, 'Added debounce to resize handler (250ms delay). Fixed recursive re-render issue in grid layout component.', 
 '2025-11-02 16:00:00', '2025-10-30 09:00:00', '2025-11-02 16:00:00'),

-- High severity bugs
('BUG-003', 'Rate limiter allows bursts exceeding configured limit', 
 'Under high load, rate limiter occasionally allows more requests than configured limit before blocking.',
 '1. Configure rate limit to 100 requests/minute
2. Send 150 concurrent requests
3. Observe how many get through',
 '100 requests should succeed, 50 should return 429 Too Many Requests',
 'Sometimes 110-120 requests succeed before limiting kicks in',
 'Staging environment, Redis 7.0, 3-node cluster',
 17, 6, 7, NULL, 'MAJOR', 'RESOLVED', 'rate-limiting,redis,backend',
 6, 3, 'Race condition in distributed counter. Switched to Redis INCRBY with Lua script for atomic operations.', 
 '2025-10-22 11:00:00', '2025-10-20 14:00:00', '2025-10-22 11:00:00'),

('BUG-004', 'Audit log search returns incorrect results with special characters', 
 'Searching audit logs with special characters like quotes or ampersands returns no results or incorrect results.',
 '1. Create audit event with message containing "User & Admin"
2. Search for "User & Admin"
3. Observe search results',
 'Should find the audit event with exact match',
 'Returns 0 results. Searching "User" alone works.',
 'Elasticsearch 8.10',
 18, 6, 7, NULL, 'LOW', 'RESOLVED', 'audit,search,elasticsearch',
 6, 5, 'Added query string escaping for special Elasticsearch characters. Updated search analyzer settings.', 
 '2025-11-06 10:00:00', '2025-11-03 11:00:00', '2025-11-06 10:00:00'),

-- Medium severity bugs (some still open)
('BUG-005', 'Notification preferences not persisting after browser refresh', 
 'When user changes notification preferences and refreshes the page, some settings revert to default.',
 '1. Go to notification preferences
2. Disable "Push notifications for comments"
3. Refresh the page
4. Check the setting again',
 'Setting should remain disabled',
 'Setting shows as enabled again after refresh',
 'Chrome 120, Safari 17',
 15, 6, 6, NULL, 'LOW', 'IN_PROGRESS', 'notifications,settings,frontend',
 3, 2, NULL, 
 NULL, '2025-11-12 09:00:00', '2025-11-12 09:00:00'),

('BUG-006', 'Mobile dashboard layout breaks on iPad in portrait mode', 
 'Dashboard widgets overlap and some are cut off when viewing on iPad in portrait orientation.',
 '1. Open dashboard on iPad (any model)
2. Rotate to portrait mode
3. Observe layout issues',
 'Widgets should stack vertically in single column for narrow viewports',
 'Widgets overlap and some content is cut off on the right side',
 'iPad Pro 11", iPadOS 17, Safari',
 16, 6, 6, NULL, 'LOW', 'OPEN', 'dashboard,mobile,responsive',
 4, 2, NULL, 
 NULL, '2025-11-15 14:00:00', '2025-11-15 14:00:00'),

-- Low severity bugs
('BUG-007', 'Typo in rate limit exceeded error message', 
 'Error message says "Rate limit exceed" instead of "Rate limit exceeded".',
 '1. Trigger rate limit by rapid API calls
2. Read the error message',
 'Message should say "Rate limit exceeded"',
 'Message says "Rate limit exceed"',
 'All environments',
 17, 6, 7, NULL, 'LOW', 'RESOLVED', 'rate-limiting,typo,ux',
 6, 3, 'Fixed typo in error message constant.', 
 '2025-10-21 09:00:00', '2025-10-21 08:00:00', '2025-10-21 09:00:00'),

('BUG-008', 'Audit log timestamps show in UTC instead of user timezone', 
 'All timestamps in audit log viewer are displayed in UTC, making it hard to correlate with local events.',
 '1. Perform an action (creates audit log)
2. View audit log
3. Compare timestamp',
 'Timestamps should display in user''s local timezone',
 'Timestamps display in UTC regardless of user timezone setting',
 'All browsers',
 18, 6, 7, NULL, 'LOW', 'OPEN', 'audit,ux,timezone',
 2, 5, NULL, 
 NULL, '2025-11-14 16:00:00', '2025-11-14 16:00:00'),

-- Bugs for current cycle (Cycle 5)
('BUG-009', 'Search autocomplete shows results after search submitted', 
 'After pressing Enter to search, the autocomplete dropdown still shows for a moment.',
 '1. Type search query
2. Wait for autocomplete to appear
3. Press Enter to search
4. Observe autocomplete still visible',
 'Autocomplete should close immediately when search is submitted',
 'Autocomplete remains visible for 200-300ms after search',
 'Chrome, Firefox',
 12, 5, 4, NULL, 'LOW', 'IN_PROGRESS', 'search,frontend,ux',
 2, 2, NULL, 
 NULL, '2026-01-11 10:00:00', '2026-01-11 10:00:00'),

('BUG-010', 'WebSocket connection drops when switching browser tabs', 
 'WebSocket connection occasionally drops when user switches to another browser tab for extended period.',
 '1. Open app with notifications enabled
2. Switch to another browser tab
3. Wait 5+ minutes
4. Return to app tab',
 'WebSocket should maintain connection or reconnect seamlessly',
 'Connection dropped, no automatic reconnection. User sees stale data.',
 'Chrome 120 on macOS, Windows',
 11, 5, 4, NULL, 'MAJOR', 'OPEN', 'websocket,reliability,backend',
 6, 3, NULL, 
 NULL, '2026-01-09 15:00:00', '2026-01-09 15:00:00');

-- Update comment counts for tasks that have comments
UPDATE tasks SET comment_count = 5 WHERE id = 1;
UPDATE tasks SET comment_count = 4 WHERE id = 4;
UPDATE tasks SET comment_count = 3 WHERE id = 5;

-- =============================================================================
-- TEST CASES
-- =============================================================================

INSERT INTO test_cases (test_case_key, title, description, preconditions, steps, expected_result,
    pitch_id, cycle_id, team_id, type, priority, status, ai_generated, tags,
    estimated_minutes, created_by_id, updated_by_id, created_at, updated_at)
VALUES
-- Email Notification Test Cases (Pitch 15)
('TC-001', 'Verify email delivery to standard email providers',
 'Ensure email notifications are delivered to major email providers (Gmail, Outlook, Yahoo)',
 'User account exists with verified email. Notification preferences enabled.',
 '1. Login as test user with Gmail address
2. Have another user mention the test user in a comment
3. Wait for notification email
4. Verify email content and formatting',
 'Email arrives within 2 minutes. Subject line correct. Body contains mention context. Links work.',
 15, 6, 6, 'FUNCTIONAL', 'HIGH', 'APPROVED', false, 'email,notification,integration',
 15, 6, NULL, '2025-10-10 10:00:00', '2025-11-08 15:00:00'),

('TC-002', 'Verify email template rendering for different notification types',
 'Each notification type should render with appropriate template',
 'Email service configured. Multiple notification types available.',
 '1. Trigger each notification type: comment_mention, task_assigned, pitch_status_change
2. Verify each email uses correct template
3. Check branding and formatting consistency',
 'Each notification type uses correct template. Consistent branding. All dynamic content renders.',
 15, 6, 6, 'FUNCTIONAL', 'LOW', 'APPROVED', false, 'email,template,notification',
 20, 6, NULL, '2025-10-10 10:30:00', '2025-11-08 15:30:00'),

('TC-003', 'Verify email preference toggles work correctly',
 'Users should be able to enable/disable specific email notification types',
 'User account with email preferences accessible.',
 '1. Navigate to notification preferences
2. Disable "comment mention" notifications
3. Have another user mention you
4. Verify no email received
5. Re-enable and verify email is received',
 'Disabled notifications do not send emails. Enabled notifications send as expected.',
 15, 6, 6, 'FUNCTIONAL', 'HIGH', 'APPROVED', false, 'email,preferences,notification',
 25, 6, NULL, '2025-10-10 11:00:00', '2025-11-08 16:00:00'),

-- Dashboard Test Cases (Pitch 16)
('TC-004', 'Verify dashboard responsive layout on different screen sizes',
 'Dashboard should be usable on desktop, tablet, and mobile screen sizes',
 'Dashboard with multiple widgets configured.',
 '1. Open dashboard on desktop (1920x1080)
2. Resize to tablet (768x1024)
3. Resize to mobile (375x667)
4. Verify layout adapts appropriately at each size',
 'Desktop: Multi-column grid. Tablet: 2-column layout. Mobile: Single column, scrollable.',
 16, 6, 6, 'USABILITY', 'HIGH', 'APPROVED', false, 'dashboard,responsive,ui',
 30, 6, NULL, '2025-10-12 09:00:00', '2025-11-05 14:00:00'),

('TC-005', 'Verify dashboard widget data refresh',
 'Widgets should refresh data at configured intervals',
 'Dashboard with widgets that have auto-refresh enabled.',
 '1. Configure widget with 30-second refresh
2. Modify underlying data
3. Wait for refresh interval
4. Verify widget shows updated data',
 'Widget data updates automatically without page refresh.',
 16, 6, 6, 'FUNCTIONAL', 'LOW', 'APPROVED', false, 'dashboard,refresh,widget',
 15, 6, NULL, '2025-10-12 10:00:00', '2025-11-05 15:00:00'),

('TC-006', 'Dashboard accessibility - keyboard navigation',
 'Verify dashboard is fully navigable using keyboard only',
 'Dashboard loaded with multiple widgets.',
 '1. Tab through all interactive elements
2. Verify focus indicators visible
3. Activate widgets using Enter/Space
4. Navigate widget contents with arrow keys',
 'All elements reachable by keyboard. Focus indicators clear. WCAG 2.1 AA compliant.',
 16, 6, 6, 'ACCESSIBILITY', 'HIGH', 'APPROVED', false, 'dashboard,accessibility,keyboard',
 20, 6, NULL, '2025-10-12 11:00:00', '2025-11-05 16:00:00'),

-- Rate Limiting Test Cases (Pitch 17)
('TC-007', 'Verify rate limit enforcement at configured threshold',
 'Requests exceeding rate limit should receive 429 status',
 'Rate limit configured to 100 requests/minute for endpoint.',
 '1. Send 100 requests within 60 seconds
2. Verify all succeed with 200 status
3. Send 101st request
4. Verify 429 status returned',
 'First 100 requests succeed. 101st and beyond get 429 with Retry-After header.',
 17, 6, 7, 'FUNCTIONAL', 'CRITICAL', 'APPROVED', false, 'rate-limit,api,security',
 20, 6, NULL, '2025-10-08 09:00:00', '2025-10-20 11:00:00'),

('TC-008', 'Verify rate limit reset after window expires',
 'Rate limit counter should reset after the configured time window',
 'Rate limit configured to 100 requests/minute.',
 '1. Exhaust rate limit (send 100 requests)
2. Wait 60 seconds
3. Send new request
4. Verify request succeeds',
 'After window expires, new requests are allowed. Counter resets properly.',
 17, 6, 7, 'FUNCTIONAL', 'HIGH', 'APPROVED', false, 'rate-limit,api,timing',
 15, 6, NULL, '2025-10-08 10:00:00', '2025-10-20 12:00:00'),

('TC-009', 'Verify distributed rate limiting across multiple nodes',
 'Rate limit should be enforced consistently across clustered application nodes',
 'Multi-node deployment with shared Redis cache.',
 '1. Send requests alternating between nodes
2. Total requests should still honor global limit
3. Verify 429 after combined limit exceeded',
 'Global rate limit enforced regardless of which node handles request.',
 17, 6, 7, 'INTEGRATION', 'HIGH', 'APPROVED', false, 'rate-limit,distributed,redis',
 30, 6, NULL, '2025-10-08 11:00:00', '2025-10-20 14:00:00'),

-- Audit Logging Test Cases (Pitch 18)
('TC-010', 'Verify audit log captures all required user actions',
 'User CRUD operations should be logged with full context',
 'Audit logging enabled. Test user account.',
 '1. Create new item
2. Update the item
3. Delete the item
4. Search audit logs for each action',
 'All three actions logged with: timestamp, user ID, action type, entity type, entity ID, payload.',
 18, 6, 7, 'FUNCTIONAL', 'HIGH', 'APPROVED', false, 'audit,logging,compliance',
 25, 6, NULL, '2025-10-15 09:00:00', '2025-11-10 10:00:00'),

('TC-011', 'Verify audit log search with complex filters',
 'Users should be able to search and filter audit logs effectively',
 'Audit logs exist for various actions, users, and time periods.',
 '1. Search by date range
2. Filter by user
3. Filter by action type
4. Combine multiple filters
5. Verify results match criteria',
 'Search returns only matching results. Filters can be combined. Performance acceptable (<2s).',
 18, 6, 7, 'FUNCTIONAL', 'LOW', 'APPROVED', false, 'audit,search,filter',
 20, 6, NULL, '2025-10-15 10:00:00', '2025-11-10 11:00:00'),

('TC-012', 'Verify audit log export in CSV format',
 'Users should be able to export filtered audit logs to CSV',
 'Audit logs exist. User has export permission.',
 '1. Filter audit logs for last 7 days
2. Click export to CSV
3. Download file
4. Open in spreadsheet application',
 'CSV file downloads successfully. All filtered records included. Proper column headers. UTF-8 encoding.',
 18, 6, 7, 'FUNCTIONAL', 'LOW', 'APPROVED', false, 'audit,export,csv',
 15, 6, NULL, '2025-10-15 11:00:00', '2025-11-10 12:00:00'),

-- Current cycle (Cycle 5) test cases - some not yet run
('TC-013', 'Verify real-time notification delivery via WebSocket',
 'Notifications should appear instantly without page refresh',
 'WebSocket connection established. User logged in.',
 '1. Open app in two browser windows (User A and User B)
2. User A mentions User B
3. Observe notification in User B window',
 'Notification appears within 1 second. Toast animation plays. Badge count increases.',
 11, 5, 4, 'FUNCTIONAL', 'HIGH', 'NOT_RUN', false, 'websocket,notification,realtime',
 20, 6, NULL, '2026-01-06 09:00:00', '2026-01-06 09:00:00'),

('TC-014', 'Verify search autocomplete performance',
 'Autocomplete suggestions should appear quickly with minimal latency',
 'Search index populated. User logged in.',
 '1. Start typing in search box
2. Measure time until suggestions appear
3. Test with common and rare search terms',
 'Suggestions appear within 200ms. At least 5 relevant suggestions. Keyboard navigation works.',
 12, 5, 4, 'PERFORMANCE', 'HIGH', 'IN_PROGRESS', false, 'search,autocomplete,performance',
 25, 6, NULL, '2026-01-08 09:00:00', '2026-01-10 14:00:00'),

('TC-015', 'AI-generated: Export data integrity verification',
 'Verify exported data matches source data exactly',
 'Data exists in all supported export formats.',
 '1. Export same dataset as CSV, JSON, and PDF
2. Compare record counts
3. Spot-check 10 random records for data accuracy
4. Verify special characters preserved',
 'Record counts match across formats. All sampled records accurate. Unicode characters preserved.',
 13, 5, 5, 'FUNCTIONAL', 'HIGH', 'NOT_RUN', true, 'export,data-integrity,ai-generated',
 30, 6, NULL, '2026-01-07 10:00:00', '2026-01-07 10:00:00');

-- =============================================================================
-- TEST RUNS
-- =============================================================================

INSERT INTO test_runs (test_case_id, cycle_id, pitch_id, status, executed_by_id, executed_at,
    duration_seconds, notes, actual_result, build_version, environment, created_at)
VALUES
-- Email notification test runs
(1, 6, 15, 'APPROVED', 6, '2025-11-08 15:00:00', 420,
 'All major email providers tested: Gmail, Outlook, Yahoo, iCloud. All deliveries successful.',
 'Emails delivered to all providers. Average delivery time: 45 seconds.',
 'v1.8.0-beta.3', 'staging', '2025-11-08 15:00:00'),

(2, 6, 15, 'APPROVED', 6, '2025-11-08 15:30:00', 600,
 'Tested comment_mention, task_assigned, pitch_status_change, and cycle_started templates.',
 'All templates render correctly. Consistent branding across all.',
 'v1.8.0-beta.3', 'staging', '2025-11-08 15:30:00'),

(3, 6, 15, 'APPROVED', 6, '2025-11-08 16:00:00', 480,
 'Toggle tests for all notification types.',
 'All toggles work as expected. Changes persist across sessions.',
 'v1.8.0-beta.3', 'staging', '2025-11-08 16:00:00'),

-- Dashboard test runs
(4, 6, 16, 'APPROVED', 6, '2025-11-05 14:00:00', 900,
 'Tested on various screen sizes using Chrome DevTools and physical devices.',
 'Layout adapts correctly at all breakpoints. Minor spacing issue on iPad Pro noted and fixed.',
 'v1.8.0-beta.2', 'staging', '2025-11-05 14:00:00'),

(5, 6, 16, 'APPROVED', 6, '2025-11-05 15:00:00', 360,
 'Tested with 30s and 60s refresh intervals.',
 'Data refreshes correctly. No memory leaks detected during 30-minute observation.',
 'v1.8.0-beta.2', 'staging', '2025-11-05 15:00:00'),

(6, 6, 16, 'APPROVED', 2, '2025-11-05 16:00:00', 720,
 'Performed accessibility audit with keyboard-only navigation.',
 'All elements focusable. Skip links work. Focus trap in modals. No a11y violations in axe-core.',
 'v1.8.0-beta.2', 'staging', '2025-11-05 16:00:00'),

-- Rate limiting test runs
(7, 6, 17, 'PASSED', 6, '2025-10-20 11:00:00', 300,
 'Automated script sent exactly 101 requests to test boundary.',
 'Requests 1-100: 200 OK. Request 101: 429 with Retry-After: 28 header.',
 'v1.7.5', 'staging', '2025-10-20 11:00:00'),

(8, 6, 17, 'PASSED', 6, '2025-10-20 12:00:00', 180,
 'Waited full minute window before retry.',
 'Request after window reset: 200 OK. Counter properly reset.',
 'v1.7.5', 'staging', '2025-10-20 12:00:00'),

(9, 6, 17, 'PASSED', 3, '2025-10-20 14:00:00', 1200,
 'Tested with requests distributed across 3 staging nodes.',
 'Global limit enforced correctly. No requests slipped through cluster boundary.',
 'v1.7.5', 'staging-cluster', '2025-10-20 14:00:00'),

-- Audit logging test runs
(10, 6, 18, 'PASSED', 6, '2025-11-10 10:00:00', 600,
 'Created, updated, and deleted test records. Verified audit trail.',
 'All actions logged with complete metadata. Payload recorded as JSON.',
 'v1.8.0-beta.4', 'staging', '2025-11-10 10:00:00'),

(11, 6, 18, 'PASSED', 6, '2025-11-10 11:00:00', 480,
 'Tested search with various filter combinations.',
 'All filter combinations work. Search response time: 180ms average.',
 'v1.8.0-beta.4', 'staging', '2025-11-10 11:00:00'),

(12, 6, 18, 'PASSED', 6, '2025-11-10 12:00:00', 300,
 'Exported 1000 records to CSV.',
 'Export completed in 8 seconds. File valid. Special characters properly escaped.',
 'v1.8.0-beta.4', 'staging', '2025-11-10 12:00:00'),

-- Current cycle test runs (Cycle 5) - one in progress
(14, 5, 12, 'IN_PROGRESS', 6, '2026-01-10 14:00:00', NULL,
 'Currently testing autocomplete with various query lengths.',
 NULL,
 'v1.9.0-alpha.1', 'dev', '2026-01-10 14:00:00');

-- =============================================================================
-- Reset sequences
-- =============================================================================
SELECT setval(pg_get_serial_sequence('comments', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('comment_reactions', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('bug_reports', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('test_cases', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('test_runs', 'id'), 100, false);
