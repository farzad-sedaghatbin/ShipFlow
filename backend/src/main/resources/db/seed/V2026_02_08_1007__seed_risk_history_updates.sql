-- =============================================================================
-- V2026_02_08_1007: Seed Risk History, Risk Feedback, and Hill Chart History
-- =============================================================================
-- This seed data demonstrates:
-- 1. Pitch risk history snapshots for trend analysis
-- 2. Risk feedback from users (AI model improvement data)
-- 3. Hill chart history (position changes over time)
-- =============================================================================

-- =============================================================================
-- PITCH RISK HISTORY (snapshots of risk scores over time)
-- =============================================================================

-- Clean up existing demo data
DELETE FROM pitch_risk_history WHERE id BETWEEN 1 AND 30;

-- Risk history for Pitch 11 (Real-time Notifications) - current cycle, showing evolution
INSERT INTO pitch_risk_history (id, pitch_id, risk_score, risk_level, risk_factors_json, recorded_at, trigger_type) OVERRIDING SYSTEM VALUE
VALUES
-- Initial assessment at cycle start
(1, 11, 45, 'MEDIUM', 
 '[{"factor": "technical_complexity", "score": 15, "description": "WebSocket infrastructure new to team"}, {"factor": "integration_risk", "score": 10, "description": "Integration with existing notification system"}, {"factor": "scope_uncertainty", "score": 20, "description": "User preferences scope unclear"}]',
 '2026-01-05 10:00:00', 'STATUS_CHANGE'),

-- After kickoff meeting - risk slightly reduced
(2, 11, 38, 'MEDIUM',
 '[{"factor": "technical_complexity", "score": 12, "description": "Team reviewed WebSocket patterns, more confident"}, {"factor": "integration_risk", "score": 8, "description": "Integration points identified"}, {"factor": "scope_uncertainty", "score": 18, "description": "User preferences still being defined"}]',
 '2026-01-05 17:00:00', 'MANUAL'),

-- After first week - good progress, risk decreasing
(3, 11, 28, 'LOW',
 '[{"factor": "technical_complexity", "score": 8, "description": "WebSocket server working in dev"}, {"factor": "integration_risk", "score": 5, "description": "Integration tested, no issues"}, {"factor": "scope_uncertainty", "score": 15, "description": "Preferences scoped down"}]',
 '2026-01-08 17:00:00', 'SCHEDULED'),

-- Current state
(4, 11, 22, 'LOW',
 '[{"factor": "technical_complexity", "score": 5, "description": "Core infrastructure complete"}, {"factor": "integration_risk", "score": 2, "description": "All integrations working"}, {"factor": "scope_uncertainty", "score": 15, "description": "UI polish remaining"}]',
 '2026-01-10 17:00:00', 'SCHEDULED');

-- Risk history for Pitch 12 (Advanced Search) - showing moderate risk
INSERT INTO pitch_risk_history (id, pitch_id, risk_score, risk_level, risk_factors_json, recorded_at, trigger_type) OVERRIDING SYSTEM VALUE
VALUES
(5, 12, 52, 'MEDIUM',
 '[{"factor": "technical_complexity", "score": 20, "description": "Elasticsearch new technology for team"}, {"factor": "performance_risk", "score": 18, "description": "Large dataset indexing concerns"}, {"factor": "scope_uncertainty", "score": 14, "description": "Filter requirements evolving"}]',
 '2026-01-05 10:00:00', 'STATUS_CHANGE'),

(6, 12, 48, 'MEDIUM',
 '[{"factor": "technical_complexity", "score": 18, "description": "ES cluster setup completed"}, {"factor": "performance_risk", "score": 18, "description": "Indexing slow on large datasets"}, {"factor": "scope_uncertainty", "score": 12, "description": "Filters finalized"}]',
 '2026-01-08 17:00:00', 'SCHEDULED'),

(7, 12, 42, 'MEDIUM',
 '[{"factor": "technical_complexity", "score": 12, "description": "Search API working well"}, {"factor": "performance_risk", "score": 20, "description": "Need to optimize autocomplete latency"}, {"factor": "scope_uncertainty", "score": 10, "description": "Scope stable"}]',
 '2026-01-10 17:00:00', 'SCHEDULED');

