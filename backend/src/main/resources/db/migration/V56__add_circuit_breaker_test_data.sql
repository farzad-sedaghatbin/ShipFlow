-- V55: Add work logs to Cycle 4 pitches to demonstrate Circuit Breaker functionality
-- This adds work logs that exceed appetite for some pitches

-- ===========================================
-- Work Logs for Cycle 4 Pitches (demonstrating circuit breaker)
-- ===========================================

-- Pitch 1: Password Reset Flow (appetite: 7 days = 56 hours)
-- Add 65 hours (116% of appetite - OVERFLOW!)
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(2, 1, '2025-12-02', 8.0, 'Started password reset flow - email templates'),
(2, 1, '2025-12-03', 8.0, 'Built token generation and validation logic'),
(3, 1, '2025-12-03', 7.5, 'Implemented password reset API endpoints'),
(2, 1, '2025-12-04', 8.0, 'Created reset password UI components'),
(3, 1, '2025-12-04', 8.0, 'Added token expiration and security features'),
(2, 1, '2025-12-05', 6.5, 'Integrated email service with reset flow'),
(3, 1, '2025-12-05', 7.0, 'Fixed edge cases and validation issues'),
(2, 1, '2025-12-06', 6.0, 'More UI refinements than expected'),
(3, 1, '2025-12-06', 6.0, 'Additional security hardening required');

-- Pitch 2: User Profile Settings (appetite: 5 days = 40 hours)
-- Add 48 hours (120% of appetite - OVERFLOW!)
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(2, 2, '2025-12-02', 8.0, 'Built profile settings form'),
(4, 2, '2025-12-02', 6.0, 'Designed profile UI mockups'),
(2, 2, '2025-12-03', 8.0, 'Implemented avatar upload'),
(3, 2, '2025-12-03', 8.0, 'Profile update API'),
(2, 2, '2025-12-04', 6.0, 'Preference management UI'),
(3, 2, '2025-12-04', 6.0, 'Backend preference storage'),
(2, 2, '2025-12-05', 6.0, 'Unexpected validation complexity');

-- Pitch 3: Biometric Login (appetite: 5 days = 40 hours)  
-- Add 22 hours (55% of appetite - OK)
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(2, 3, '2025-12-02', 8.0, 'Research biometric APIs'),
(3, 3, '2025-12-02', 7.0, 'Backend token integration'),
(2, 3, '2025-12-03', 7.0, 'Initial Face ID integration');

-- Pitch 4: Two-Factor Authentication (appetite: 14 days = 112 hours)
-- Add 95 hours (85% of appetite - WARNING zone)
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(3, 4, '2025-12-02', 8.0, 'TOTP library integration'),
(3, 4, '2025-12-03', 8.0, 'QR code generation'),
(2, 4, '2025-12-03', 8.0, '2FA setup UI'),
(3, 4, '2025-12-04', 8.0, 'Backup codes system'),
(2, 4, '2025-12-04', 8.0, '2FA verification flow'),
(3, 4, '2025-12-05', 7.5, 'Session management with 2FA'),
(2, 4, '2025-12-05', 7.5, 'Recovery flow UI'),
(3, 4, '2025-12-06', 8.0, 'Testing 2FA edge cases'),
(2, 4, '2025-12-06', 8.0, 'UI polish and accessibility'),
(6, 4, '2025-12-09', 8.0, 'QA testing 2FA flows'),
(6, 4, '2025-12-10', 8.0, 'Additional test scenarios'),
(3, 4, '2025-12-11', 8.0, 'Bug fixes from QA');

-- Pitch 5: Activity History Timeline (appetite: 10 days = 80 hours)
-- Add 94 hours (118% of appetite - OVERFLOW!)
INSERT INTO work_logs (person_id, pitch_id, date, hours_spent, note) VALUES
(3, 5, '2025-12-02', 8.0, 'Activity tracking service'),
(3, 5, '2025-12-03', 8.0, 'Database schema for history'),
(2, 5, '2025-12-03', 8.0, 'Timeline UI component'),
(4, 5, '2025-12-04', 6.0, 'Timeline design iterations'),
(2, 5, '2025-12-04', 8.0, 'Infinite scroll implementation'),
(3, 5, '2025-12-05', 8.0, 'Activity filtering API'),
(2, 5, '2025-12-05', 8.0, 'Filter UI controls'),
(3, 5, '2025-12-06', 8.0, 'Performance optimization'),
(2, 5, '2025-12-06', 8.0, 'Pagination refinements'),
(3, 5, '2025-12-09', 8.0, 'More performance work needed'),
(2, 5, '2025-12-09', 8.0, 'Additional UI polish'),
(6, 5, '2025-12-10', 8.0, 'QA discovered scope creep');

-- Update pitch statuses to reflect active development
UPDATE pitches SET status = 'IN_PROGRESS', updated_at = CURRENT_TIMESTAMP WHERE id IN (1, 2, 4, 5);
UPDATE pitches SET status = 'STARTED', updated_at = CURRENT_TIMESTAMP WHERE id = 3;
