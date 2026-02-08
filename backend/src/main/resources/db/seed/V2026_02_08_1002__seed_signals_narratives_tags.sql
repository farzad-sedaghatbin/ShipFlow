-- =============================================================================
-- V2026_02_08_1002: Seed Signals, Narratives, and Tags
-- =============================================================================
-- This seed data demonstrates:
-- 1. Tags for organizing and correlating retro items and pitches
-- 2. Cycle signal cache for health dashboard
-- 3. Cycle narratives (template-generated summaries)
-- 4. Retro items with acted_on follow-through tracking
-- =============================================================================

-- =============================================================================
-- TAGS for Project 1 (Customer Portal)
-- =============================================================================

INSERT INTO tags (name, project_id, color, description, created_at, created_by_id)
VALUES
('tech-debt', 1, '#ef4444', 'Technical debt that needs addressing', CURRENT_TIMESTAMP, 3),
('performance', 1, '#f59e0b', 'Performance-related items and improvements', CURRENT_TIMESTAMP, 3),
('security', 1, '#dc2626', 'Security concerns and improvements', CURRENT_TIMESTAMP, 3),
('ux-improvement', 1, '#8b5cf6', 'User experience enhancements', CURRENT_TIMESTAMP, 4),
('documentation', 1, '#06b6d4', 'Documentation needs and updates', CURRENT_TIMESTAMP, 5),
('process', 1, '#10b981', 'Process improvements and workflow changes', CURRENT_TIMESTAMP, 2),
('testing', 1, '#3b82f6', 'Testing and QA related items', CURRENT_TIMESTAMP, 6),
('infrastructure', 1, '#6366f1', 'Infrastructure and DevOps items', CURRENT_TIMESTAMP, 5),
('feature-request', 1, '#ec4899', 'New feature requests from users', CURRENT_TIMESTAMP, 4),
('bug-pattern', 1, '#f43f5e', 'Recurring bug patterns to address', CURRENT_TIMESTAMP, 6);

-- Tags for Project 2 (Mobile App)
INSERT INTO tags (name, project_id, color, description, created_at, created_by_id)
VALUES
('ios', 2, '#64748b', 'iOS-specific items', CURRENT_TIMESTAMP, 2),
('android', 2, '#22c55e', 'Android-specific items', CURRENT_TIMESTAMP, 2),
('offline-mode', 2, '#0ea5e9', 'Offline functionality', CURRENT_TIMESTAMP, 5),
('push-notifications', 2, '#a855f7', 'Push notification related', CURRENT_TIMESTAMP, 3);

-- =============================================================================
-- LINK TAGS TO RETRO ITEMS (from Cycle 6 retrospective)
-- =============================================================================

-- "Audit logging feature scope creep" (item 6) -> process
INSERT INTO retro_item_tags (retro_item_id, tag_id) 
SELECT 6, id FROM tags WHERE name = 'process' AND project_id = 1;

-- "Lack of documentation slowed down onboarding" (item 7) -> documentation
INSERT INTO retro_item_tags (retro_item_id, tag_id) 
SELECT 7, id FROM tags WHERE name = 'documentation' AND project_id = 1;

-- "Dashboard performance issues discovered late" (item 8) -> performance, testing
INSERT INTO retro_item_tags (retro_item_id, tag_id) 
SELECT 8, id FROM tags WHERE name = 'performance' AND project_id = 1;
INSERT INTO retro_item_tags (retro_item_id, tag_id) 
SELECT 8, id FROM tags WHERE name = 'testing' AND project_id = 1;

-- "Too many meetings interrupted deep work" (item 9) -> process
INSERT INTO retro_item_tags (retro_item_id, tag_id) 
SELECT 9, id FROM tags WHERE name = 'process' AND project_id = 1;

-- "Implement feature flags for safer deployments" (item 10) -> infrastructure
INSERT INTO retro_item_tags (retro_item_id, tag_id) 
SELECT 10, id FROM tags WHERE name = 'infrastructure' AND project_id = 1;

