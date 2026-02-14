-- =============================================================================
-- V2026_02_14_0002: Seed Roadmap Features (Initiatives, Epics, Releases)
-- =============================================================================
-- This seed data demonstrates the Initiative → Epic → Pitch hierarchy
-- and release management for roadmap planning and stakeholder communication.
-- =============================================================================

-- =============================================================================
-- INITIATIVES - Strategic themes spanning multiple quarters
-- =============================================================================
INSERT INTO initiatives (
    name, description, status, target_start_date, target_end_date,
    color, sort_order, owner_id, project_id,
    created_at, updated_at
) VALUES
-- Mobile Experience Initiative (Q1-Q2 2026)
(
    'Mobile Experience 2026',
    'Comprehensive mobile app overhaul to improve user engagement and retention on iOS and Android platforms',
    'IN_PROGRESS',
    '2026-01-01', '2026-06-30',
    '#3498db', 1, 2, 2,
    CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '5 days'
),
-- Customer Portal Enhancement (Q2-Q3 2026)
(
    'Customer Portal Modernization',
    'Modernize customer-facing portal with improved UX, performance, and self-service capabilities',
    'PLANNED',
    '2026-04-01', '2026-09-30',
    '#2ecc71', 2, 2, 1,
    CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '3 days'
),
-- Internal Tools (Q1 2026)
(
    'Developer Productivity Tools',
    'Build internal tools to improve developer workflow and reduce manual tasks',
    'IN_PROGRESS',
    '2026-01-15', '2026-03-31',
    '#e74c3c', 3, 4, 3,
    CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP - INTERVAL '2 days'
),
-- Platform Stability (Ongoing)
(
    'Platform Stability & Performance',
    'Ongoing efforts to improve system reliability, performance monitoring, and infrastructure resilience',
    'IN_PROGRESS',
    '2026-01-01', '2026-12-31',
    '#f39c12', 4, 5, 1,
    CURRENT_TIMESTAMP - INTERVAL '35 days', CURRENT_TIMESTAMP - INTERVAL '1 day'
);

-- =============================================================================
-- EPICS - Large feature groups organizing related pitches
-- =============================================================================
INSERT INTO epics (
    name, description, status, target_start_date, target_end_date,
    color, sort_order, initiative_id, project_id,
    created_at, updated_at
) VALUES
-- Epics for Mobile Experience Initiative
(
    'Mobile Checkout Redesign',
    'Streamlined checkout flow for mobile with Apple Pay and Google Pay integration',
    'IN_PROGRESS',
    '2026-02-01', '2026-04-30',
    '#3498db', 1, 1, 2,
    CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '2 days'
),
(
    'Mobile Offline Support',
    'Enable core features to work offline with background sync when connection restored',
    'PLANNED',
    '2026-05-01', '2026-06-30',
    '#3498db', 2, 1, 2,
    CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '1 day'
),
-- Epics for Customer Portal Modernization
(
    'Self-Service Account Management',
    'Allow customers to manage subscriptions, billing, and profile settings without support',
    'PLANNED',
    '2026-04-01', '2026-06-30',
    '#2ecc71', 1, 2, 1,
    CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '1 day'
),
(
    'Advanced Analytics Dashboard',
    'Real-time analytics and insights for customer usage patterns and trends',
    'DRAFT',
    '2026-07-01', '2026-09-30',
    '#2ecc71', 2, 2, 1,
    CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP
),
-- Epics for Developer Productivity
(
    'CI/CD Pipeline Improvements',
    'Faster builds, better test reporting, and automated deployment workflows',
    'IN_PROGRESS',
    '2026-01-15', '2026-03-15',
    '#e74c3c', 1, 3, 3,
    CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '3 days'
),
-- Independent Epic (no parent initiative)
(
    'Security & Compliance Hardening',
    'SOC 2 compliance preparation and security audit remediation',
    'IN_PROGRESS',
    '2026-02-01', '2026-05-31',
    '#9b59b6', 1, NULL, 1,
    CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '2 days'
);

