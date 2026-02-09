-- ===========================================
-- Seed Data: Acted-On Retrospective Items
-- ===========================================
-- This seed file demonstrates the flexible retro action feature introduced in v0.5
-- which allows converting retro items to pitches, tasks, or just marking them as acted on.
-- 
-- Examples included:
-- 1. TRY_NEXT items converted to a pitch draft (Feature Flags & Design Docs)
-- 2. ACTIONS items converted to tasks (Performance Testing, ADR Template)
-- 3. ACTIONS items marked as acted on without creation (No-meeting policy proposal)

-- ===========================================
-- Update Retro Items with Acted-On Status
-- ===========================================

-- Items 10 & 13: Converted to pitch draft
UPDATE retro_items SET 
    acted_on = true,
    acted_on_notes = 'Converted to pitch draft: Infrastructure Improvements for Next Cycle',
    acted_on_at = '2025-11-14 16:15:00',
    acted_on_by_id = 2
WHERE id IN (10, 13);

-- Items 14 & 16: Converted to tasks
UPDATE retro_items SET 
    acted_on = true,
    acted_on_notes = 'Converted to task in Cycle 6',
    acted_on_at = '2025-11-14 16:20:00',
    acted_on_by_id = 3
WHERE id IN (14, 16);

-- Item 15: Just marked as acted on (proposal submitted to management)
UPDATE retro_items SET 
    acted_on = true,
    acted_on_notes = 'Policy proposal submitted to management for review. Awaiting decision on no-meeting mornings.',
    acted_on_at = '2025-11-14 16:25:00',
    acted_on_by_id = 4
WHERE id = 15;

-- Item 17: Just marked as acted on (working with existing template)
UPDATE retro_items SET 
    acted_on = true,
    acted_on_notes = 'Decided to use existing Architecture Decision Records (ADR) template from engineering wiki instead of creating new one.',
    acted_on_at = '2025-11-14 16:30:00',
    acted_on_by_id = 5
WHERE id = 17;

-- ===========================================
-- Pitch Draft Converted from Retro Items
-- ===========================================

INSERT INTO pitches (id, title, description, appetite_days, problem_statement, solution, rabbit_holes, no_gos, status, cycle_id, team_id, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(19, 
 'Infrastructure Improvements for Safer & More Thoughtful Development',
 'Auto-generated from retrospective: Test Retro',
 14,
 'Based on our Cycle 6 retrospective, we identified two critical gaps in our development process: lack of feature flags for safer deployments and insufficient technical design documentation before implementation. These gaps led to risky deployments and scope creep in our audit logging feature.',
 'Implement a lightweight feature flag system using a simple key-value configuration approach with environment-specific overrides. Additionally, establish a streamlined technical design document (TDD) template that teams fill out before starting medium/large features. The TDD should capture API contracts, data models, and key architectural decisions without being overly bureaucratic.',
 'Avoid building a complex feature flag management UI in this cycle - start with config files and environment variables. Don''t try to retrofit existing features with feature flags; focus on making it available for new work.',
 'No runtime feature flag editing through admin UI (that can come later if needed). No integration with third-party feature flag services yet - keep it simple and in-house first.',
 'PENDING',
 6,
 6,
 '2025-11-14 16:15:00',
 '2025-11-14 16:15:00');

-- ===========================================
-- Tasks Converted from Retro Items
-- ===========================================

-- Task from item 14: Set up feature flag infrastructure
INSERT INTO tasks (id, title, description, status, priority, assignee_id, cycle_id, created_by_id, created_at, updated_at, due_date) OVERRIDING SYSTEM VALUE VALUES
(50,
 'Set up Feature Flag Infrastructure',
 'Implement basic feature flag system based on retro action item. Create FeatureFlagService with configuration support for enabling/disabling features per environment. Add documentation for developers on how to use feature flags in their code.

Acceptance Criteria:
- FeatureFlagService class created with isEnabled(flagName) method
- Configuration file support (application.properties)
- Unit tests for FeatureFlagService
- Developer documentation with examples
- At least one example feature using the new flag system',
 'TODO',
 'HIGH',
 3,
 6,
 3,
 '2025-11-14 16:20:00',
 '2025-11-14 16:20:00',
 '2025-11-21 23:59:59');

-- Task from item 16: Integrate performance tests into CI pipeline
INSERT INTO tasks (id, title, description, status, priority, assignee_id, cycle_id, created_by_id, created_at, updated_at, due_date) OVERRIDING SYSTEM VALUE VALUES
(51,
 'Integrate Performance Tests into CI Pipeline',
 'Add performance testing to our CI/CD pipeline to catch performance regressions earlier in the development cycle. Based on retrospective feedback that dashboard performance issues were discovered too late.

Acceptance Criteria:
- JMeter or Gatling performance test suite created
- CI pipeline updated to run performance tests on staging deployments
- Performance baseline metrics documented
- Alert thresholds configured for regression detection
- Test reports published to CI job artifacts',
 'TODO',
 'HIGH',
 6,
 6,
 3,
 '2025-11-14 16:20:00',
 '2025-11-14 16:20:00',
 '2025-11-28 23:59:59');

-- ===========================================
-- Comments on Task Items (for context)
-- ===========================================
-- Note: Comments on pitches are not supported by the current schema constraint

-- Comment on task 50
INSERT INTO comments (id, content, entity_type, entity_id, author_id, is_edited, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1000,
 'This task originated from retrospective action item #14. Bob volunteered to own this during the retro discussion.',
 'TASK',
 50,
 3,
 false,
 '2025-11-14 16:22:00',
 NULL);

-- Comment on task 51
INSERT INTO comments (id, content, entity_type, entity_id, author_id, is_edited, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1001,
 'Created from retrospective action #16. Emma has experience with performance testing from her previous project.',
 'TASK',
 51,
 3,
 false,
 '2025-11-14 16:23:00',
 NULL);