-- Risk history for Pitch 14 (Team Collaboration Hub) - high risk, circuit breaker triggered
INSERT INTO pitch_risk_history (id, pitch_id, risk_score, risk_level, risk_factors_json, recorded_at, trigger_type) OVERRIDING SYSTEM VALUE
VALUES
(8, 14, 55, 'MEDIUM',
 '[{"factor": "technical_complexity", "score": 18, "description": "Real-time sync complexity"}, {"factor": "dependency_risk", "score": 20, "description": "Depends on notifications feature"}, {"factor": "scope_uncertainty", "score": 17, "description": "@mentions implementation unclear"}]',
 '2026-01-05 10:00:00', 'STATUS_CHANGE'),

(9, 14, 62, 'HIGH',
 '[{"factor": "technical_complexity", "score": 20, "description": "Data model more complex than expected"}, {"factor": "dependency_risk", "score": 25, "description": "Notifications feature delayed"}, {"factor": "scope_uncertainty", "score": 17, "description": "Activity feed scope expanding"}]',
 '2026-01-07 17:00:00', 'SCHEDULED'),

(10, 14, 78, 'HIGH',
 '[{"factor": "technical_complexity", "score": 22, "description": "Threading model needs rethink"}, {"factor": "dependency_risk", "score": 28, "description": "Blocked on notifications integration"}, {"factor": "scope_uncertainty", "score": 28, "description": "Stakeholder requests adding scope"}]',
 '2026-01-09 17:00:00', 'SCHEDULED'),

-- Circuit breaker triggered
(11, 14, 85, 'CRITICAL',
 '[{"factor": "technical_complexity", "score": 25, "description": "Architecture refactor needed"}, {"factor": "dependency_risk", "score": 30, "description": "Hard dependency on delayed feature"}, {"factor": "scope_uncertainty", "score": 30, "description": "Scope creep confirmed"}]',
 '2026-01-10 11:00:00', 'CIRCUIT_BREAKER');

-- Risk history for completed pitches (Cycle 6) - showing full lifecycle
INSERT INTO pitch_risk_history (id, pitch_id, risk_score, risk_level, risk_factors_json, recorded_at, trigger_type) OVERRIDING SYSTEM VALUE
VALUES
-- Email Notification System (Pitch 15) - smooth delivery
(12, 15, 40, 'MEDIUM',
 '[{"factor": "integration_risk", "score": 20, "description": "SendGrid API integration"}, {"factor": "technical_complexity", "score": 12, "description": "Template engine"}, {"factor": "scope_uncertainty", "score": 8, "description": "Well-defined scope"}]',
 '2025-10-01 10:00:00', 'STATUS_CHANGE'),

(13, 15, 32, 'MEDIUM',
 '[{"factor": "integration_risk", "score": 15, "description": "SendGrid working in staging"}, {"factor": "technical_complexity", "score": 10, "description": "Templates implemented"}, {"factor": "scope_uncertainty", "score": 7, "description": "Scope stable"}]',
 '2025-10-15 17:00:00', 'SCHEDULED'),

(14, 15, 18, 'LOW',
 '[{"factor": "integration_risk", "score": 8, "description": "Delivery verified"}, {"factor": "technical_complexity", "score": 5, "description": "Feature complete"}, {"factor": "scope_uncertainty", "score": 5, "description": "Only polish left"}]',
 '2025-11-01 17:00:00', 'SCHEDULED'),

(15, 15, 5, 'MINIMAL',
 '[{"factor": "integration_risk", "score": 2, "description": "All tests passing"}, {"factor": "technical_complexity", "score": 2, "description": "Ready for release"}, {"factor": "scope_uncertainty", "score": 1, "description": "Complete"}]',
 '2025-11-10 10:00:00', 'STATUS_CHANGE'),

-- API Rate Limiting (Pitch 17) - had a spike due to bug
(16, 17, 35, 'MEDIUM',
 '[{"factor": "technical_complexity", "score": 18, "description": "Distributed rate limiting"}, {"factor": "performance_risk", "score": 12, "description": "Redis latency"}, {"factor": "scope_uncertainty", "score": 5, "description": "Clear requirements"}]',
 '2025-10-01 10:00:00', 'STATUS_CHANGE'),

(17, 17, 28, 'LOW',
 '[{"factor": "technical_complexity", "score": 12, "description": "Token bucket working"}, {"factor": "performance_risk", "score": 10, "description": "Redis performing well"}, {"factor": "scope_uncertainty", "score": 6, "description": "Admin UI descoped"}]',
 '2025-10-10 17:00:00', 'SCHEDULED'),

-- Risk spiked when race condition discovered
(18, 17, 58, 'MEDIUM',
 '[{"factor": "technical_complexity", "score": 25, "description": "Race condition in counter"}, {"factor": "performance_risk", "score": 20, "description": "Inconsistent limiting under load"}, {"factor": "scope_uncertainty", "score": 13, "description": "May need Lua scripts"}]',
 '2025-10-18 14:00:00', 'MANUAL'),

