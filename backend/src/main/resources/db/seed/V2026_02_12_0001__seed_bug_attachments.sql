-- =============================================================================
-- V2026_02_12_0001: Seed Bug Report Attachments
-- =============================================================================
-- This seed data demonstrates:
-- 1. Image attachments (screenshots, error screens)
-- 2. Video attachments (screen recordings, reproductions)
-- 3. Multiple attachments per bug report
-- 4. Various file types (PNG, JPG, MP4, WEBM)
-- =============================================================================

-- Note: In production, these files would be uploaded through the API and stored
-- in the configured upload directory. For demo purposes, we're creating database
-- records that represent these attachments. The actual files should be placed in
-- the upload directory before running the application.

-- =============================================================================
-- ATTACHMENTS FOR BUG-001: Email notifications not delivered
-- =============================================================================

-- Screenshot showing the notification settings page
INSERT INTO uploaded_documents (
    file_name, original_file_name, storage_path, file_type, file_size,
    extracted_text, text_extracted, entity_type, entity_id, 
    uploader_id, created_at
) VALUES 
(
    '550e8400-e29b-41d4-a716-446655440001.png',
    'email-settings-screenshot.png',
    'bug_attachments/550e8400-e29b-41d4-a716-446655440001.png',
    'png',
    245678,
    NULL,
    false,
    'BUG_REPORT',
    1,  -- BUG-001
    6,  -- Reporter (Emma Brown)
    '2025-11-08 10:05:00'
),
(
    '550e8400-e29b-41d4-a716-446655440002.png',
    'gmail-inbox-empty.png',
    'bug_attachments/550e8400-e29b-41d4-a716-446655440002.png',
    'png',
    198432,
    NULL,
    false,
    'BUG_REPORT',
    1,  -- BUG-001
    6,  -- Reporter (Emma Brown)
    '2025-11-08 10:10:00'
);

-- =============================================================================
-- ATTACHMENTS FOR BUG-002: Dashboard widgets freeze when resizing
-- =============================================================================

-- Screen recording showing the freeze behavior
INSERT INTO uploaded_documents (
    file_name, original_file_name, storage_path, file_type, file_size,
    extracted_text, text_extracted, entity_type, entity_id, 
    uploader_id, created_at
) VALUES 
(
    '550e8400-e29b-41d4-a716-446655440003.mp4',
    'dashboard-freeze-reproduction.mp4',
    'bug_attachments/550e8400-e29b-41d4-a716-446655440003.mp4',
    'mp4',
    5678901,
    NULL,
    false,
    'BUG_REPORT',
    2,  -- BUG-002
    2,  -- Reporter (Alice Johnson)
    '2025-10-30 09:15:00'
),
(
    '550e8400-e29b-41d4-a716-446655440004.png',
    'console-error-screenshot.png',
    'bug_attachments/550e8400-e29b-41d4-a716-446655440004.png',
    'png',
    312456,
    NULL,
    false,
    'BUG_REPORT',
    2,  -- BUG-002
    2,  -- Reporter (Alice Johnson)
    '2025-10-30 09:20:00'
),
(
    '550e8400-e29b-41d4-a716-446655440005.jpg',
    'browser-dev-tools.jpg',
    'bug_attachments/550e8400-e29b-41d4-a716-446655440005.jpg',
    'jpg',
    456789,
    NULL,
    false,
    'BUG_REPORT',
    2,  -- BUG-002
    2,  -- Reporter (Alice Johnson)
    '2025-10-30 09:25:00'
);

-- =============================================================================
-- ATTACHMENTS FOR BUG-005: Notification preferences not persisting
-- =============================================================================

-- Before and after screenshots
INSERT INTO uploaded_documents (
    file_name, original_file_name, storage_path, file_type, file_size,
    extracted_text, text_extracted, entity_type, entity_id, 
    uploader_id, created_at
) VALUES 
(
    '550e8400-e29b-41d4-a716-446655440006.png',
    'settings-before-refresh.png',
    'bug_attachments/550e8400-e29b-41d4-a716-446655440006.png',
    'png',
    178923,
    NULL,
    false,
    'BUG_REPORT',
    5,  -- BUG-005
    3,  -- Reporter (Bob Williams)
    '2025-11-12 09:05:00'
),
(
    '550e8400-e29b-41d4-a716-446655440007.png',
    'settings-after-refresh.png',
    'bug_attachments/550e8400-e29b-41d4-a716-446655440007.png',
    'png',
    182341,
    NULL,
    false,
    'BUG_REPORT',
    5,  -- BUG-005
    3,  -- Reporter (Bob Williams)
    '2025-11-12 09:10:00'
);

