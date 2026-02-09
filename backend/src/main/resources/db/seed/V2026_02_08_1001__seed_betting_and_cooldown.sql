-- =============================================================================
-- V2026_02_08_1001: Seed Betting Decisions and Cooldown Activities
-- =============================================================================
-- This seed data demonstrates:
-- 1. Betting decisions with commit, reject, defer, and needs-shaping outcomes
-- 2. Cooldown activities showing various activity types and statuses
-- =============================================================================

-- =============================================================================
-- BETTING DECISIONS for Cycle 4 (SHAPING phase) pitches
-- =============================================================================

-- Committed pitches (these get teams assigned)
INSERT INTO betting_decisions (pitch_id, cycle_id, decision, reason, decided_by_id, decided_at, 
    requested_appetite_days, available_capacity_days, appetite_comparison_notes, 
    considered_team_id, priority_score, strategic_alignment_notes)
VALUES
-- Password Reset Flow (Pitch 1) - COMMITTED to Alpha Team
(1, 4, 'COMMITTED', 
 'Critical security requirement. Users have been requesting this for months. Team has the backend expertise needed for secure token handling.',
 2, '2025-12-05 14:00:00', 7, 14, 'Small batch - fits well within team capacity with room for unexpected issues',
 1, 95, 'Directly supports Q1 security compliance goals'),

-- Two-Factor Authentication (Pitch 4) - COMMITTED to Alpha Team  
(4, 4, 'COMMITTED',
 'Security roadmap priority. Complements the password reset work. Same team can share context on authentication flows.',
 2, '2025-12-05 14:15:00', 14, 14, 'Uses remaining Alpha Team capacity for the cycle',
 1, 90, 'Required for enterprise customer compliance certifications'),

-- Offline Mode (Pitch 6) - COMMITTED to Beta Team
(6, 4, 'COMMITTED',
 'Top customer request. Field sales team loses data when traveling. High business impact.',
 4, '2025-12-05 14:30:00', 14, 21, 'Good fit - team has mobile expertise and appetite leaves buffer',
 2, 88, 'Supports mobile-first strategy for 2026'),

-- Rejected pitches
-- Billing & Payments Integration (Pitch 8) - REJECTED
(8, 4, 'REJECTED',
 'Too risky for current cycle. Need dedicated security review first. Also, appetite seems underestimated - Stripe integration typically takes longer.',
 2, '2025-12-05 15:00:00', 21, 21, 'Appetite matches capacity but no buffer for compliance requirements',
 NULL, 70, 'Important but not urgent - no active enterprise deals pending'),

-- Multi-tenant Support (Pitch 9) - REJECTED
(9, 4, 'REJECTED',
 'Massive architectural change. Needs more shaping - current pitch lacks migration strategy for existing customers.',
 4, '2025-12-05 15:15:00', 28, 21, 'Exceeds single team capacity - would need coordination across teams',
 NULL, 85, 'Strategic priority but execution risk too high without more shaping'),

-- Deferred pitches  
-- User Profile Settings (Pitch 2) - DEFERRED
(2, 4, 'DEFERRED',
 'Good pitch but not urgent. Deferring to next cycle when we have designer availability for proper UX work.',
 4, '2025-12-05 15:30:00', 5, 14, 'Small batch - easy to fit but designer Carol committed elsewhere',
 3, 60, 'Nice to have - improves user experience but not blocking anything'),

-- Custom Dashboards (Pitch 7) - DEFERRED
(7, 4, 'DEFERRED',
 'Interesting feature but analytics shows low usage of current dashboard. Deferring until we validate user need.',
 2, '2025-12-05 15:45:00', 14, 14, 'Capacity available but questioning ROI',
 NULL, 55, 'Exploring - need more customer interviews first'),

