-- ============================================================================
-- V1003: QA Test Management seed data
-- BugSeverity: TRIVIAL, MINOR, MAJOR, CRITICAL, BLOCKER
-- TestCasePriority: LOW, MEDIUM, HIGH, CRITICAL
-- ============================================================================

-- ===========================================
-- TEST CASES
-- Type: UNIT, INTEGRATION, E2E, SMOKE, REGRESSION, PERFORMANCE, SECURITY
-- Status: DRAFT, ACTIVE, DEPRECATED
-- ===========================================
INSERT INTO test_cases (id, test_case_key, title, description, preconditions, steps, expected_result, pitch_id, cycle_id, team_id, type, priority, status, tags, estimated_minutes, created_by_id, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
-- Authentication test cases
(1, 'TC-001', 'User Login Success', 'Verify user can login with valid credentials', 'User account exists', '1. Navigate to login\n2. Enter valid email\n3. Enter valid password\n4. Click login', 'User is logged in and redirected to dashboard', 14, 1, NULL, 'E2E', 'CRITICAL', 'ACTIVE', 'auth,login', 5, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'TC-002', 'User Login Invalid Password', 'Verify error for invalid password', 'User account exists', '1. Navigate to login\n2. Enter valid email\n3. Enter wrong password\n4. Click login', 'Error message displayed, user not logged in', 14, 1, NULL, 'E2E', 'HIGH', 'ACTIVE', 'auth,login,negative', 3, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'TC-003', 'Password Reset Flow', 'Verify password reset email flow', 'User account exists', '1. Click forgot password\n2. Enter email\n3. Check email\n4. Click reset link\n5. Enter new password', 'Password is changed, user can login', 14, 1, NULL, 'E2E', 'HIGH', 'ACTIVE', 'auth,password-reset', 10, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- GitHub Integration test cases
(4, 'TC-004', 'GitHub OAuth Connect', 'Verify GitHub OAuth connection flow', 'GitHub OAuth app configured', '1. Go to integrations\n2. Click Connect GitHub\n3. Authorize on GitHub\n4. Verify redirect back', 'GitHub account is connected, token stored', 1, 4, 1, 'INTEGRATION', 'CRITICAL', 'ACTIVE', 'github,oauth', 8, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'TC-005', 'GitHub Webhook Receive', 'Verify webhook events are received', 'GitHub integration connected', '1. Trigger PR event on GitHub\n2. Check webhook logs', 'Webhook event is logged and processed', 1, 4, 1, 'INTEGRATION', 'HIGH', 'ACTIVE', 'github,webhook', 5, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'TC-006', 'GitHub Issue Sync', 'Verify issue sync from GitHub', 'Integration connected, issues exist', '1. Create issue on GitHub\n2. Wait for sync\n3. Verify issue in ShipFlow', 'Issue appears in ShipFlow with correct data', 1, 4, 1, 'INTEGRATION', 'MEDIUM', 'DRAFT', 'github,sync', 10, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Slack Notification test cases
(7, 'TC-007', 'Slack Channel Config', 'Verify Slack channel configuration', 'Slack app installed', '1. Go to Slack settings\n2. Select channel\n3. Save configuration', 'Channel configuration is saved', 2, 4, 1, 'E2E', 'HIGH', 'ACTIVE', 'slack,config', 5, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'TC-008', 'Slack Notification Send', 'Verify notifications sent to Slack', 'Channel configured', '1. Trigger notification event\n2. Check Slack channel', 'Message appears in Slack channel', 2, 4, 1, 'INTEGRATION', 'CRITICAL', 'ACTIVE', 'slack,notification', 5, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Dashboard test cases
(9, 'TC-009', 'Widget Drag and Drop', 'Verify widgets can be repositioned', 'Dashboard with widgets exists', '1. Open dashboard\n2. Drag widget\n3. Drop in new position', 'Widget moves to new position and persists', 3, 4, 2, 'E2E', 'HIGH', 'ACTIVE', 'dashboard,dnd', 5, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'TC-010', 'Chart Widget Data', 'Verify chart displays correct data', 'Chart widget on dashboard', '1. View chart widget\n2. Compare with source data', 'Chart shows accurate data with correct labels', 3, 4, 2, 'E2E', 'MEDIUM', 'ACTIVE', 'dashboard,charts', 8, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Performance test cases
(11, 'TC-011', 'Page Load Performance', 'Verify dashboard loads under 3s', 'User logged in', '1. Navigate to dashboard\n2. Measure load time', 'Dashboard loads in under 3 seconds', NULL, 4, NULL, 'PERFORMANCE', 'MEDIUM', 'ACTIVE', 'performance', 5, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 'TC-012', 'API Response Time', 'Verify API responds under 500ms', 'Server running', '1. Make API request\n2. Measure response time', 'Response received in under 500ms', NULL, 4, NULL, 'PERFORMANCE', 'LOW', 'ACTIVE', 'performance,api', 10, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- TEST RUNS (execution records)
-- Status: PASSED, FAILED, BLOCKED, SKIPPED
-- ===========================================
INSERT INTO test_runs (id, test_case_id, cycle_id, pitch_id, status, executed_by_id, executed_at, duration_seconds, notes, actual_result, build_version, environment) OVERRIDING SYSTEM VALUE VALUES
-- Recent test runs
(1, 1, 4, 14, 'PASSED', 6, '2025-10-20 10:00:00', 180, 'Login flow working', 'User logged in successfully', 'v1.2.0', 'staging'),
(2, 2, 4, 14, 'PASSED', 6, '2025-10-20 10:05:00', 120, 'Error handling correct', 'Error message displayed as expected', 'v1.2.0', 'staging'),
(3, 3, 4, 14, 'PASSED', 6, '2025-10-20 10:15:00', 420, 'Password reset works', 'Password changed successfully', 'v1.2.0', 'staging'),
(4, 4, 4, 1, 'PASSED', 6, '2025-10-25 14:00:00', 300, 'OAuth flow complete', 'GitHub connected successfully', 'v1.2.1', 'staging'),
(5, 5, 4, 1, 'FAILED', 6, '2025-10-26 09:00:00', 180, 'Webhook not received', 'Timeout waiting for webhook', 'v1.2.1', 'staging'),
(6, 5, 4, 1, 'PASSED', 6, '2025-10-27 11:00:00', 180, 'Fixed and re-tested', 'Webhook received and processed', 'v1.2.2', 'staging'),
(7, 7, 4, 2, 'PASSED', 6, '2025-10-28 15:00:00', 240, 'Channel config saved', 'Configuration persisted correctly', 'v1.2.2', 'staging'),
(8, 9, 4, 3, 'BLOCKED', 6, '2025-11-01 10:00:00', 60, 'Widget framework not ready', 'Could not test - feature incomplete', 'v1.2.3', 'staging');

-- ===========================================
-- BUG REPORTS
-- Severity: TRIVIAL, MINOR, MAJOR, CRITICAL, BLOCKER
-- Status: NEW, IN_PROGRESS, FIXED, VERIFIED, WONT_FIX, CLOSED
-- ===========================================
INSERT INTO bug_reports (id, bug_key, title, description, steps_to_reproduce, expected_behavior, actual_behavior, environment, pitch_id, cycle_id, team_id, test_run_id, severity, status, tags, reporter_id, assignee_id, resolution, resolved_at, created_at, updated_at, project_id) OVERRIDING SYSTEM VALUE VALUES
-- Active bugs
(1, 'BUG-001', 'Login button disabled on mobile', 'Login button appears disabled on iOS Safari', '1. Open app on iOS Safari\n2. Navigate to login\n3. Observe button state', 'Button should be clickable', 'Button appears grayed out but is clickable', 'iOS 17, Safari', 14, 4, NULL, NULL, 'MINOR', 'IN_PROGRESS', 'mobile,ui', 6, 2, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
(2, 'BUG-002', 'GitHub webhook timeout', 'Webhook events timeout after 30 seconds', '1. Configure webhook\n2. Trigger large PR event\n3. Check logs', 'Event should process within 10s', 'Event times out after 30s', 'Production', 1, 4, 1, 5, 'MAJOR', 'FIXED', 'github,performance', 6, 3, 'Increased timeout and added async processing', '2025-10-27', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
(3, 'BUG-003', 'Dashboard widget overlap', 'Widgets overlap when browser resized quickly', '1. Open dashboard\n2. Rapidly resize browser\n3. Observe widgets', 'Widgets should reflow smoothly', 'Widgets temporarily overlap', 'Chrome 120', 3, 4, 2, 8, 'TRIVIAL', 'NEW', 'dashboard,ui', 6, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
(4, 'BUG-004', 'Slack notification missing pitch title', 'Pitch title not included in notification', '1. Trigger pitch update notification\n2. Check Slack message', 'Message should include pitch title', 'Title shows as "undefined"', 'Staging', 2, 4, 1, NULL, 'MINOR', 'FIXED', 'slack,notification', 6, 2, 'Fixed template variable name', '2025-10-29', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
(5, 'BUG-005', 'Cannot save empty widget title', 'Validation error when clearing widget title', '1. Edit widget\n2. Clear title field\n3. Click save', 'Should show validation error', 'Crashes with 500 error', 'All environments', 3, 4, 2, NULL, 'MAJOR', 'IN_PROGRESS', 'dashboard,validation', 6, 5, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),

-- Past bugs (resolved)
(6, 'BUG-006', 'Password reset token expired', 'Tokens expiring after 1 hour instead of 24', '1. Request password reset\n2. Wait 2 hours\n3. Click link', 'Link should work for 24 hours', 'Link expires after 1 hour', 'Production', 14, 3, NULL, NULL, 'CRITICAL', 'VERIFIED', 'auth,security', 6, 3, 'Fixed token expiry configuration', '2025-10-01', '2025-09-28', '2025-10-01', 1),
(7, 'BUG-007', 'Hill chart not updating', 'Position changes not reflected on chart', '1. Update scope position\n2. Refresh page\n3. Check chart', 'Chart should show new position', 'Chart shows old position', 'All', 16, 2, NULL, NULL, 'MAJOR', 'VERIFIED', 'hill-chart,ui', 6, 2, 'Fixed cache invalidation', '2025-08-15', '2025-08-10', '2025-08-15', 1);

-- ===========================================
-- BUG ATTACHMENTS (sample file references)
-- ===========================================
INSERT INTO bug_attachments (bug_report_id, file_name, file_path, file_type, file_size, uploaded_by_id, uploaded_at) VALUES
(1, 'ios-login-screenshot.png', '/attachments/bugs/1/ios-login-screenshot.png', 'image/png', 125000, 6, CURRENT_TIMESTAMP),
(2, 'webhook-timeout-logs.txt', '/attachments/bugs/2/webhook-timeout-logs.txt', 'text/plain', 45000, 6, CURRENT_TIMESTAMP),
(3, 'widget-overlap.mp4', '/attachments/bugs/3/widget-overlap.mp4', 'video/mp4', 2500000, 6, CURRENT_TIMESTAMP);

-- Reset sequences
SELECT setval('test_cases_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM test_cases), false);
SELECT setval('test_runs_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM test_runs), false);
SELECT setval('bug_reports_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM bug_reports), false);
