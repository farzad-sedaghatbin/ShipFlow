-- =============================================================================
-- V2026_02_08_1004: Seed Task Dependencies, Meeting Actions, and Enhanced Pitch Data
-- =============================================================================
-- This seed data demonstrates:
-- 1. Task dependencies (blocking relationships)
-- 2. Meeting action items with various statuses
-- 3. Work log timers (active timer demo)
-- 4. Pitch fields: problem_statement, solution, rabbit_holes, risks, no_gos
-- =============================================================================

-- =============================================================================
-- TASK DEPENDENCIES (referencing tasks from V21)
-- =============================================================================

-- Task 2 (Update deprecated dependencies) blocks Task 1 (Refactor auth middleware)
-- Can't refactor safely until dependencies are updated
INSERT INTO task_dependencies (source_task_id, target_task_id, dependency_type, created_at)
VALUES
(2, 1, 'BLOCKS', '2026-01-06 10:00:00');

-- Task 3 (Database query optimization) depends on Task 1 (Refactor auth middleware) being done  
-- Auth refactor will change query patterns, so optimization should wait
INSERT INTO task_dependencies (source_task_id, target_task_id, dependency_type, created_at)
VALUES
(1, 3, 'DEPENDS_ON', '2026-01-06 10:30:00');

-- Task 5 (WebSocket reconnection) blocks Task 4 (Safari notification sound)
-- Reconnection handling should be solid before fixing Safari-specific audio issues
INSERT INTO task_dependencies (source_task_id, target_task_id, dependency_type, created_at)
VALUES
(5, 4, 'BLOCKS', '2026-01-08 09:00:00');

-- Some cross-cutting related dependencies (not blocking, just related)
INSERT INTO task_dependencies (source_task_id, target_task_id, dependency_type, created_at)
VALUES
(1, 5, 'RELATED_TO', '2026-01-06 11:00:00');  -- Auth changes may affect WebSocket auth

-- =============================================================================
-- MEETING ACTIONS for meetings in V21 and V25
-- =============================================================================

-- Actions from Cycle 5 Kickoff meeting for Real-time Notifications (meeting likely ID 1 area)
-- We'll reference pitch 11's meetings
INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Set up WebSocket server infrastructure with STOMP protocol', 3, 'COMPLETED', '2026-01-08',
       'Completed on time. Used Spring WebSocket with STOMP.'
FROM meetings m WHERE m.pitch_id = 11 AND m.type = 'KICKOFF' LIMIT 1;

INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Create notification data model and database schema', 3, 'COMPLETED', '2026-01-07',
       'Schema reviewed and approved by team.'
FROM meetings m WHERE m.pitch_id = 11 AND m.type = 'KICKOFF' LIMIT 1;

INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Design notification UI components and toast animations', 2, 'COMPLETED', '2026-01-10',
       'Used Framer Motion for animations.'
FROM meetings m WHERE m.pitch_id = 11 AND m.type = 'KICKOFF' LIMIT 1;

INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Write test plan for WebSocket connection scenarios', 6, 'COMPLETED', '2026-01-09',
       'Test plan documented in Notion.'
FROM meetings m WHERE m.pitch_id = 11 AND m.type = 'KICKOFF' LIMIT 1;

-- Actions from Advanced Search kickoff
INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Set up Elasticsearch cluster in staging', 3, 'COMPLETED', '2026-01-08',
       'Single-node cluster for dev, 3-node for staging.'
FROM meetings m WHERE m.pitch_id = 12 AND m.type = 'KICKOFF' LIMIT 1;

INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Create search indexing pipeline for existing data', 3, 'IN_PROGRESS', '2026-01-12',
       'Initial indexing done, working on real-time sync.'
FROM meetings m WHERE m.pitch_id = 12 AND m.type = 'KICKOFF' LIMIT 1;

INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Design filter panel UI mockups', 4, 'COMPLETED', '2026-01-07',
       NULL
FROM meetings m WHERE m.pitch_id = 12 AND m.type = 'KICKOFF' LIMIT 1;

-- Actions from Data Export kickoff
INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Implement async export job queue', 5, 'COMPLETED', '2026-01-09',
       'Using RabbitMQ for job queue.'
FROM meetings m WHERE m.pitch_id = 13 AND m.type = 'KICKOFF' LIMIT 1;

INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Research PDF generation libraries', 5, 'COMPLETED', '2026-01-07',
       'Decided on Apache PDFBox + OpenPDF for templates.'
FROM meetings m WHERE m.pitch_id = 13 AND m.type = 'KICKOFF' LIMIT 1;

-- Actions from Team Collaboration Hub kickoff  
INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Define collaboration workspace data model', 5, 'COMPLETED', '2026-01-08',
       'ER diagram reviewed with team.'
FROM meetings m WHERE m.pitch_id = 14 AND m.type = 'KICKOFF' LIMIT 1;

INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Design @mention autocomplete UX', 4, 'COMPLETED', '2026-01-08',
       'Figma mockups shared with team.'
FROM meetings m WHERE m.pitch_id = 14 AND m.type = 'KICKOFF' LIMIT 1;

INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Set up integration with notifications system', 5, 'OPEN', '2026-01-15',
       'Blocked on notifications feature completion.'
FROM meetings m WHERE m.pitch_id = 14 AND m.type = 'KICKOFF' LIMIT 1;

-- Actions from Standup meetings
INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Investigate notification delivery latency', 3, 'COMPLETED', '2026-01-09',
       'Found issue with message serialization. Fixed.'
FROM meetings m WHERE m.pitch_id = 11 AND m.type = 'STANDUP' LIMIT 1;

INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Review and merge notification service PR', 2, 'COMPLETED', '2026-01-08',
       'Merged after code review.'
FROM meetings m WHERE m.pitch_id = 11 AND m.type = 'STANDUP' LIMIT 1;

-- Actions from Hill Chart Review meetings
INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Accelerate notification preferences UI development', 2, 'IN_PROGRESS', '2026-01-14',
       'Bringing in David to help with frontend.'
FROM meetings m WHERE m.pitch_id = 11 AND m.type = 'HILL_CHART_REVIEW' LIMIT 1;

INSERT INTO meeting_actions (meeting_id, description, assigned_to_id, status, due_date, notes)
SELECT m.id, 'Add Lighthouse CI for search UI performance', 2, 'OPEN', '2026-01-16',
       'Performance budget: 200ms for first result.'
FROM meetings m WHERE m.pitch_id = 12 AND m.type = 'HILL_CHART_REVIEW' LIMIT 1;

-- =============================================================================
-- WORK LOG TIMER (active timer for a developer)
-- Note: Check if work_log_timers table exists
-- =============================================================================

-- Insert a currently running timer for Alice working on search UI
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) 
VALUES
(2, 12, '2026-01-11', 0, 'Working on search UI filter panel - timer running');