-- Needs more shaping
-- Biometric Login (Pitch 3) - NEEDS_SHAPING
(3, 4, 'NEEDS_SHAPING',
 'Exciting feature but pitch lacks technical details. How do we handle devices without biometric hardware? Fallback flow unclear.',
 4, '2025-12-05 16:00:00', 5, 14, 'Appetite seems too low for the complexity involved',
 NULL, 75, 'Good strategic fit for mobile-first vision'),

-- Workflow Automation (Pitch 10) - NEEDS_SHAPING
(10, 4, 'NEEDS_SHAPING',
 'Love the vision but scope is too broad. Need to identify a smaller first version. What are the top 3 workflows users want automated?',
 2, '2025-12-05 16:15:00', 28, 21, 'Way over capacity without scope reduction',
 NULL, 80, 'High potential for differentiation but needs focus'),

-- Activity History Timeline (Pitch 5) - NEEDS_SHAPING
(5, 4, 'NEEDS_SHAPING',
 'Useful feature but unclear what "activity" includes. Login history only? All actions? Need to define boundaries.',
 4, '2025-12-05 16:30:00', 10, 14, 'Capacity available but scope ambiguity is concerning',
 2, 65, 'Supports transparency goals but lower priority than security');

-- Update pitches with team assignments for committed decisions
UPDATE pitches SET team_id = 1 WHERE id IN (1, 4);
UPDATE pitches SET team_id = 2 WHERE id = 6;

-- =============================================================================
-- COOLDOWN ACTIVITIES for Cycle 6 (active COOLDOWN phase for demo)
-- =============================================================================

INSERT INTO cooldown_activities (cycle_id, activity_type, title, description, status, 
    assignee_id, created_by_id, estimated_hours, actual_hours, priority, related_pitch_id, 
    notes, started_at, completed_at, created_at, updated_at)
VALUES
-- Tech Debt items
(6, 'TECH_DEBT', 'Refactor email template rendering', 
 'Email templates have duplicated code. Extract common components and create a template engine.',
 'COMPLETED', 3, 3, 8, 10, 2, 15,
 'Technical debt from rapid Email Notification implementation. Now more maintainable.',
 '2025-11-15 09:00:00', '2025-11-18 16:00:00', '2025-11-13 10:00:00', '2025-11-18 16:00:00'),

(6, 'TECH_DEBT', 'Clean up unused API endpoints', 
 'Several deprecated endpoints still in codebase. Remove and update API documentation.',
 'COMPLETED', 5, 3, 4, 3, 3, NULL,
 'Cleaned up 12 deprecated endpoints. API surface is now cleaner.',
 '2025-11-16 10:00:00', '2025-11-16 15:00:00', '2025-11-13 10:15:00', '2025-11-16 15:00:00'),

-- Bug fixes
(6, 'BUG_FIX', 'Fix dashboard widget overlap on mobile', 
 'Dashboard widgets overlap on screens smaller than 768px. CSS grid issue.',
 'COMPLETED', 2, 6, 3, 2, 1, 16,
 'Quick CSS fix - added appropriate breakpoints.',
 '2025-11-15 14:00:00', '2025-11-15 16:00:00', '2025-11-13 10:30:00', '2025-11-15 16:00:00'),

(6, 'BUG_FIX', 'Rate limiter memory leak', 
 'Rate limiter not releasing old entries. Memory grows over time.',
 'COMPLETED', 3, 6, 6, 8, 1, 17,
 'Added TTL to Redis keys. Memory now stable under load.',
 '2025-11-17 09:00:00', '2025-11-18 14:00:00', '2025-11-13 10:45:00', '2025-11-18 14:00:00'),

-- Documentation
(6, 'DOCUMENTATION', 'Update API documentation for v2.0', 
 'API docs outdated after Cycle 6 features. Update OpenAPI spec and examples.',
 'COMPLETED', 5, 4, 6, 5, 2, NULL,
 'Swagger docs now reflect all new endpoints with examples.',
 '2025-11-19 09:00:00', '2025-11-19 17:00:00', '2025-11-13 11:00:00', '2025-11-19 17:00:00'),