-- "Add performance testing earlier in the cycle" (item 12) -> testing, performance
INSERT INTO retro_item_tags (retro_item_id, tag_id) 
SELECT 12, id FROM tags WHERE name = 'testing' AND project_id = 1;
INSERT INTO retro_item_tags (retro_item_id, tag_id) 
SELECT 12, id FROM tags WHERE name = 'performance' AND project_id = 1;

-- "Create technical design docs before implementation" (item 13) -> documentation, process
INSERT INTO retro_item_tags (retro_item_id, tag_id) 
SELECT 13, id FROM tags WHERE name = 'documentation' AND project_id = 1;
INSERT INTO retro_item_tags (retro_item_id, tag_id) 
SELECT 13, id FROM tags WHERE name = 'process' AND project_id = 1;

-- =============================================================================
-- LINK TAGS TO PITCHES (for Cycle 4 - SHAPING)
-- =============================================================================

-- Password Reset Flow (Pitch 1) -> security
INSERT INTO pitch_tags (pitch_id, tag_id) 
SELECT 1, id FROM tags WHERE name = 'security' AND project_id = 1;

-- Two-Factor Authentication (Pitch 4) -> security
INSERT INTO pitch_tags (pitch_id, tag_id) 
SELECT 4, id FROM tags WHERE name = 'security' AND project_id = 1;

-- User Profile Settings (Pitch 2) -> ux-improvement
INSERT INTO pitch_tags (pitch_id, tag_id) 
SELECT 2, id FROM tags WHERE name = 'ux-improvement' AND project_id = 1;

-- Activity History Timeline (Pitch 5) -> ux-improvement
INSERT INTO pitch_tags (pitch_id, tag_id) 
SELECT 5, id FROM tags WHERE name = 'ux-improvement' AND project_id = 1;

-- Custom Dashboards (Pitch 7) -> ux-improvement, feature-request
INSERT INTO pitch_tags (pitch_id, tag_id) 
SELECT 7, id FROM tags WHERE name = 'ux-improvement' AND project_id = 1;
INSERT INTO pitch_tags (pitch_id, tag_id) 
SELECT 7, id FROM tags WHERE name = 'feature-request' AND project_id = 1;

-- =============================================================================
-- CYCLE SIGNAL CACHE for Health Dashboard
-- =============================================================================