-- Store the timer reference (we'll update this work log when timer stops)
-- This assumes there's a way to track active timers - if not, we'll simulate it
-- For now, add a partial work log entry that shows active work

-- =============================================================================
-- UPDATE PITCHES with Shape Up methodology fields
-- =============================================================================

-- Pitch 1: Password Reset Flow
UPDATE pitches SET 
    problem_statement = 'Users currently have no way to recover their account if they forget their password. This leads to 50+ support tickets per week and frustrated customers who often abandon their accounts entirely.',
    solution = 'Implement a secure, self-service password reset flow:

**Fat Marker Sketch:**
- Reset request form (email input only)
- Email with secure, expiring token (15 min TTL)
- Token validation screen
- New password form with strength indicator
- Confirmation and auto-login

**Key decisions:**
- Use JWT tokens with short expiry
- Rate limit: 3 requests per email per hour
- Invalidate all sessions on password change',
    rabbit_holes = '- Building a full audit trail of password changes (defer to audit logging feature)
- Supporting phone-based reset (out of scope, future SMS integration)
- Complex password policy UI (just show basic requirements)
- Account lockout logic (separate security feature)',
    risks = '- Token security: Mitigated by short expiry and one-time use
- Email deliverability: Using SendGrid with verified domain
- Brute force attacks: Rate limiting implemented
- Unknown: User confusion if email doesn''t arrive (add troubleshooting help)',
    no_gos = '- No SMS/phone verification in this version
- No security questions
- No admin-initiated password resets
- No "forgot username" flow',
    wireframe_links = 'https://figma.com/file/mock123/password-reset-flow'
WHERE id = 1;

-- Pitch 4: Two-Factor Authentication
UPDATE pitches SET 
    problem_statement = 'Enterprise customers require 2FA for security compliance (SOC 2, HIPAA). We''re losing deals because competitors offer this. Also, 12% of support tickets are about unauthorized account access.',
    solution = 'Add TOTP-based 2FA with authenticator app support:

**Fat Marker Sketch:**
- 2FA setup wizard in security settings
- QR code generation for authenticator apps
- Backup codes (10 one-time codes)
- 2FA challenge on login when enabled
- Disable 2FA with re-authentication

**Key decisions:**
- Support Google Authenticator, Authy, 1Password, etc.
- No SMS fallback (less secure, adds complexity)
- 10 backup codes, show once, can regenerate',
    rabbit_holes = '- Hardware key support like YubiKey (future enhancement)
- Multiple 2FA methods per account (complexity)
- Recovery flow for lost authenticator (backup codes suffice)
- Trusted devices/remember me (separate feature)',
    risks = '- Users losing authenticator: Backup codes mitigate
- Time drift on TOTP: Using 30-second windows with ±1 tolerance
- Migration of existing users: Opt-in, not mandatory initially
- Unknown: Performance impact of additional auth step',
    no_gos = '- No SMS-based 2FA
- No email-based 2FA codes
- No biometric 2FA in this version
- No enforced 2FA for all users (org admin decision)',
    wireframe_links = 'https://figma.com/file/mock456/2fa-setup-flow'
WHERE id = 4;

-- Pitch 6: Offline Mode
UPDATE pitches SET 
    problem_statement = 'Field sales team loses work when traveling through areas with poor connectivity. They report data entry taking 3x longer because they have to wait for network or re-enter lost data. Estimated productivity loss: 8 hours/week per rep.',
    solution = 'Implement offline-first architecture with sync:

**Fat Marker Sketch:**
- Service Worker for caching static assets
- IndexedDB for offline data storage
- Sync queue for pending changes
- Visual indicator: Online/Offline/Syncing status
- Conflict resolution: Last-write-wins with user notification

**Key decisions:**
- Cache last 7 days of data by default
- Sync on reconnection + manual sync button
- Show "pending sync" badge on offline-modified items',
    rabbit_holes = '- Real-time collaboration while offline (not possible)
- Syncing large file attachments (limit to metadata)
- Complex merge conflicts (CRDT, multi-party sync)
- Background sync on mobile (battery concerns)',
    risks = '- Data conflicts: Last-write-wins may lose changes; mitigated by user notification
- Storage limits: IndexedDB has limits; implement data eviction
- Cache invalidation: Version API responses for staleness detection
- Unknown: User behavior with stale data',
    no_gos = '- No offline file uploads
- No offline real-time collaboration
- No selective sync (all or nothing for now)
- No offline notifications',
    wireframe_links = 'https://figma.com/file/mock789/offline-mode-ux'
WHERE id = 6;

-- Pitch 11: Real-time Notifications (Cycle 5)
UPDATE pitches SET 
    problem_statement = 'Users miss important updates because they have to manually refresh the page. Comments, mentions, and status changes go unnoticed for hours. This causes delays in team collaboration and missed deadlines.',
    solution = 'WebSocket-based real-time notification system:

**Fat Marker Sketch:**
- WebSocket connection on page load
- Notification types: mentions, comments, status changes
- Toast notifications with dismiss/action buttons
- Notification center dropdown (bell icon)
- Unread badge count
- User preferences per notification type

**Technical approach:**
- Spring WebSocket with STOMP
- Redis pub/sub for multi-instance support
- Exponential backoff for reconnection',
    rabbit_holes = '- Push notifications to mobile (separate mobile feature)
- Email digest of missed notifications (use existing email system)
- Notification grouping/batching logic (nice to have)
- Historical notification feed beyond 30 days',
    risks = '- WebSocket scaling: Redis pub/sub handles multi-instance
- Connection stability: Reconnection with exponential backoff
- Browser tab notification: Focus events handled properly
- Unknown: Performance with many concurrent connections',
    no_gos = '- No mobile push notifications
- No browser push notifications (PWA scope)
- No notification scheduling
- No email fallback for real-time notifications',
    wireframe_links = 'https://figma.com/file/mock101/realtime-notifications'
WHERE id = 11;

-- Pitch 12: Advanced Search (Cycle 5)
UPDATE pitches SET 
    problem_statement = 'Finding content is painful. Users report spending 10+ minutes searching for items they know exist. Current search only matches exact titles. No filters, no fuzzy matching, no recent searches.',
    solution = 'Full-text search with Elasticsearch:

**Fat Marker Sketch:**
- Smart search bar with autocomplete
- Filters: type, date range, author, tags
- Recent searches dropdown
- Search results with highlighted matches
- Keyboard shortcuts (/, Cmd+K)

**Technical approach:**
- Elasticsearch for indexing and querying
- Real-time index updates via event listener
- Search-as-you-type with debouncing',
    rabbit_holes = '- Natural language queries ("show me tasks from last week")
- Search synonyms and custom dictionaries
- Faceted search with counts
- Search analytics and optimization',
    risks = '- Elasticsearch cluster management: Start with managed service
- Index sync consistency: Event-driven updates with reconciliation job
- Search relevance tuning: Iterative improvement based on usage
- Unknown: Query patterns at scale',
    no_gos = '- No natural language search
- No saved searches
- No search result export
- No cross-project search',
    wireframe_links = 'https://figma.com/file/mock102/advanced-search'
WHERE id = 12;

-- =============================================================================
-- Mark one pitch with circuit breaker triggered (for demo)
-- =============================================================================

UPDATE pitches SET is_circuit_breaker_triggered = true 
WHERE id = 14; -- Team Collaboration Hub - behind schedule

-- =============================================================================
-- Add anonymous retro items for demo
-- =============================================================================

-- Add some anonymous retro items (requires is_anonymous column from V49)
INSERT INTO retro_items (content, column_type, retrospective_id, author_id, vote_count, is_anonymous, created_at, updated_at) 
VALUES
('Management pressure to cut corners on testing is harming product quality', 'DID_NOT_GO_WELL', 1, 3, 6, true, '2025-11-13 11:50:00', '2025-11-14 15:00:00'),
('Some team members are not pulling their weight', 'DID_NOT_GO_WELL', 1, 5, 3, true, '2025-11-13 11:55:00', '2025-11-14 15:00:00');

-- =============================================================================
-- Reset sequences
-- =============================================================================
SELECT setval(pg_get_serial_sequence('task_dependencies', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('meeting_actions', 'id'), 100, false);
SELECT setval(pg_get_serial_sequence('retro_items', 'id'), 100, false);