-- Fixed and back on track
(19, 17, 22, 'LOW',
 '[{"factor": "technical_complexity", "score": 8, "description": "Lua script fixed race condition"}, {"factor": "performance_risk", "score": 10, "description": "Performance verified"}, {"factor": "scope_uncertainty", "score": 4, "description": "Scope complete"}]',
 '2025-10-20 12:00:00', 'MANUAL'),

(20, 17, 8, 'MINIMAL',
 '[{"factor": "technical_complexity", "score": 3, "description": "Production ready"}, {"factor": "performance_risk", "score": 3, "description": "Load tested"}, {"factor": "scope_uncertainty", "score": 2, "description": "Complete"}]',
 '2025-10-20 17:00:00', 'STATUS_CHANGE');

-- =============================================================================
-- RISK FEEDBACK (user feedback on AI risk assessments)
-- This helps improve the risk prediction model
-- =============================================================================

-- Check if risk_feedback table exists, create if not
-- Note: This may need schema migration if table doesn't exist
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'risk_feedback') THEN
        INSERT INTO risk_feedback (pitch_id, user_id, original_risk_score, suggested_risk_score, rating, notes, created_at)
        VALUES
        -- Feedback on Pitch 14 (Collaboration Hub) - user agreed risk was high
        (14, 4, 62, 70, 'OVERLY_OPTIMISTIC', 
         'Dependency on notifications feature was underestimated. Real-time sync complexity also higher than predicted.',
         '2026-01-08 10:00:00'),

        -- Feedback on Pitch 12 (Search) - user thought risk was overestimated
        (12, 3, 52, 40, 'OVERLY_PESSIMISTIC',
         'Team has more Elasticsearch experience than the model assumed. Technical complexity was overweighted.',
         '2026-01-06 14:00:00'),

        -- Feedback on Pitch 11 (Notifications) - accurate prediction
        (11, 3, 45, 45, 'ACCURATE',
         'Initial assessment was spot-on. Good identification of WebSocket learning curve as main risk.',
         '2026-01-05 16:00:00'),

        -- Feedback on completed pitch - post-hoc analysis
        (17, 6, 35, 50, 'OVERLY_OPTIMISTIC',
         'Post-mortem: Race condition risk in distributed systems should have higher initial weight.',
         '2025-10-25 10:00:00'),

        (15, 2, 40, 35, 'OVERLY_PESSIMISTIC',
         'SendGrid integration was smoother than predicted. Their SDK is well-documented.',
         '2025-11-12 09:00:00');
    END IF;
END $$;

-- =============================================================================
-- HILL CHART HISTORY (tracking hill chart position changes)
-- Note: If hill_chart_history table exists, this tracks audit trail
-- =============================================================================

-- Check if table exists and insert history if it does
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'hill_chart_history') THEN
        -- History for "WebSocket Infrastructure" scope (Pitch 11)
        INSERT INTO hill_chart_history (hill_chart_point_id, pitch_id, previous_position, new_position, user_id, note, created_at)
        SELECT hcp.id, 11, 0, 30, 3, 'Initial positioning - figuring out STOMP configuration', '2026-01-06 10:00:00'
        FROM hill_chart_points hcp 
        WHERE hcp.pitch_id = 11 AND hcp.scope = 'WebSocket Infrastructure'
        LIMIT 1;

        INSERT INTO hill_chart_history (hill_chart_point_id, pitch_id, previous_position, new_position, user_id, note, created_at)
        SELECT hcp.id, 11, 30, 55, 3, 'Past the hill - architecture decision made, now implementing', '2026-01-07 14:00:00'
        FROM hill_chart_points hcp 
        WHERE hcp.pitch_id = 11 AND hcp.scope = 'WebSocket Infrastructure'
        LIMIT 1;

        INSERT INTO hill_chart_history (hill_chart_point_id, pitch_id, previous_position, new_position, user_id, note, created_at)
        SELECT hcp.id, 11, 55, 85, 3, 'Core server running, testing reconnection logic', '2026-01-09 16:00:00'
        FROM hill_chart_points hcp 
        WHERE hcp.pitch_id = 11 AND hcp.scope = 'WebSocket Infrastructure'
        LIMIT 1;

        -- History for "Search Index Setup" scope (Pitch 12)
        INSERT INTO hill_chart_history (hill_chart_point_id, pitch_id, previous_position, new_position, user_id, note, created_at)
        SELECT hcp.id, 12, 0, 40, 3, 'Learning Elasticsearch, setting up cluster', '2026-01-06 11:00:00'
        FROM hill_chart_points hcp 
        WHERE hcp.pitch_id = 12 AND hcp.scope = 'Search Index Setup'
        LIMIT 1;

        INSERT INTO hill_chart_history (hill_chart_point_id, pitch_id, previous_position, new_position, user_id, note, created_at)
        SELECT hcp.id, 12, 40, 70, 3, 'Index mappings defined, bulk import working', '2026-01-08 15:00:00'
        FROM hill_chart_points hcp 
        WHERE hcp.pitch_id = 12 AND hcp.scope = 'Search Index Setup'
        LIMIT 1;

        INSERT INTO hill_chart_history (hill_chart_point_id, pitch_id, previous_position, new_position, user_id, note, created_at)
        SELECT hcp.id, 12, 70, 90, 3, 'Real-time sync configured, final tuning', '2026-01-10 10:00:00'
        FROM hill_chart_points hcp 
        WHERE hcp.pitch_id = 12 AND hcp.scope = 'Search Index Setup'
        LIMIT 1;
    END IF;