-- =============================================================================
-- ATTACHMENTS FOR BUG-006: Mobile dashboard layout breaks on iPad
-- =============================================================================

-- iPad screenshots showing the layout issue
INSERT INTO uploaded_documents (
    file_name, original_file_name, storage_path, file_type, file_size,
    extracted_text, text_extracted, entity_type, entity_id, 
    uploader_id, created_at
) VALUES 
(
    '550e8400-e29b-41d4-a716-446655440008.png',
    'ipad-portrait-layout-broken.png',
    'bug_attachments/550e8400-e29b-41d4-a716-446655440008.png',
    'png',
    534210,
    NULL,
    false,
    'BUG_REPORT',
    6,  -- BUG-006
    4,  -- Reporter (David Martinez)
    '2025-11-15 14:05:00'
),
(
    '550e8400-e29b-41d4-a716-446655440009.png',
    'ipad-landscape-working.png',
    'bug_attachments/550e8400-e29b-41d4-a716-446655440009.png',
    'png',
    498765,
    NULL,
    false,
    'BUG_REPORT',
    6,  -- BUG-006
    4,  -- Reporter (David Martinez)
    '2025-11-15 14:10:00'
),
(
    '550e8400-e29b-41d4-a716-44665544000a.webm',
    'ipad-issue-recording.webm',
    'bug_attachments/550e8400-e29b-41d4-a716-44665544000a.webm',
    'webm',
    3456789,
    NULL,
    false,
    'BUG_REPORT',
    6,  -- BUG-006
    4,  -- Reporter (David Martinez)
    '2025-11-15 14:15:00'
);

-- =============================================================================
-- ATTACHMENTS FOR BUG-009: Search autocomplete shows results after search
-- =============================================================================

-- Screen recording showing the autocomplete delay issue
INSERT INTO uploaded_documents (
    file_name, original_file_name, storage_path, file_type, file_size,
    extracted_text, text_extracted, entity_type, entity_id, 
    uploader_id, created_at
) VALUES 
(
    '550e8400-e29b-41d4-a716-44665544000b.mp4',
    'autocomplete-delay.mp4',
    'bug_attachments/550e8400-e29b-41d4-a716-44665544000b.mp4',
    'mp4',
    2345678,
    NULL,
    false,
    'BUG_REPORT',
    9,  -- BUG-009
    2,  -- Reporter (Alice Johnson)
    '2026-01-11 10:15:00'
);

-- =============================================================================
-- ATTACHMENTS FOR BUG-010: WebSocket connection drops
-- =============================================================================

-- Network inspector screenshots and video
INSERT INTO uploaded_documents (
    file_name, original_file_name, storage_path, file_type, file_size,
    extracted_text, text_extracted, entity_type, entity_id, 
    uploader_id, created_at
) VALUES 
(
    '550e8400-e29b-41d4-a716-44665544000c.png',
    'network-tab-websocket-closed.png',
    'bug_attachments/550e8400-e29b-41d4-a716-44665544000c.png',
    'png',
    398765,
    NULL,
    false,
    'BUG_REPORT',
    10,  -- BUG-010
    6,  -- Reporter (Emma Brown)
    '2026-01-09 15:10:00'
),
(
    '550e8400-e29b-41d4-a716-44665544000d.webm',
    'websocket-reconnection-video.webm',
    'bug_attachments/550e8400-e29b-41d4-a716-44665544000d.webm',
    'webm',
    4567890,
    NULL,
    false,
    'BUG_REPORT',
    10,  -- BUG-010
    6,  -- Reporter (Emma Brown)
    '2026-01-09 15:20:00'
),
(
    '550e8400-e29b-41d4-a716-44665544000e.jpg',
    'console-errors-websocket.jpg',
    'bug_attachments/550e8400-e29b-41d4-a716-44665544000e.jpg',
    'jpg',
    287654,
    NULL,
    false,
    'BUG_REPORT',
    10,  -- BUG-010
    6,  -- Reporter (Emma Brown)
    '2026-01-09 15:25:00'
);

-- =============================================================================
-- Summary
-- =============================================================================
-- Total seed data created:
-- - Bug attachments: 14 files across 7 bug reports
-- - File types: PNG (9), JPG (2), MP4 (2), WEBM (2)
-- - Demonstrates:
--   * Single file attachments (BUG-001, BUG-009)
--   * Multiple file attachments (BUG-002, BUG-005, BUG-006, BUG-010)
--   * Mixed media types (images + videos on same bug)
--   * Different file sizes (realistic ranges)
-- =============================================================================
