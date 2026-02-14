-- =============================================================================
-- V2026_02_08_1005: Seed Custom Dashboards, Metrics, and Knowledge/QA System
-- =============================================================================
-- This seed data demonstrates:
-- 1. Custom dashboards with widget layouts
-- 2. Custom metrics with formulas
-- 3. Metric data points for trending
-- 4. Knowledge items for RAG system
-- 5. QA interactions (Q&A history)
-- 6. Manual notes
-- =============================================================================

-- =============================================================================
-- CUSTOM METRICS
-- =============================================================================

-- Clean up existing demo data
DELETE FROM metric_data_points WHERE metric_id BETWEEN 1 AND 7;
DELETE FROM dashboard_widget_configs WHERE dashboard_id BETWEEN 1 AND 6;
DELETE FROM custom_dashboards WHERE id BETWEEN 1 AND 6;
DELETE FROM custom_metrics WHERE id BETWEEN 1 AND 7;
DELETE FROM qa_interactions WHERE id BETWEEN 1 AND 10;
DELETE FROM knowledge_items WHERE id BETWEEN 1 AND 10;
DELETE FROM manual_notes WHERE id BETWEEN 1 AND 10;

INSERT INTO custom_metrics (id, user_id, name, description, formula, data_source, aggregation_type, 
    filters, display_format, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE
VALUES
-- Metrics for Alice (Project Manager)
(1, 2, 'Team Velocity', 'Average story points completed per cycle', 
 'SUM(completed_story_points) / COUNT(cycles)', 'CYCLE', 'AVG',
 '{"projectId": 1, "cycleStatus": "CLOSED"}', 'NUMBER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(2, 2, 'On-Time Delivery Rate', 'Percentage of pitches delivered within appetite',
 '(COUNT(pitches WHERE actual_days <= appetite_days) / COUNT(pitches)) * 100', 'PITCH', 'RATIO',
 '{"projectId": 1, "status": "DONE"}', 'PERCENTAGE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(3, 2, 'Bug Escape Rate', 'Production bugs found after release per cycle',
 'COUNT(bugs WHERE environment = production) / COUNT(cycles)', 'CUSTOM', 'AVG',
 '{"severity": ["CRITICAL", "MAJOR"]}', 'NUMBER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Metrics for Bob (Developer)
(4, 3, 'Code Review Turnaround', 'Average hours from PR opened to first review',
 'AVG(first_review_at - pr_opened_at)', 'CUSTOM', 'AVG',
 '{"teamId": 4}', 'DURATION', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(5, 3, 'Tech Debt Ratio', 'Hours spent on tech debt vs feature work',
 'SUM(work_logs WHERE tags CONTAINS tech-debt) / SUM(work_logs)', 'WORK_LOG', 'RATIO',
 '{"cycleId": 5}', 'PERCENTAGE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Metrics for Emma (QA)
(6, 6, 'Test Coverage Trend', 'Unit test coverage percentage over time',
 'test_coverage_percentage', 'CUSTOM', 'AVG',
 '{"projectId": 1}', 'PERCENTAGE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(7, 6, 'Defect Detection Rate', 'Bugs found before vs after release',
 '(bugs_in_testing / (bugs_in_testing + bugs_in_production)) * 100', 'CUSTOM', 'RATIO',
 '{"projectId": 1}', 'PERCENTAGE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- METRIC DATA POINTS (historical values for trending)
-- =============================================================================

-- Team Velocity over last 3 cycles
INSERT INTO metric_data_points (metric_id, calculated_value, calculation_date, metadata)
VALUES
(1, 34.5, '2025-09-15', '{"cycleId": 5, "pitchesCompleted": 3}'),
(1, 38.2, '2025-10-30', '{"cycleId": 6, "pitchesCompleted": 4}'),
(1, 41.0, '2026-01-10', '{"cycleId": 5, "pitchesCompleted": 2, "inProgress": 2}');

-- On-Time Delivery Rate
INSERT INTO metric_data_points (metric_id, calculated_value, calculation_date, metadata)
VALUES
(2, 75.0, '2025-09-15', '{"onTime": 3, "total": 4}'),
(2, 82.0, '2025-10-30', '{"onTime": 4, "total": 4}'),
(2, 85.5, '2026-01-10', '{"onTime": 2, "total": 2}');

-- Bug Escape Rate
INSERT INTO metric_data_points (metric_id, calculated_value, calculation_date, metadata)
VALUES
(3, 3.2, '2025-09-15', '{"criticalBugs": 2, "highBugs": 4}'),
(3, 2.0, '2025-10-30', '{"criticalBugs": 1, "highBugs": 3}'),
(3, 1.5, '2026-01-10', '{"criticalBugs": 0, "highBugs": 2}');

-- Code Review Turnaround (in hours)
INSERT INTO metric_data_points (metric_id, calculated_value, calculation_date, metadata)
VALUES
(4, 8.5, '2025-10-01', '{"prsReviewed": 12}'),
(4, 6.2, '2025-11-01', '{"prsReviewed": 15}'),
(4, 4.8, '2026-01-10', '{"prsReviewed": 8}');

-- Tech Debt Ratio
INSERT INTO metric_data_points (metric_id, calculated_value, calculation_date, metadata)
VALUES
(5, 22.5, '2025-10-01', '{"techDebtHours": 45, "totalHours": 200}'),
(5, 18.0, '2025-11-01', '{"techDebtHours": 36, "totalHours": 200}'),
(5, 15.5, '2026-01-10', '{"techDebtHours": 31, "totalHours": 200}');

-- Test Coverage Trend
INSERT INTO metric_data_points (metric_id, calculated_value, calculation_date, metadata)
VALUES
(6, 72.0, '2025-09-01', '{"lines": 8500, "covered": 6120}'),
(6, 78.0, '2025-10-01', '{"lines": 9200, "covered": 7176}'),
(6, 82.0, '2025-11-01', '{"lines": 10500, "covered": 8610}'),
(6, 85.0, '2026-01-10', '{"lines": 11200, "covered": 9520}');

-- Defect Detection Rate
INSERT INTO metric_data_points (metric_id, calculated_value, calculation_date, metadata)
VALUES
(7, 65.0, '2025-09-01', '{"inTesting": 13, "inProduction": 7}'),
(7, 72.0, '2025-10-01', '{"inTesting": 18, "inProduction": 7}'),
(7, 80.0, '2025-11-01', '{"inTesting": 20, "inProduction": 5}'),
(7, 88.0, '2026-01-10', '{"inTesting": 22, "inProduction": 3}');

-- =============================================================================
-- CUSTOM DASHBOARDS
-- =============================================================================

INSERT INTO custom_dashboards (id, user_id, name, description, is_default, layout_config, 
    is_template, template_category, created_at, updated_at) OVERRIDING SYSTEM VALUE
VALUES
-- Alice's dashboards
(1, 2, 'Project Health Overview', 'Key metrics for monitoring project health and team performance',
 true, '{"columns": 3, "rowHeight": 200, "gap": 16}', false, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(2, 2, 'Cycle Progress', 'Track current cycle progress and upcoming milestones',
 false, '{"columns": 2, "rowHeight": 250, "gap": 12}', false, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Bob's dashboard
(3, 3, 'Developer Productivity', 'Personal productivity metrics and code quality indicators',
 true, '{"columns": 2, "rowHeight": 220, "gap": 16}', false, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Emma's dashboard
(4, 6, 'QA Dashboard', 'Test execution status and quality metrics',
 true, '{"columns": 3, "rowHeight": 180, "gap": 16}', false, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- System templates (available to all users)
(5, 1, 'Executive Summary', 'High-level overview for stakeholders and management',
 false, '{"columns": 3, "rowHeight": 200, "gap": 16}', true, 'EXECUTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(6, 1, 'Developer Template', 'Standard developer dashboard with coding metrics',
 false, '{"columns": 2, "rowHeight": 220, "gap": 16}', true, 'DEVELOPER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- DASHBOARD WIDGET CONFIGURATIONS
-- =============================================================================

-- Alice's Project Health Overview widgets
INSERT INTO dashboard_widget_configs (dashboard_id, widget_id, widget_type, metric_id,
    position_x, position_y, width, height, data_filters, chart_config, settings, display_order, created_at, updated_at)
VALUES
(1, NULL, 'CUSTOM_KPI', 1, 0, 0, 1, 1, NULL, NULL, 
 '{"title": "Team Velocity", "icon": "trending-up", "color": "#22c55e"}', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
 
(1, NULL, 'CUSTOM_KPI', 2, 1, 0, 1, 1, NULL, NULL,
 '{"title": "On-Time Delivery", "icon": "check-circle", "color": "#3b82f6"}', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(1, NULL, 'CUSTOM_KPI', 3, 2, 0, 1, 1, NULL, NULL,
 '{"title": "Bug Escape Rate", "icon": "bug", "color": "#ef4444"}', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(1, NULL, 'CHART', 1, 0, 1, 2, 2, 
 '{"projectId": 1}', 
 '{"chartType": "LINE", "colors": ["#22c55e"], "showLegend": true}',
 '{"title": "Velocity Trend", "timeRange": "6_MONTHS"}', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(1, NULL, 'CHART', 2, 2, 1, 1, 2,
 '{"projectId": 1}',
 '{"chartType": "GAUGE", "colors": ["#ef4444", "#f59e0b", "#22c55e"], "thresholds": [60, 80]}',
 '{"title": "Delivery Health", "showTarget": true}', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Bob's Developer Productivity widgets
INSERT INTO dashboard_widget_configs (dashboard_id, widget_id, widget_type, metric_id,
    position_x, position_y, width, height, data_filters, chart_config, settings, display_order, created_at, updated_at)
VALUES
(3, NULL, 'CUSTOM_KPI', 4, 0, 0, 1, 1, NULL, NULL,
 '{"title": "PR Review Time", "icon": "git-pull-request", "color": "#8b5cf6", "format": "hours"}', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(3, NULL, 'CUSTOM_KPI', 5, 1, 0, 1, 1, NULL, NULL,
 '{"title": "Tech Debt %%", "icon": "tool", "color": "#f59e0b"}', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(3, NULL, 'CHART', 4, 0, 1, 2, 2,
 '{"teamId": 4}',
 '{"chartType": "BAR", "colors": ["#8b5cf6"], "showAverage": true}',
 '{"title": "PR Review Turnaround", "timeRange": "3_MONTHS"}', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Emma's QA Dashboard widgets
INSERT INTO dashboard_widget_configs (dashboard_id, widget_id, widget_type, metric_id,
    position_x, position_y, width, height, data_filters, chart_config, settings, display_order, created_at, updated_at)
VALUES
(4, NULL, 'CUSTOM_KPI', 6, 0, 0, 1, 1, NULL, NULL,
 '{"title": "Test Coverage", "icon": "shield", "color": "#22c55e"}', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(4, NULL, 'CUSTOM_KPI', 7, 1, 0, 1, 1, NULL, NULL,
 '{"title": "Defect Detection", "icon": "search", "color": "#3b82f6"}', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(4, NULL, 'CHART', 6, 0, 1, 2, 2,
 '{"projectId": 1}',
 '{"chartType": "AREA", "colors": ["#22c55e"], "fillOpacity": 0.3}',
 '{"title": "Coverage Over Time", "timeRange": "6_MONTHS", "showGoal": 90}', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(4, NULL, 'CHART', 7, 2, 0, 1, 2,
 '{"projectId": 1}',
 '{"chartType": "PIE", "colors": ["#22c55e", "#ef4444"]}',
 '{"title": "Bugs: Testing vs Production"}', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Developer Template widgets (system template for developers)
INSERT INTO dashboard_widget_configs (dashboard_id, widget_id, widget_type, metric_id,
    position_x, position_y, width, height, data_filters, chart_config, settings, display_order, created_at, updated_at)
VALUES
(6, NULL, 'TABLE', NULL, 0, 0, 8, 4, NULL, NULL,
 '{"title": "My Active Tasks", "pageSize": 10, "searchable": true, "sourceType": "TASK_LIST", "sortBy": "priority", "sortOrder": "desc", "limit": 10, "filters": [{"field": "status", "operator": "in", "value": ["TODO", "IN_PROGRESS"]}]}', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(6, NULL, 'CUSTOM_KPI', 4, 8, 0, 2, 2, NULL, NULL,
 '{"title": "Code Review Time", "icon": "git-pull-request", "color": "#8b5cf6", "format": "hours"}', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(6, NULL, 'CUSTOM_KPI', 5, 10, 0, 2, 2, NULL, NULL,
 '{"title": "Tech Debt", "icon": "tool", "color": "#f59e0b"}', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Executive Summary Template widgets (high-level overview for stakeholders)
INSERT INTO dashboard_widget_configs (dashboard_id, widget_id, widget_type, metric_id,
    position_x, position_y, width, height, data_filters, chart_config, settings, display_order, created_at, updated_at)
VALUES
(5, NULL, 'CUSTOM_KPI', 1, 0, 0, 1, 1, NULL, NULL,
 '{"title": "Team Velocity", "icon": "trending-up", "color": "#22c55e"}', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(5, NULL, 'CUSTOM_KPI', 2, 1, 0, 1, 1, NULL, NULL,
 '{"title": "On-Time Delivery", "icon": "check-circle", "color": "#3b82f6"}', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(5, NULL, 'CUSTOM_KPI', 3, 2, 0, 1, 1, NULL, NULL,
 '{"title": "Bug Escape Rate", "icon": "bug", "color": "#ef4444"}', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(5, NULL, 'TABLE', NULL, 0, 1, 2, 2, NULL, NULL,
 '{"title": "Active Pitches", "pageSize": 5, "searchable": false, "sourceType": "PITCH_LIST", "sortBy": "createdAt", "sortOrder": "desc", "limit": 5, "filters": [{"field": "status", "operator": "in", "value": ["PENDING", "IN_PROGRESS"]}]}', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(5, NULL, 'TABLE', NULL, 2, 1, 1, 2, NULL, NULL,
 '{"title": "Critical Bugs", "pageSize": 5, "searchable": false, "sourceType": "BUG_LIST", "sortBy": "severity", "sortOrder": "desc", "limit": 5, "filters": [{"field": "severity", "operator": "equals", "value": "CRITICAL"}]}', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- KNOWLEDGE ITEMS (for RAG system)
-- =============================================================================

INSERT INTO knowledge_items (id, entity_type, entity_id, content, title, cycle_id, team_id, pitch_id,
    author_id, is_embedded, chunk_index, total_chunks, created_at, updated_at) OVERRIDING SYSTEM VALUE
VALUES
-- Pitch documentation as knowledge items
(1, 'PITCH', 1, 'The Password Reset Flow feature implements secure password recovery using JWT tokens with 15-minute expiry. Key security measures include rate limiting (3 requests per hour per email), one-time token use, and full session invalidation on password change. The implementation uses SendGrid for email delivery with verified domain authentication.',
 'Password Reset Flow - Technical Overview', 4, 1, 1, 3, false, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(2, 'PITCH', 4, 'Two-Factor Authentication implementation uses TOTP (Time-based One-Time Password) compatible with Google Authenticator, Authy, and 1Password. The system generates 10 backup codes during setup for account recovery. No SMS or email fallback is provided to maintain security standards. Auth0 was evaluated but TOTP was chosen for lower operational cost.',
 '2FA Implementation - Technical Decisions', 4, 1, 4, 3, false, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(3, 'PITCH', 11, 'Real-time Notifications architecture uses Spring WebSocket with STOMP protocol. Redis pub/sub enables multi-instance support for horizontal scaling. Reconnection uses exponential backoff starting at 1 second with max 30 seconds. The notification service is event-driven with async processing to prevent blocking the main application flow.',
 'Real-time Notifications - Architecture', 5, 4, 11, 3, false, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Meeting notes as knowledge items
(4, 'MEETING', 1, 'Kickoff meeting for Real-time Notifications discussed WebSocket scaling concerns. Decision: Use Redis pub/sub instead of in-memory channel management. This allows horizontal scaling without sticky sessions. Team agreed to implement exponential backoff for reconnection.',
 'Real-time Notifications Kickoff Decisions', 5, 4, 11, 2, false, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Retrospective insights as knowledge items
(5, 'RETROSPECTIVE', 1, 'Cycle 6 Retrospective identified key improvement areas: 1) Need for earlier performance testing - dashboard had memory leak discovered late, 2) Documentation gaps slowed onboarding, 3) Too many meetings disrupted deep work. Action items: Feature flags implementation, no-meeting mornings policy, and ADR templates.',
 'Cycle 6 Retro Insights', 6, NULL, NULL, 4, false, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Bug resolution knowledge
(6, 'BUG_REPORT', 1, 'Gmail email delivery issue was caused by missing SPF record for domain. Resolution: Added SPF record and verified DKIM signing with SendGrid. Lesson learned: Always verify DNS records when changing email providers. Email deliverability testing should be part of QA checklist.',
 'Gmail Delivery Bug - Root Cause Analysis', 6, 6, 15, 3, false, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Technical decision records
(7, 'MANUAL_NOTE', 1, 'Rate Limiting Algorithm Selection: Chose Token Bucket over Leaky Bucket for API rate limiting. Reasons: 1) Allows bursting up to bucket capacity, which is friendlier for bursty client behavior, 2) Easier to reason about and configure, 3) Well-supported in Redis with INCRBY and lua scripts. Fixed sliding window was considered but adds complexity for distributed scenarios.',
 'Rate Limiting Algorithm Decision', 6, 7, 17, 3, false, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Onboarding knowledge
(8, 'MANUAL_NOTE', 2, 'Developer Onboarding: New developers should start with the README.md in each service repository. Key environment setup steps: 1) Install Docker Desktop, 2) Run docker-compose up for local services, 3) Run mvn spring-boot:run for backend, 4) Run npm run dev for frontend. Common issues: Port 8080 conflict with other services, Redis connection refused if Docker not running.',
 'Developer Onboarding Guide', NULL, NULL, NULL, 2, false, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================================================
-- QA INTERACTIONS (Q&A history)
-- =============================================================================

INSERT INTO qa_interactions (id, question, answer, context_type, context_id, cycle_id, team_id,
    user_id, source_knowledge_ids, confidence_score, processing_time_ms, created_at) OVERRIDING SYSTEM VALUE
VALUES
-- User questions and RAG-generated answers
(1, 'How does the password reset token work?',
 'The password reset flow uses JWT tokens with a 15-minute expiry time. When a user requests a reset, a secure token is generated and sent via email through SendGrid. The token can only be used once - after successful password reset, it''s invalidated. Additionally, all existing user sessions are terminated when the password is changed for security. Rate limiting prevents abuse with a maximum of 3 reset requests per email per hour.',
 'PITCH', 1, 4, 1, 3, '1', 92, 450, '2025-12-10 14:30:00'),

(2, 'What was decided about 2FA SMS support?',
 'SMS-based 2FA was intentionally excluded from the Two-Factor Authentication feature. The team evaluated SMS but decided against it for several reasons: 1) SMS is considered less secure due to SIM swapping attacks, 2) SMS adds operational complexity and cost, 3) TOTP-based authentication with backup codes provides sufficient recovery options. The decision is documented in the pitch risks section and was confirmed during the betting ceremony.',
 'PITCH', 4, 4, 1, 2, '2', 95, 380, '2025-12-15 10:00:00'),

(3, 'Why did we have email delivery issues with Gmail?',
 'The Gmail delivery issue was caused by a missing SPF (Sender Policy Framework) record in our DNS configuration after switching to SendGrid. Without proper SPF and DKIM authentication, Gmail''s spam filters were blocking our emails. The resolution involved adding the correct SPF record and verifying DKIM signing through SendGrid''s dashboard. This is now part of our QA checklist for email-related features.',
 'BUG_REPORT', 1, 6, 6, 6, '6', 98, 320, '2025-11-12 09:00:00'),

(4, 'What are the key Action Items from the last retrospective?',
 'The Cycle 6 retrospective identified four main action items: 1) Bob is implementing feature flag infrastructure using LaunchDarkly - this is now complete, 2) Carol proposed a no-meeting mornings policy to management - approved and in effect, 3) Emma is integrating performance tests into the CI pipeline - in progress with Lighthouse CI added, 4) David created an ADR (Architecture Decision Record) template for technical decisions - complete and being used for new pitches.',
 'RETROSPECTIVE', 1, 6, NULL, 4, '5', 90, 520, '2025-12-01 11:30:00'),

(5, 'How do we handle WebSocket reconnection?',
 'WebSocket reconnection uses exponential backoff with jitter to prevent thundering herd problems. The algorithm starts with a 1-second delay, doubles after each failed attempt up to a maximum of 30 seconds, and adds random jitter of ±20% to spread out reconnection attempts. After successful reconnection, the client receives any missed notifications. If reconnection fails after 10 attempts, the user is shown an error suggesting they refresh the page.',
 'PITCH', 11, 5, 4, 2, '3', 88, 410, '2026-01-08 16:00:00'),

-- Question with feedback
(6, 'What''s our test coverage target?',
 'Based on the QA dashboard metrics, our test coverage target is 90%. Current coverage is at 85% and trending upward. The team has been consistently improving coverage over the past 6 months, moving from 72% to 85%. Priority areas for coverage improvement include the notification service and the new search functionality.',
 'CUSTOM', NULL, NULL, NULL, 6, '6,7', 85, 380, '2026-01-05 14:00:00');

-- Add feedback to one interaction
UPDATE qa_interactions 
SET feedback_type = 'HELPFUL', 
    feedback_at = '2026-01-05 14:10:00'
WHERE question = 'What''s our test coverage target?';

-- =============================================================================
-- MANUAL NOTES
-- =============================================================================

INSERT INTO manual_notes (id, title, content, context_type, context_id, cycle_id, team_id, pitch_id,
    author_id, include_in_knowledge, created_at, updated_at) OVERRIDING SYSTEM VALUE
VALUES
(1, 'Rate Limiting Configuration', 
 'Production rate limit settings:\n- Default: 1000 requests/minute per API key\n- Premium: 5000 requests/minute\n- Admin: No limit\n\nBurst allowance: 20% over limit for 10-second windows.\n\nRedis key format: rate_limit:{api_key}:{minute_bucket}',
 'PITCH', 17, 6, 7, 17, 3, true, '2025-10-22 10:00:00', '2025-10-22 10:00:00'),

(2, 'Dashboard Widget Development Guide',
 'To create a new dashboard widget:\n1. Create component in src/components/widgets/\n2. Register in widget-registry.ts\n3. Add type to WidgetType enum\n4. Implement data fetching hook in src/hooks/\n5. Add entry in widget config defaults\n\nWidget interface requires: id, type, renderConfig, dataSource.',
 'PITCH', 16, 6, 6, 16, 2, true, '2025-11-06 15:00:00', '2025-11-06 15:00:00'),

(3, 'Elasticsearch Index Mappings',
 'Search index mappings for content types:\n\n```json\n{\n  "pitches": {\n    "properties": {\n      "title": {"type": "text", "analyzer": "standard"},\n      "description": {"type": "text"},\n      "tags": {"type": "keyword"}\n    }\n  }\n}\n```\n\nRe-index command: `./scripts/reindex-search.sh`',
 'PITCH', 12, 5, 4, 12, 3, true, '2026-01-09 11:00:00', '2026-01-09 11:00:00'),

(4, 'Deployment Checklist',
 'Pre-deployment checklist:\n- [ ] All tests passing in CI\n- [ ] Database migrations reviewed\n- [ ] Feature flags configured\n- [ ] Rollback plan documented\n- [ ] Monitoring alerts configured\n- [ ] On-call notified\n\nPost-deployment:\n- [ ] Smoke tests passing\n- [ ] Error rates normal\n- [ ] Performance metrics stable',
 'CYCLE', 5, 5, NULL, NULL, 2, true, '2026-01-04 16:00:00', '2026-01-04 16:00:00');

-- =============================================================================
-- Reset sequences
-- =============================================================================
SELECT setval(pg_get_serial_sequence('custom_metrics', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('metric_data_points', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('custom_dashboards', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('dashboard_widget_configs', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('knowledge_items', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('qa_interactions', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('manual_notes', 'id'), 100, false);