END $$;

-- =============================================================================
-- EVIDENCE records for risk justification (if table exists)
-- =============================================================================

DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'evidences') THEN
        INSERT INTO evidences (pitch_id, person_id, date, description)
        VALUES
        -- Evidence for Pitch 14 circuit breaker
        (14, 5, '2026-01-09', 
         'BLOCKER: Integration with notifications feature blocked - WebSocket reconnection logic incomplete. Source: Daily standup 2026-01-09. Impact: HIGH'),
        
        (14, 4, '2026-01-08',
         'SCOPE_CHANGE: Product requested additional activity feed features - adds 3-4 days to scope. Source: Email from Carol 2026-01-08. Impact: MEDIUM'),
        
        (14, 5, '2026-01-10',
         'TECHNICAL_ISSUE: Comment threading model needs redesign - current approach doesnt scale. Source: Code review feedback. Impact: HIGH'),

        -- Evidence for Pitch 17 (Rate Limiting) bug discovery and fix
        (17, 6, '2025-10-18',
         'BUG_DISCOVERY: Race condition discovered in distributed counter under high load. Source: Load testing results. Impact: HIGH'),
        
        (17, 3, '2025-10-20',
         'MITIGATION: Implemented atomic Lua script for Redis counter operations. Source: PR #98 code review. Impact: HIGH');
    END IF;
END $$;

-- Add evidences for Cycle 6 retrospective action items
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'evidences') THEN
        INSERT INTO evidences (pitch_id, person_id, date, description)
        VALUES
        -- Evidence for retrospective action: Feature flags (Bob)
        (15, 3, '2025-11-16',
         'ACTION_COMPLETED: Feature flag infrastructure implemented using LaunchDarkly. Source: Cycle 6 retrospective action item. Allows safer releases with gradual rollout.'),
        
        -- Evidence for retrospective action: No-meeting mornings (Carol)
        (16, 4, '2025-11-18',
         'POLICY_CHANGE: No-meeting mornings policy approved by management. Monday-Friday 9AM-12PM reserved for deep work. Source: Cycle 6 retrospective action item.'),
        
        -- Evidence for retrospective action: ADR templates (David)
        (17, 5, '2025-11-17',
         'DOCUMENTATION: Architecture Decision Record (ADR) template created and added to repository. All major technical decisions now documented. Source: Cycle 6 retrospective action item.'),
        
        -- Evidence for retrospective action: Performance testing (Emma)
        (18, 6, '2025-11-19',
         'CI_IMPROVEMENT: Lighthouse CI integrated into build pipeline. Performance regression tests now automated. Source: Cycle 6 retrospective action item.');
    END IF;
END $$;

-- =============================================================================
-- Reset sequences
-- =============================================================================
SELECT setval(pg_get_serial_sequence('pitch_risk_history', 'id'), 100, false);

-- Conditionally reset other sequences if tables exist
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'risk_feedback') THEN
        PERFORM setval(pg_get_serial_sequence('risk_feedback', 'id'), 100, false);
    END IF;
    
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'hill_chart_history') THEN
        PERFORM setval(pg_get_serial_sequence('hill_chart_history', 'id'), 100, false);
    END IF;
    
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'evidences') THEN
        PERFORM setval(pg_get_serial_sequence('evidences', 'id'), 100, false);
    END IF;
END $$;
