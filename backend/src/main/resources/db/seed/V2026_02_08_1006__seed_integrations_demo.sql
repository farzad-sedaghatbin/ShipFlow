-- =============================================================================
-- V2026_02_08_1006: Seed Integration Demo Data (GitHub, Slack, Teams)
-- =============================================================================
-- This seed data demonstrates:
-- 1. GitHub integration with repositories, branches, commits, and PRs
-- 2. Slack workspace configuration and notification history
-- 3. Microsoft Teams configuration and notification history
-- Note: These are MOCK configurations for demo purposes, not real API connections
-- =============================================================================

-- =============================================================================
-- GITHUB INTEGRATION
-- =============================================================================

-- Clean up existing demo data (in correct order to respect foreign keys)
DELETE FROM task_github_links WHERE id BETWEEN 1 AND 10;
DELETE FROM pitch_github_links WHERE id BETWEEN 1 AND 10;
DELETE FROM github_pull_requests WHERE id BETWEEN 1 AND 10;
DELETE FROM github_commits WHERE id BETWEEN 1 AND 10;
DELETE FROM github_branches WHERE id BETWEEN 1 AND 10;
DELETE FROM github_configuration WHERE id BETWEEN 1 AND 10;
DELETE FROM github_repositories WHERE id BETWEEN 1 AND 10;
DELETE FROM slack_notification_history WHERE id BETWEEN 1 AND 10;
DELETE FROM slack_channel_config WHERE id BETWEEN 1 AND 10;
DELETE FROM slack_configuration WHERE id BETWEEN 1 AND 10;
DELETE FROM teams_notification_history WHERE id BETWEEN 1 AND 10;
DELETE FROM teams_channel_config WHERE id BETWEEN 1 AND 10;
DELETE FROM teams_configuration WHERE id BETWEEN 1 AND 10;