-- =============================================================================
-- RELEASES - Versioned delivery milestones
-- =============================================================================
INSERT INTO releases (
    version, name, description, status, risk_level,
    target_date, release_date, release_notes,
    project_id, created_at, updated_at
) VALUES
-- Released versions
(
    'v2.3.0',
    'Winter 2026 Release',
    'Mobile app improvements and performance optimizations',
    'RELEASED',
    'LOW',
    '2026-01-15', '2026-01-18',
    '- Improved mobile checkout flow\n- Fixed critical performance issues\n- Updated dependencies\n- Enhanced error handling',
    2,
    CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '26 days'
),
-- In staging
(
    'v2.4.0',
    'Q1 2026 Feature Release',
    'Customer portal enhancements and developer tools',
    'STAGING',
    'MEDIUM',
    '2026-02-28', NULL,
    '- New self-service account features\n- CI/CD improvements\n- Bug fixes and stability improvements',
    1,
    CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '1 day'
),
-- In progress
(
    'v2.5.0',
    'Spring 2026 Release',
    'Mobile offline support and analytics dashboard',
    'IN_PROGRESS',
    'HIGH',
    '2026-04-30', NULL,
    NULL,
    2,
    CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP
),
(
    'v2.6.0',
    'Q2 2026 Major Release',
    'Platform stability improvements and security hardening',
    'IN_PROGRESS',
    'CRITICAL',
    '2026-05-31', NULL,
    NULL,
    1,
    CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP
),
-- Planned
(
    'v3.0.0',
    'Summer 2026 Platform Update',
    'Major platform overhaul with breaking changes',
    'PLANNING',
    'CRITICAL',
    '2026-07-31', NULL,
    NULL,
    1,
    CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP
),
(
    'v2.7.0',
    'Q3 2026 Release',
    'Advanced analytics and reporting features',
    'PLANNING',
    'MEDIUM',
    '2026-08-31', NULL,
    NULL,
    1,
    CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP
),
-- Draft/early planning
(
    'v3.1.0',
    'Fall 2026 Release',
    'Post-platform migration enhancements',
    'PLANNING',
    'LOW',
    '2026-10-31', NULL,
    NULL,
    1,
    CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP
);

-- =============================================================================
-- RELEASE-CYCLE ASSOCIATIONS
-- =============================================================================
-- Link releases to cycles (many-to-many relationship)
DO $$
DECLARE
    v_release_2_4_id BIGINT;
    v_release_2_5_id BIGINT;
    v_cycle_1_id BIGINT;
    v_cycle_2_id BIGINT;
BEGIN
    -- Get release IDs
    SELECT id INTO v_release_2_4_id FROM releases WHERE version = 'v2.4.0';
    SELECT id INTO v_release_2_5_id FROM releases WHERE version = 'v2.5.0';
    
    -- Get cycle IDs (assuming cycles exist from previous seeds)
    SELECT id INTO v_cycle_1_id FROM cycles WHERE name LIKE 'Cycle 1%' LIMIT 1;
    SELECT id INTO v_cycle_2_id FROM cycles WHERE name LIKE 'Cycle 2%' LIMIT 1;
    
    -- Create associations if cycles exist
    IF v_cycle_1_id IS NOT NULL AND v_release_2_4_id IS NOT NULL THEN
        INSERT INTO release_cycles (release_id, cycle_id)
        VALUES (v_release_2_4_id, v_cycle_1_id)
        ON CONFLICT DO NOTHING;
    END IF;
    
    IF v_cycle_2_id IS NOT NULL AND v_release_2_5_id IS NOT NULL THEN
        INSERT INTO release_cycles (release_id, cycle_id)
        VALUES (v_release_2_5_id, v_cycle_2_id)
        ON CONFLICT DO NOTHING;
    END IF;
END $$;

-- =============================================================================
-- EPIC-PITCH ASSOCIATIONS
-- =============================================================================
-- Link existing pitches to epics for demonstration
DO $$
DECLARE
    v_epic_checkout_id BIGINT;
    v_epic_cicd_id BIGINT;
    v_pitch_1_id BIGINT;
    v_pitch_2_id BIGINT;
BEGIN
    -- Get epic IDs
    SELECT id INTO v_epic_checkout_id FROM epics WHERE name = 'Mobile Checkout Redesign';
    SELECT id INTO v_epic_cicd_id FROM epics WHERE name = 'CI/CD Pipeline Improvements';
    
    -- Get some pitch IDs (from existing seed data)
    SELECT id INTO v_pitch_1_id FROM pitches WHERE title LIKE 'Password Reset%' LIMIT 1;
    SELECT id INTO v_pitch_2_id FROM pitches WHERE title LIKE 'Two-Factor%' LIMIT 1;
    
    -- Link pitches to epics if they exist
    IF v_pitch_1_id IS NOT NULL AND v_epic_checkout_id IS NOT NULL THEN
        UPDATE pitches SET epic_id = v_epic_checkout_id WHERE id = v_pitch_1_id;
    END IF;
    
    IF v_pitch_2_id IS NOT NULL AND v_epic_cicd_id IS NOT NULL THEN
        UPDATE pitches SET epic_id = v_epic_cicd_id WHERE id = v_pitch_2_id;
    END IF;
END $$;

-- Log the seed data creation
DO $$
BEGIN
    RAISE NOTICE 'Created roadmap demo data: 4 initiatives, 6 epics, 7 releases';
END $$;
