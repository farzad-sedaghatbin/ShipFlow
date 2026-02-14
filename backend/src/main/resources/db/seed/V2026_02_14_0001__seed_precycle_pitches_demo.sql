-- =============================================================================
-- V2026_02_14_0001: Seed Pre-Cycle Pitches for Shape Up Workflow Demo
-- =============================================================================
-- This seed data demonstrates the Shape Up pre-cycle pitch lifecycle:
-- - IDEA: Initial pitch ideas that need exploration
-- - DRAFT: Pitches being shaped but not yet ready
-- - SHAPED: Fully shaped pitches ready for betting (no cycle assigned)
-- 
-- These pitches have cycle_id = NULL because they haven't been bet on yet.
-- The betting table will show SHAPED pitches as candidates for assignment.
-- =============================================================================

-- =============================================================================
-- IDEA PITCHES - Initial concepts that need exploration
-- =============================================================================
INSERT INTO pitches (
    title, description, appetite_days, cycle_id, team_id, status, 
    problem_statement, solution, rabbit_holes, no_gos,
    created_at, updated_at
) VALUES
(
    'Voice Search Integration',
    'Add voice search capability to the application for hands-free navigation',
    NULL, NULL, NULL, 'IDEA',
    'Users on mobile and in field environments struggle to type searches. Voice input would improve accessibility and ease of use.',
    NULL, NULL, NULL,
    CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days'
),
(
    'AI-Powered Recommendations',
    'Use machine learning to suggest relevant content based on user behavior',
    NULL, NULL, NULL, 'IDEA',
    'Users spend too much time searching for content. Personalized recommendations could surface relevant items proactively.',
    NULL, NULL, NULL,
    CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days'
),
(
    'Dark Theme Support',
    'Add system-wide dark theme option for reduced eye strain',
    NULL, NULL, NULL, 'IDEA',
    'Many users work in low-light environments. Dark theme would reduce eye strain and battery consumption on OLED displays.',
    NULL, NULL, NULL,
    CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day'
);

-- =============================================================================
-- DRAFT PITCHES - Being shaped, not yet ready for betting
-- =============================================================================
INSERT INTO pitches (
    title, description, appetite_days, cycle_id, team_id, status,
    problem_statement, solution, rabbit_holes, no_gos,
    created_at, updated_at
) VALUES
(
    'Real-time Collaboration',
    'Enable multiple users to edit documents simultaneously with live cursors',
    14, NULL, NULL, 'DRAFT',
    'Teams struggle to collaborate on shared documents without overwriting each other''s work. Email attachments cause version conflicts.',
    'Implement operational transformation (OT) for real-time sync. Show presence indicators and live cursors. Start with text documents only.',
    'Conflict resolution for simultaneous edits at same position',
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '2 days'
),
(
    'Advanced Search Filters',
    'Add powerful filtering capabilities to search results',
    7, NULL, NULL, 'DRAFT',
    'Users cannot narrow down search results effectively. Large result sets are overwhelming without proper filtering.',
    'Add faceted search with filters for date range, type, author, and tags. Include saved searches feature.',
    'Performance impact of complex filter queries on large datasets',
    'Full-text search rewrite - only extend existing capability',
    CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '1 day'
);

-- =============================================================================
-- SHAPED PITCHES - Ready for betting (will appear in betting table)
-- =============================================================================
-- These pitches are fully shaped with all Shape Up elements and ready to bet on
INSERT INTO pitches (
    title, description, appetite_days, cycle_id, team_id, status,
    problem_statement, solution, rabbit_holes, risks, no_gos,
    created_at, updated_at
) VALUES
(
    'Export to PDF',
    'Allow users to export reports and dashboards to professionally formatted PDFs',
    5, NULL, NULL, 'SHAPED',
    'Users need to share reports with stakeholders who don''t have system access. Currently they screenshot or manually recreate reports.',
    'Add PDF export button to reports page. Use server-side PDF generation with company branding. Include charts and tables with proper formatting.',
    'Complex chart rendering in PDF format may require third-party library',
    'PDF generation could timeout for very large reports',
    'Custom templates - use standard company branding only',
    CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '1 day'
),
(
    'Bulk Data Import',
    'Enable importing data from CSV and Excel files with validation',
    10, NULL, NULL, 'SHAPED',
    'New customers spend weeks manually entering historical data. This delays time-to-value and creates data entry errors.',
    'Create import wizard with: 1) File upload 2) Column mapping 3) Preview with validation errors 4) Confirm and import. Support CSV and XLSX.',
    'Character encoding issues with international data',
    'Large files could cause memory issues - need chunked processing',
    'Real-time sync with external systems - import is one-time batch only',
    CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '2 days'
),
(
    'Email Digest Notifications',
    'Send daily/weekly email summaries instead of individual notifications',
    7, NULL, NULL, 'SHAPED',
    'Users complain about notification overload. Too many individual emails cause important updates to be missed.',
    'Add digest preference in notification settings. Batch notifications and send summary emails at user-selected frequency.',
    'Email template complexity for variable content',
    'Delayed notifications might miss urgent items',
    'Real-time alerts - digest is for informational updates only',
    CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '1 day'
),
(
    'API Rate Limiting Dashboard',
    'Display API usage metrics and rate limit status for developers',
    5, NULL, NULL, 'SHAPED',
    'Developers integrating with our API have no visibility into their usage or remaining quota. They hit limits unexpectedly.',
    'Create developer dashboard showing: Current usage, remaining quota, historical trends, and alerts when approaching limits.',
    'Real-time usage tracking performance',
    'Cache invalidation for rate limit counters',
    'Automatic limit increases - manual review required',
    CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP
);

-- Log the seed data creation
DO $$
BEGIN
    RAISE NOTICE 'Created pre-cycle pitch demo data: 3 IDEA, 2 DRAFT, 4 SHAPED pitches';
END $$;