-- GitHub Repositories
INSERT INTO github_repositories (id, owner, name, full_name, url, default_branch, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE
VALUES
(1, 'acme-corp', 'customer-portal', 'acme-corp/customer-portal', 
 'https://github.com/acme-corp/customer-portal', 'main', true, 
 '2025-06-01 10:00:00', CURRENT_TIMESTAMP),
 
(2, 'acme-corp', 'customer-portal-api', 'acme-corp/customer-portal-api',
 'https://github.com/acme-corp/customer-portal-api', 'main', true,
 '2025-06-01 10:00:00', CURRENT_TIMESTAMP),

(3, 'acme-corp', 'shared-ui-components', 'acme-corp/shared-ui-components',
 'https://github.com/acme-corp/shared-ui-components', 'main', true,
 '2025-03-15 09:00:00', CURRENT_TIMESTAMP);

-- GitHub Branches
INSERT INTO github_branches (id, repository_id, name, head_sha, is_default, created_at, updated_at) OVERRIDING SYSTEM VALUE
VALUES
-- customer-portal branches
(1, 1, 'main', 'abc123def456789', true, '2025-06-01 10:00:00', CURRENT_TIMESTAMP),
(2, 1, 'develop', 'def789abc123456', false, '2025-06-01 10:00:00', CURRENT_TIMESTAMP),
(3, 1, 'feature/password-reset', 'pass123reset456', false, '2025-12-08 09:00:00', CURRENT_TIMESTAMP),
(4, 1, 'feature/2fa-implementation', '2fa789impl012', false, '2025-12-10 14:00:00', CURRENT_TIMESTAMP),
(5, 1, 'feature/realtime-notifications', 'notif123real456', false, '2026-01-06 08:00:00', CURRENT_TIMESTAMP),

-- customer-portal-api branches
(6, 2, 'main', 'api789main012345', true, '2025-06-01 10:00:00', CURRENT_TIMESTAMP),
(7, 2, 'develop', 'apidev456develop', false, '2025-06-01 10:00:00', CURRENT_TIMESTAMP),
(8, 2, 'feature/rate-limiting', 'rate123limit456', false, '2025-10-05 11:00:00', CURRENT_TIMESTAMP),
(9, 2, 'feature/audit-logging', 'audit789log123', false, '2025-10-08 09:00:00', CURRENT_TIMESTAMP),

-- shared-ui-components branches
(10, 3, 'main', 'ui123main456789', true, '2025-03-15 09:00:00', CURRENT_TIMESTAMP),
(11, 3, 'feature/dashboard-widgets', 'dash456widget789', false, '2025-10-01 10:00:00', CURRENT_TIMESTAMP);

-- GitHub Commits (recent activity)
INSERT INTO github_commits (id, repository_id, sha, message, author_name, author_email, author_username,
    commit_date, branch, url, created_at) OVERRIDING SYSTEM VALUE
VALUES
-- Password Reset feature commits
(1, 1, 'a1b2c3d4e5f6789012345678901234567890abcd', 
 'feat: Add password reset request form with email validation',
 'Alice Johnson', 'alice@acme.com', 'alicej', 
 '2025-12-08 10:30:00', 'feature/password-reset',
 'https://github.com/acme-corp/customer-portal/commit/a1b2c3d4', '2025-12-08 10:30:00'),

(2, 1, 'b2c3d4e5f6789012345678901234567890abcde',
 'feat: Implement secure token generation with JWT [CP-1]',
 'Bob Smith', 'bob@acme.com', 'bobsmith',
 '2025-12-08 14:00:00', 'feature/password-reset',
 'https://github.com/acme-corp/customer-portal/commit/b2c3d4e5', '2025-12-08 14:00:00'),

(3, 1, 'c3d4e5f6789012345678901234567890abcdef',
 'fix: Handle expired token edge case gracefully',
 'Bob Smith', 'bob@acme.com', 'bobsmith',
 '2025-12-09 09:15:00', 'feature/password-reset',
 'https://github.com/acme-corp/customer-portal/commit/c3d4e5f6', '2025-12-09 09:15:00'),

-- 2FA feature commits
(4, 1, 'd4e5f6789012345678901234567890abcdefgh',
 'feat: Add TOTP secret generation and QR code display',
 'Bob Smith', 'bob@acme.com', 'bobsmith',
 '2025-12-12 11:00:00', 'feature/2fa-implementation',
 'https://github.com/acme-corp/customer-portal/commit/d4e5f678', '2025-12-12 11:00:00'),

(5, 1, 'e5f6789012345678901234567890abcdefghij',
 'feat: Implement backup codes generation - fixes CP-4',
 'Alice Johnson', 'alice@acme.com', 'alicej',
 '2025-12-13 16:30:00', 'feature/2fa-implementation',
 'https://github.com/acme-corp/customer-portal/commit/e5f67890', '2025-12-13 16:30:00'),

-- Real-time notifications commits (current cycle)
(6, 1, 'f6789012345678901234567890abcdefghijkl',
 'feat: Set up WebSocket connection with STOMP protocol [CP-11]',
 'Bob Smith', 'bob@acme.com', 'bobsmith',
 '2026-01-06 14:00:00', 'feature/realtime-notifications',
 'https://github.com/acme-corp/customer-portal/commit/f6789012', '2026-01-06 14:00:00'),

(7, 1, 'g789012345678901234567890abcdefghijklm',
 'feat: Add notification toast component with animations',
 'Alice Johnson', 'alice@acme.com', 'alicej',
 '2026-01-07 10:30:00', 'feature/realtime-notifications',
 'https://github.com/acme-corp/customer-portal/commit/g7890123', '2026-01-07 10:30:00'),

-- API commits for rate limiting
(8, 2, 'h89012345678901234567890abcdefghijklmn',
 'feat: Implement token bucket rate limiter with Redis',
 'Bob Smith', 'bob@acme.com', 'bobsmith',
 '2025-10-16 12:00:00', 'feature/rate-limiting',
 'https://github.com/acme-corp/customer-portal-api/commit/h8901234', '2025-10-16 12:00:00'),

(9, 2, 'i9012345678901234567890abcdefghijklmno',
 'fix: Race condition in distributed rate counter - closes #127',
 'Bob Smith', 'bob@acme.com', 'bobsmith',
 '2025-10-22 09:00:00', 'feature/rate-limiting',
 'https://github.com/acme-corp/customer-portal-api/commit/i9012345', '2025-10-22 09:00:00'),

-- Audit logging commits
(10, 2, 'j012345678901234567890abcdefghijklmnop',
 'feat: Add event capture middleware for audit logging',
 'David Wilson', 'david@acme.com', 'davidw',
 '2025-10-23 11:00:00', 'feature/audit-logging',
 'https://github.com/acme-corp/customer-portal-api/commit/j0123456', '2025-10-23 11:00:00');

-- GitHub Pull Requests
INSERT INTO github_pull_requests (id, repository_id, pr_number, title, description, state, 
    head_branch, base_branch, author_username, url, opened_at, closed_at, merged_at, 
    merged_by_username, created_at, updated_at) OVERRIDING SYSTEM VALUE
VALUES
-- Merged PRs from Cycle 6
(1, 1, 142, 'feat: Password Reset Flow implementation',
 'Implements secure password reset as specified in pitch CP-1.\n\n## Changes\n- Reset request form\n- JWT token generation\n- Email integration with SendGrid\n- Token validation and password update\n\nCloses CP-1',
 'merged', 'feature/password-reset', 'main', 'bobsmith',
 'https://github.com/acme-corp/customer-portal/pull/142',
 '2025-12-15 10:00:00', '2025-12-16 14:00:00', '2025-12-16 14:00:00', 'alicej',
 '2025-12-15 10:00:00', '2025-12-16 14:00:00'),

(2, 2, 98, 'feat: API Rate Limiting with Redis',
 'Implements token bucket rate limiting.\n\n- Configurable limits per endpoint\n- Redis-backed distributed counters\n- Lua script for atomic operations\n\nFixes race condition reported in #127',
 'merged', 'feature/rate-limiting', 'main', 'bobsmith',
 'https://github.com/acme-corp/customer-portal-api/pull/98',
 '2025-10-18 09:00:00', '2025-10-20 15:00:00', '2025-10-20 15:00:00', 'davidw',
 '2025-10-18 09:00:00', '2025-10-20 15:00:00'),

(3, 2, 103, 'feat: Audit Logging System',
 'Complete audit logging implementation.\n\n- Event capture middleware\n- Elasticsearch storage\n- Search and filter API\n- CSV export functionality',
 'merged', 'feature/audit-logging', 'main', 'davidw',
 'https://github.com/acme-corp/customer-portal-api/pull/103',
 '2025-11-08 14:00:00', '2025-11-12 11:00:00', '2025-11-12 11:00:00', 'bobsmith',
 '2025-11-08 14:00:00', '2025-11-12 11:00:00'),

-- Open PRs (current cycle)
(4, 1, 156, 'feat: Real-time Notifications - WebSocket infrastructure',
 'Sets up WebSocket server and client connectivity.\n\n## Status\n- [x] WebSocket server configuration\n- [x] STOMP protocol integration\n- [x] Connection management\n- [ ] Reconnection logic (in progress)\n\nPart of CP-11',
 'open', 'feature/realtime-notifications', 'develop', 'bobsmith',
 'https://github.com/acme-corp/customer-portal/pull/156',
 '2026-01-08 09:00:00', NULL, NULL, NULL,
 '2026-01-08 09:00:00', CURRENT_TIMESTAMP),

(5, 1, 157, 'feat: Notification UI components',
 'Adds notification center and toast components.\n\n- Notification center dropdown\n- Toast notifications with animations\n- Unread badge indicator\n\nDepends on #156',
 'open', 'feature/realtime-notifications', 'develop', 'alicej',
 'https://github.com/acme-corp/customer-portal/pull/157',
 '2026-01-09 10:00:00', NULL, NULL, NULL,
 '2026-01-09 10:00:00', CURRENT_TIMESTAMP),

-- 2FA PR (ready for review)
(6, 1, 148, 'feat: Two-Factor Authentication with TOTP',
 'Implements TOTP-based 2FA.\n\n## Features\n- Authenticator app support\n- QR code setup wizard\n- 10 backup codes\n- 2FA challenge on login\n\nReady for security review.',
 'open', 'feature/2fa-implementation', 'develop', 'bobsmith',
 'https://github.com/acme-corp/customer-portal/pull/148',
 '2025-12-18 11:00:00', NULL, NULL, NULL,
 '2025-12-18 11:00:00', CURRENT_TIMESTAMP);

-- GitHub Configuration (per repository)
INSERT INTO github_configuration (id, repository_id, webhook_secret, access_token, 
    auto_link_enabled, auto_close_tasks_on_merge, created_at, updated_at) OVERRIDING SYSTEM VALUE
VALUES
(1, 1, 'demo_webhook_secret_portal_123', 'ghp_demo_token_portal_abc123',
 true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 2, 'demo_webhook_secret_api_456', 'ghp_demo_token_api_def456',
 true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 3, 'demo_webhook_secret_ui_789', 'ghp_demo_token_ui_ghi789',
 true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Pitch-GitHub Links (connecting pitches to branches and PRs)
INSERT INTO pitch_github_links (id, pitch_id, link_type, branch_id, linked_at, linked_by_user_id) OVERRIDING SYSTEM VALUE
VALUES
(1, 1, 'BRANCH', 3, '2025-12-08 09:30:00', 3),  -- Password Reset -> feature branch
(2, 4, 'BRANCH', 4, '2025-12-10 15:00:00', 3),  -- 2FA -> feature branch
(3, 11, 'BRANCH', 5, '2026-01-06 08:30:00', 3); -- Real-time notifications -> feature branch

INSERT INTO pitch_github_links (id, pitch_id, link_type, pull_request_id, linked_at, linked_by_user_id) OVERRIDING SYSTEM VALUE
VALUES
(4, 1, 'PULL_REQUEST', 1, '2025-12-15 10:30:00', 3),  -- Password Reset -> PR
(5, 17, 'PULL_REQUEST', 2, '2025-10-18 10:00:00', 3), -- Rate Limiting -> PR
(6, 18, 'PULL_REQUEST', 3, '2025-11-08 14:30:00', 5); -- Audit Logging -> PR

-- Task-GitHub Links (auto-linked via commit messages)
INSERT INTO task_github_links (id, task_id, link_type, commit_id, linked_at, linked_by_user_id, auto_linked) OVERRIDING SYSTEM VALUE
VALUES
(1, 1, 'COMMIT', 2, '2025-12-08 14:30:00', 3, true),  -- Auto-linked via [CP-1] in commit
(2, 5, 'COMMIT', 6, '2026-01-06 14:30:00', 3, true);  -- Auto-linked via [CP-11] in commit

-- =============================================================================
-- SLACK INTEGRATION
-- =============================================================================

-- Slack Workspace Configuration
INSERT INTO slack_configuration (id, workspace_name, webhook_url, default_channel, is_enabled, created_at, updated_at) OVERRIDING SYSTEM VALUE
VALUES
(1, 'Acme Engineering', 'https://hooks.slack.com/services/DEMO/WEBHOOK/URL123', 
 '#engineering-general', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Slack Channel Configurations
INSERT INTO slack_channel_config (id, slack_config_id, channel_name, channel_webhook_url,
    notify_task_assigned, notify_task_completed, notify_task_blocked,
    notify_pitch_shaped, notify_cycle_started, notify_cycle_cooldown,
    notify_betting_completed, notify_sprint_started, created_at, updated_at) OVERRIDING SYSTEM VALUE
VALUES
(1, 1, '#engineering-general', 'https://hooks.slack.com/services/DEMO/WEBHOOK/GEN123',
 false, false, false, true, true, true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(2, 1, '#cycle-updates', 'https://hooks.slack.com/services/DEMO/WEBHOOK/CYC456',
 false, false, false, true, true, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(3, 1, '#alpha-team', 'https://hooks.slack.com/services/DEMO/WEBHOOK/ALPHA789',
 true, true, true, false, false, false, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(4, 1, '#beta-team', 'https://hooks.slack.com/services/DEMO/WEBHOOK/BETA012',
 true, true, true, false, false, false, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Slack Notification History (recent notifications)
INSERT INTO slack_notification_history (id, slack_config_id, channel_name, notification_type,
    message_text, entity_type, entity_id, sent_at, success, error_message) OVERRIDING SYSTEM VALUE
VALUES
(1, 1, '#engineering-general', 'CYCLE_STARTED',
 ':rocket: *Cycle 5 - Build Sprint* has started!\nProject: Customer Portal\nDuration: Jan 5 - Feb 13',
 'CYCLE', 5, '2026-01-05 09:00:00', true, NULL),

(2, 1, '#cycle-updates', 'PITCH_SHAPED',
 ':bulb: New pitch shaped: *Real-time Notifications*\nAppetite: 14 days\nTeam: Phoenix Team',
 'PITCH', 11, '2026-01-04 15:00:00', true, NULL),

(3, 1, '#alpha-team', 'TASK_ASSIGNED',
 ':clipboard: Task assigned to @alice:\n*Refactor authentication middleware*\nPriority: HIGH\nDue: Jan 20',
 'TASK', 1, '2026-01-06 10:30:00', true, NULL),

(4, 1, '#alpha-team', 'TASK_BLOCKED',
 ':warning: Task blocked: *Handle WebSocket reconnection*\nBlocked by: #2 Update deprecated dependencies\nAssigned: @bob',
 'TASK', 5, '2026-01-08 11:00:00', true, NULL),

(5, 1, '#engineering-general', 'BETTING_COMPLETED',
 ':game_die: *Betting Ceremony Complete for Cycle 4*\n\n:white_check_mark: Committed (3):\n• Password Reset Flow → Alpha Team\n• Two-Factor Authentication → Alpha Team\n• Offline Mode → Beta Team\n\n:x: Not this cycle (7 pitches deferred/rejected)',
 'CYCLE', 4, '2025-12-05 17:00:00', true, NULL),

-- Failed notification (for demo)
(6, 1, '#beta-team', 'TASK_COMPLETED',
 ':white_check_mark: Task completed: *Performance benchmarks for rate limiter*\nBy: @emma\nActual: 5.5 hours (estimated: 6 hours)',
 'TASK', 2, '2025-10-19 17:30:00', false, 'channel_not_found: Channel #beta-team has been archived');

-- =============================================================================
-- MICROSOFT TEAMS INTEGRATION
-- =============================================================================

-- Teams Workspace Configuration
INSERT INTO teams_configuration (id, tenant_name, webhook_url, default_channel, is_enabled, created_at, updated_at) OVERRIDING SYSTEM VALUE
VALUES
(1, 'Acme Corporation', 'https://acme.webhook.office.com/webhookb2/demo-webhook-url',
 'Engineering General', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Teams Channel Configurations
INSERT INTO teams_channel_config (id, teams_config_id, channel_name, channel_webhook_url,
    notify_task_assigned, notify_task_completed, notify_task_blocked,
    notify_pitch_shaped, notify_cycle_started, notify_cycle_cooldown,
    notify_betting_completed, notify_sprint_started, created_at, updated_at) OVERRIDING SYSTEM VALUE
VALUES
(1, 1, 'Engineering General', 'https://acme.webhook.office.com/webhookb2/eng-general',
 false, false, false, true, true, true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(2, 1, 'Product Updates', 'https://acme.webhook.office.com/webhookb2/product-updates',
 false, true, false, true, true, true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(3, 1, 'Phoenix Team', 'https://acme.webhook.office.com/webhookb2/phoenix-team',
 true, true, true, false, false, false, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Teams Notification History
INSERT INTO teams_notification_history (id, teams_config_id, channel_name, notification_type,
    message_text, entity_type, entity_id, sent_at, success, error_message) OVERRIDING SYSTEM VALUE
VALUES
(1, 1, 'Engineering General', 'CYCLE_STARTED',
 '**Cycle 5 - Build Sprint has started!**\n\nProject: Customer Portal\nDuration: Jan 5 - Feb 13\n\n[View Cycle](https://shipflow.example.com/cycles/5)',
 'CYCLE', 5, '2026-01-05 09:00:00', true, NULL),

(2, 1, 'Product Updates', 'CYCLE_COOLDOWN',
 '**Cycle 6 has entered COOLDOWN phase**\n\nAll pitches completed!\n- Email Notification System ✅\n- User Dashboard Redesign ✅\n- API Rate Limiting ✅\n- Audit Logging System ✅\n\nCooldown runs until Nov 30.',
 'CYCLE', 6, '2025-11-15 10:00:00', true, NULL),

(3, 1, 'Phoenix Team', 'TASK_ASSIGNED',
 '**New task assigned**\n\n**Task:** Refactor authentication middleware\n**Assigned to:** Bob Smith\n**Priority:** HIGH\n**Due:** Jan 20, 2026',
 'TASK', 1, '2026-01-06 10:30:00', true, NULL);

-- =============================================================================
-- Reset sequences
-- =============================================================================
SELECT setval(pg_get_serial_sequence('github_repositories', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('github_branches', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('github_commits', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('github_pull_requests', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('github_configuration', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('pitch_github_links', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('task_github_links', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('slack_configuration', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('slack_channel_config', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('slack_notification_history', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('teams_configuration', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('teams_channel_config', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('teams_notification_history', 'id'), 100, false);
