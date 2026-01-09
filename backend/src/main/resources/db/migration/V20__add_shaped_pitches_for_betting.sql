-- Add Cycle 4 with SHAPED pitches for Betting Table demo
-- These pitches are ready to be "bet on" and assigned to teams during the betting ceremony

-- Add Cycle 4 for betting demo
INSERT INTO cycles (id, name, start_date, end_date, phase, project_id, is_active) VALUES
(4, 'Cycle 4 - Betting Demo', '2025-12-01', '2026-02-28', 'SHAPING', 1, true);

-- Add teams to Cycle 4
INSERT INTO teams (id, name, cycle_id) VALUES
(1, 'Alpha Team', 4),
(2, 'Beta Team', 4),
(3, 'Gamma Team', 4);

-- Add team assignments
INSERT INTO team_assignments (person_id, team_id, role, start_date, is_active) VALUES
(2, 1, 'FRONTEND', '2025-12-01', true),
(3, 1, 'BACKEND', '2025-12-01', true),
(4, 2, 'DESIGNER', '2025-12-01', true),
(5, 2, 'FULLSTACK', '2025-12-01', true),
(6, 3, 'QA', '2025-12-01', true);

-- ===========================================
-- 10 SHAPED Pitches for Cycle 4
-- ===========================================

INSERT INTO pitches (id, title, description, appetite_days, cycle_id, team_id, status, created_at, updated_at) VALUES
-- Small batches (1 week)
(1, 'Password Reset Flow', 'Implement secure password reset with email verification and expiring tokens', 7, 4, NULL, 'SHAPED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'User Profile Settings', 'Allow users to update their profile information, avatar, and preferences', 5, 4, NULL, 'SHAPED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Biometric Login', 'Add Face ID and fingerprint authentication for mobile app', 5, 4, NULL, 'SHAPED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Medium batches (2 weeks)
(4, 'Two-Factor Authentication', 'Add 2FA support with TOTP authenticator apps and backup codes', 14, 4, NULL, 'SHAPED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'Activity History Timeline', 'Show users a timeline of their account activity and login history', 10, 4, NULL, 'SHAPED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'Offline Mode', 'Enable offline data caching and sync when connection restored', 14, 4, NULL, 'SHAPED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'Custom Dashboards', 'Allow users to create custom dashboards with drag-drop widgets', 14, 4, NULL, 'SHAPED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Big batches (3-4 weeks)
(8, 'Billing & Payments Integration', 'Integrate Stripe for subscription management and payment processing', 21, 4, NULL, 'SHAPED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 'Multi-tenant Support', 'Add organization/workspace support with role-based permissions', 28, 4, NULL, 'SHAPED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'Workflow Automation', 'Visual workflow builder for automating repetitive tasks', 28, 4, NULL, 'SHAPED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- Generate initial betting slots for all teams in Cycle 4
-- ===========================================

INSERT INTO betting_slots (id, cycle_id, team_id, position, start_date, end_date, pitch_id, notes, created_at, updated_at) VALUES
(1, 4, 1, 0, '2025-12-01', '2026-02-28', NULL, 'Alpha Team capacity for betting cycle', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 4, 2, 0, '2025-12-01', '2026-02-28', NULL, 'Beta Team capacity for betting cycle', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 4, 3, 0, '2025-12-01', '2026-02-28', NULL, 'Gamma Team capacity for betting cycle', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- Reset sequences to avoid primary key conflicts
-- H2 uses ALTER TABLE to restart identity columns
-- ===========================================

ALTER TABLE betting_slots ALTER COLUMN id RESTART WITH 100;
ALTER TABLE pitches ALTER COLUMN id RESTART WITH 100;
ALTER TABLE teams ALTER COLUMN id RESTART WITH 100;
ALTER TABLE cycles ALTER COLUMN id RESTART WITH 100;