-- Appetite Accuracy signal for Project 1
INSERT INTO cycle_signal_cache (project_id, signal_type, signal_data, cycles_included, calculated_at, valid_until)
VALUES
(1, 'APPETITE_ACCURACY', 
 '{"overall_accuracy": 78, "on_time_count": 7, "total_count": 9, "average_overrun_days": 2.3, "trend": "IMPROVING", "by_cycle": [{"cycle_id": 5, "accuracy": 75}, {"cycle_id": 6, "accuracy": 82}], "recommendations": ["Consider adding buffer for backend-heavy pitches", "Review estimation process for integration work"]}',
 '5,6', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days'),

-- Shaping Pattern signal
(1, 'SHAPING_PATTERN',
 '{"avg_rabbit_holes_identified": 3.2, "avg_risks_identified": 4.1, "common_missing_elements": ["migration_strategy", "rollback_plan"], "well_shaped_percentage": 65, "needs_more_shaping_percentage": 25, "poorly_shaped_percentage": 10, "trend": "STABLE", "top_shaping_issues": ["Scope ambiguity in acceptance criteria", "Missing technical constraints"]}',
 '4,5,6', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days'),

-- Risk Correlation signal  
(1, 'RISK_CORRELATION',
 '{"high_risk_indicators": ["scope_creep_mentioned_in_retro", "estimate_exceeded_appetite", "late_hill_chart_movement"], "correlation_strength": 0.72, "predictive_accuracy": 68, "common_risk_factors": [{"factor": "integration_work", "impact_score": 8.5}, {"factor": "new_technology", "impact_score": 7.2}, {"factor": "unclear_requirements", "impact_score": 6.8}], "trend": "IMPROVING"}',
 '5,6', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days'),

-- Retro Follow-Through signal
(1, 'RETRO_FOLLOW_THROUGH',
 '{"total_action_items": 12, "completed_count": 8, "in_progress_count": 2, "not_started_count": 2, "completion_rate": 67, "avg_completion_days": 14.5, "most_successful_categories": ["process", "documentation"], "struggling_categories": ["infrastructure"], "trend": "STABLE", "recommendations": ["Assign owners immediately in retro", "Set specific deadlines for action items"]}',
 '5,6', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days');

-- =============================================================================
-- CYCLE NARRATIVES for Cycle 6 (completed)
-- =============================================================================

INSERT INTO cycle_narratives (cycle_id, narrative_type, content, is_ai_generated, generated_at, generated_by_id, created_at)
VALUES
-- What We Bet narrative
(6, 'WHAT_WE_BET', 
 '## What We Bet On in Cycle 6

This cycle, we made **4 strategic bets** focused on improving our technical foundation and user experience:

### 🎯 Big Bets
1. **Email Notification System** (14 days appetite) - A comprehensive notification system that our users have been requesting for months. This was our highest-confidence bet given the clear requirements and experienced team.

2. **Audit Logging System** (14 days appetite) - Essential for enterprise compliance. While complex, we had a clear technical approach.

### 🎲 Calculated Risks  
3. **User Dashboard Redesign** (10 days appetite) - A significant UX improvement. We bet on Carol''s designs being implementation-ready.

4. **API Rate Limiting** (7 days appetite) - Technical infrastructure work. Small appetite but high impact for system stability.

### What We Said No To
We deferred several pitches including the billing integration and multi-tenant support, choosing to focus on these foundational features first.',
 false, '2025-11-14 10:00:00', 2, '2025-11-14 10:00:00'),

-- What Shipped narrative
(6, 'WHAT_SHIPPED',
 '## What Shipped in Cycle 6 ✅

All 4 pitches shipped successfully! Here''s what we delivered:

### Email Notification System
- SendGrid integration with template support
- User preference management
- Email analytics with open/click tracking
- Background job queue for reliable delivery

### User Dashboard Redesign
- New responsive grid layout
- Customizable widget system
- Lazy loading for better performance
- Dark mode support (bonus!)

### API Rate Limiting
- Token bucket algorithm implementation
- Redis-backed distributed limiting
- Admin dashboard for monitoring
- Per-endpoint configuration

### Audit Logging System
- Comprehensive event capture
- Efficient storage with proper indexing
- Advanced search and filtering UI
- Export and reporting capabilities

**Total shipped:** 4/4 pitches (100%)',
 false, '2025-11-14 10:30:00', 2, '2025-11-14 10:30:00'),

-- What We Cut narrative
(6, 'WHAT_WE_CUT',
 '## What We Cut or Reduced 🔪

To ship on time, we made these scope decisions:

### Intentionally Cut
- **Email scheduling UI** - We shipped the backend support but deferred the calendar UI to next cycle
- **Audit log advanced analytics** - Basic reporting shipped; predictive analytics deferred
- **Dashboard drag-and-drop reordering** - Shipped with fixed positions; reordering coming in Cycle 7

### Reduced Scope
- **Rate limiting dashboard** - Shipped monitoring only; configuration UI simplified
- **Email template editor** - Used markdown instead of full WYSIWYG editor

### Why We Cut
All cuts were discussed in hill chart reviews. We prioritized core functionality over nice-to-haves, staying true to our appetite commitments.',
 false, '2025-11-14 11:00:00', 2, '2025-11-14 11:00:00'),

-- Surprises narrative
(6, 'SURPRISES',
 '## Surprises & Learnings 🎁

### Pleasant Surprises
- **Dark mode shipped as bonus** - The new CSS architecture made it trivial to add
- **Email delivery speed** - SendGrid''s edge network gave us sub-second delivery
- **Team velocity** - New pair programming practice improved throughput by ~20%

### Challenges We Didn''t Expect
- **Redis connection pooling** - Had to tune pool settings for high-load scenarios
- **Dashboard memory usage** - Initial widget implementation leaked memory; required refactor
- **Audit log volume** - Generated 10x more data than estimated; added compression

### What We''d Do Differently
1. Add performance testing earlier in cycle
2. Prototype integration points before committing
3. Include buffer time for infrastructure features

### Team Kudos
- Bob for heroic Redis debugging session
- Carol for design system that enabled dark mode
- Emma for catching the memory leak early',
 false, '2025-11-14 11:30:00', 2, '2025-11-14 11:30:00'),

-- Full Summary narrative
(6, 'FULL_SUMMARY',
 '## Cycle 6 Summary: A Successful Foundation Sprint

### Overview
Cycle 6 ran from **Oct 1 - Nov 15, 2025** with teams Omega and Sigma delivering all 4 committed pitches. This was a foundation-building cycle focused on security, observability, and UX improvements.

### Key Metrics
- **Delivery Rate:** 100% (4/4 pitches)
- **Appetite Accuracy:** 82%
- **Team Velocity:** 340 story points
- **Bug Escape Rate:** 2 minor issues

### Strategic Outcomes
✅ Security compliance ready for enterprise sales
✅ User experience significantly improved
✅ Observability foundation for future features
✅ Team practices (pair programming) validated

### Action Items from Retro
1. Bob: Feature flag infrastructure *(in progress)*
2. Carol: No-meeting mornings policy *(completed)*
3. Emma: Performance tests in CI *(in progress)*
4. David: ADR template *(completed)*

### Looking Ahead
Cycle 7 will build on this foundation with mobile-focused features and the deferred billing integration.',
 false, '2025-11-14 12:00:00', 2, '2025-11-14 12:00:00');

-- =============================================================================
-- UPDATE RETRO ITEMS with acted_on tracking (demonstrating follow-through)
-- =============================================================================

-- "Implement feature flags for safer deployments" (item 10) - acted on
UPDATE retro_items 
SET acted_on = true, 
    acted_on_notes = 'Bob set up LaunchDarkly integration. Now using feature flags for all new features.',
    acted_on_at = '2025-12-01 14:00:00',
    acted_on_by_id = 3
WHERE id = 10;

-- "Schedule focus blocks - no meetings during morning hours" (item 11) - acted on
UPDATE retro_items 
SET acted_on = true, 
    acted_on_notes = 'Carol got management approval. Team now has meeting-free mornings Mon-Thu.',
    acted_on_at = '2025-11-20 10:00:00',
    acted_on_by_id = 4
WHERE id = 11;

-- "Add performance testing earlier in the cycle" (item 12) - in progress, not fully acted on
UPDATE retro_items 
SET acted_on = false, 
    acted_on_notes = 'Emma added Lighthouse CI but still working on load testing integration.',
    acted_on_at = NULL,
    acted_on_by_id = 6
WHERE id = 12;

-- "Create technical design docs before implementation" (item 13) - acted on
UPDATE retro_items 
SET acted_on = true, 
    acted_on_notes = 'David created ADR template. Team now writes ADRs for all pitches during shaping.',
    acted_on_at = '2025-11-25 16:00:00',
    acted_on_by_id = 5
WHERE id = 13;

-- ACTION items - mark some as acted on
-- "ACTION: Bob to set up feature flag infrastructure" (item 14)
UPDATE retro_items 
SET acted_on = true, 
    acted_on_notes = 'LaunchDarkly configured. First feature flag (dashboard-v2) deployed.',
    acted_on_at = '2025-12-01 14:00:00',
    acted_on_by_id = 3
WHERE id = 14;

-- "ACTION: Carol to propose no-meeting mornings" (item 15)
UPDATE retro_items 
SET acted_on = true, 
    acted_on_notes = 'Policy approved and implemented. Positive feedback from team.',
    acted_on_at = '2025-11-20 10:00:00',
    acted_on_by_id = 4
WHERE id = 15;

-- =============================================================================
-- Reset sequences
-- =============================================================================
SELECT setval(pg_get_serial_sequence('tags', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('cycle_signal_cache', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('cycle_narratives', 'id'), 100, false);