-- Learning / Research
(6, 'LEARNING', 'Research GraphQL for future API', 
 'Evaluate GraphQL as alternative to REST for complex dashboard queries.',
 'COMPLETED', 5, 5, 8, 10, 4, NULL,
 'Wrote evaluation document. Recommendation: adopt for dashboard API in Q2.',
 '2025-11-20 09:00:00', '2025-11-22 16:00:00', '2025-11-13 11:15:00', '2025-11-22 16:00:00'),

-- Infrastructure
(6, 'INFRASTRUCTURE', 'Set up staging environment', 
 'Create staging environment matching production for safer deployments.',
 'COMPLETED', 5, 2, 12, 14, 2, NULL,
 'Staging now mirrors prod. Added to deployment pipeline.',
 '2025-11-15 09:00:00', '2025-11-20 17:00:00', '2025-11-13 11:30:00', '2025-11-20 17:00:00'),

-- QA activities
(6, 'QA', 'End-to-end test suite for email system', 
 'Create comprehensive E2E tests for email notification feature.',
 'COMPLETED', 6, 6, 10, 12, 2, 15,
 '25 new E2E tests added. Coverage increased to 85%.',
 '2025-11-16 09:00:00', '2025-11-19 16:00:00', '2025-11-13 11:45:00', '2025-11-19 16:00:00'),

-- Kickoff prep for next cycle
(6, 'KICKOFF_PREP', 'Prepare Cycle 7 pitch deck', 
 'Review backlog, prioritize pitches, prepare materials for betting ceremony.',
 'COMPLETED', 4, 4, 6, 5, 1, NULL,
 'Deck ready with 8 candidate pitches. Scheduled betting for Dec 1.',
 '2025-11-21 09:00:00', '2025-11-22 14:00:00', '2025-11-13 12:00:00', '2025-11-22 14:00:00'),

-- Refactoring
(6, 'REFACTORING', 'Extract audit logging to separate module', 
 'Audit logging code spread across services. Consolidate into dedicated module.',
 'COMPLETED', 5, 5, 8, 9, 3, 18,
 'Created audit-core module. All services now use consistent logging.',
 '2025-11-18 09:00:00', '2025-11-20 16:00:00', '2025-11-13 12:15:00', '2025-11-20 16:00:00'),

-- Planning for next cycle
(6, 'PLANNING', 'Define Q1 2026 technical roadmap', 
 'Collaborate with product to align technical initiatives with business goals.',
 'COMPLETED', 2, 2, 4, 4, 2, NULL,
 'Roadmap approved. Focus areas: performance, mobile, and security.',
 '2025-11-21 14:00:00', '2025-11-22 12:00:00', '2025-11-13 12:30:00', '2025-11-22 12:00:00'),

-- Research
(6, 'RESEARCH', 'Evaluate authentication providers', 
 'Compare Auth0, Okta, and Keycloak for potential 2FA implementation.',
 'COMPLETED', 3, 3, 6, 8, 3, NULL,
 'Recommendation: Auth0 for SaaS, Keycloak for on-premise option.',
 '2025-11-15 09:00:00', '2025-11-17 16:00:00', '2025-11-13 12:45:00', '2025-11-17 16:00:00'),

-- One activity still in progress (for current cooldown demo)
(6, 'OTHER', 'Team retrospective action items follow-up', 
 'Track and verify completion of action items from Cycle 6 retrospective.',
 'IN_PROGRESS', 4, 4, 2, NULL, 2, NULL,
 'Tracking 4 action items. 2 completed, 2 in progress.',
 '2025-11-22 09:00:00', NULL, '2025-11-13 13:00:00', CURRENT_TIMESTAMP);

-- =============================================================================
-- Reset sequences to avoid conflicts
-- =============================================================================
SELECT setval(pg_get_serial_sequence('betting_decisions', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('cooldown_activities', 'id'), 100, false);
