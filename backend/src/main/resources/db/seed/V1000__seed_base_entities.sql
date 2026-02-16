-- ============================================================================
-- V1000: Base entities seed data (Projects, Users, Persons, Teams)
-- This file creates the core entities needed for demo/dev environment
-- ============================================================================

-- ===========================================
-- 1. PROJECTS
-- ===========================================
INSERT INTO projects (id, name, project_key, description, color, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'ShipFlow Platform', 'SHIP', 'The main ShipFlow product development project', '#3B82F6', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Mobile App', 'MOB', 'Mobile application for iOS and Android', '#10B981', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Analytics Dashboard', 'DASH', 'Business analytics and reporting platform', '#8B5CF6', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- 2. PERSONS (Team members - not tied to auth users)
-- ===========================================
INSERT INTO persons (id, name, email, skills, avatar_url, department, bio, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'Admin User', 'admin@shipflow.dev', 'Project Management, Strategy', NULL, 'Leadership', 'System administrator', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Alice Developer', 'alice@shipflow.dev', 'React, TypeScript, Node.js', NULL, 'Engineering', 'Senior Frontend Developer', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Bob Backend', 'bob@shipflow.dev', 'Java, Spring Boot, PostgreSQL', NULL, 'Engineering', 'Backend Engineer', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Carol Designer', 'carol@shipflow.dev', 'Figma, UI/UX, Design Systems', NULL, 'Design', 'Product Designer', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'Dan Fullstack', 'dan@shipflow.dev', 'React, Java, AWS', NULL, 'Engineering', 'Fullstack Developer', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'Eve QA', 'eve@shipflow.dev', 'Testing, Automation, Cypress', NULL, 'Quality', 'QA Engineer', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'Frank PM', 'frank@shipflow.dev', 'Agile, Scrum, Roadmapping', NULL, 'Product', 'Product Manager', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'Grace DevOps', 'grace@shipflow.dev', 'Docker, Kubernetes, CI/CD', NULL, 'Engineering', 'DevOps Engineer', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- 3. USERS (Authentication accounts)
-- Password is 'password123' hashed with BCrypt
-- Valid UserRole values: ADMIN, MANAGER, MEMBER, READONLY
-- ===========================================
INSERT INTO users (id, username, password, role, person_id, is_active, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'alice', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MEMBER', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'bob', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MEMBER', 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'carol', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MEMBER', 4, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'dan', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MEMBER', 5, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'eve', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MEMBER', 6, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'frank', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MANAGER', 7, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'grace', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MEMBER', 8, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ===========================================
-- 4. CYCLES (Shape Up 6-week cycles)
-- Valid CyclePhase values: SHAPING, BETTING, BUILD, COOLDOWN
-- ===========================================
INSERT INTO cycles (id, name, start_date, end_date, phase, project_id, is_active) OVERRIDING SYSTEM VALUE VALUES
-- Past completed cycles
(1, 'Cycle 1 - Foundation', '2025-06-01', '2025-07-12', 'COOLDOWN', 1, false),
(2, 'Cycle 2 - Core Features', '2025-07-15', '2025-08-26', 'COOLDOWN', 1, false),
(3, 'Cycle 3 - Polish', '2025-09-01', '2025-10-12', 'COOLDOWN', 1, false),
-- Current active cycle in BUILD phase
(4, 'Cycle 4 - Integrations', '2025-10-15', '2025-11-26', 'BUILD', 1, true),
-- Future cycle in SHAPING
(5, 'Cycle 5 - Mobile', '2025-12-01', '2026-01-12', 'SHAPING', 1, true),
-- Mobile App project cycles
(6, 'Mobile Cycle 1', '2025-09-01', '2025-10-12', 'COOLDOWN', 2, false),
(7, 'Mobile Cycle 2', '2025-10-15', '2025-11-26', 'BUILD', 2, true);

-- ===========================================
-- 5. TEAMS (Per-cycle teams)
-- ===========================================
INSERT INTO teams (id, name, cycle_id) OVERRIDING SYSTEM VALUE VALUES
(1, 'Alpha Team', 4),
(2, 'Beta Team', 4),
(3, 'Gamma Team', 5),
(4, 'Mobile Team', 7);

-- ===========================================
-- 6. TEAM ASSIGNMENTS
-- Valid TeamMemberRole values: BACKEND, FRONTEND, QA, DESIGNER, FULLSTACK, TECH_LEAD, PRODUCT_MANAGER
-- ===========================================
INSERT INTO team_assignments (person_id, team_id, role, start_date, is_active) VALUES
-- Alpha Team (Cycle 4)
(2, 1, 'FRONTEND', '2025-10-15', true),
(3, 1, 'BACKEND', '2025-10-15', true),
-- Beta Team (Cycle 4)
(4, 2, 'DESIGNER', '2025-10-15', true),
(5, 2, 'FULLSTACK', '2025-10-15', true),
(6, 2, 'QA', '2025-10-15', true),
-- Gamma Team (Cycle 5)
(2, 3, 'FRONTEND', '2025-12-01', true),
(5, 3, 'FULLSTACK', '2025-12-01', true),
-- Mobile Team
(3, 4, 'BACKEND', '2025-10-15', true),
(8, 4, 'BACKEND', '2025-10-15', true);

-- ===========================================
-- 7. USER-PROJECT ASSIGNMENTS
-- Valid roles: VIEWER, CONTRIBUTOR, MANAGER
-- ===========================================
INSERT INTO user_projects (user_id, project_id, project_role, created_at)
SELECT u.id, p.id, 
       CASE WHEN u.role = 'ADMIN' THEN 'MANAGER' 
            WHEN u.role = 'MANAGER' THEN 'MANAGER' 
            ELSE 'CONTRIBUTOR' END,
       CURRENT_TIMESTAMP
FROM users u
CROSS JOIN projects p
WHERE u.is_active = true AND p.is_active = true
ON CONFLICT DO NOTHING;

-- Reset sequences for all tables
SELECT setval('projects_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM projects), false);
SELECT setval('persons_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM persons), false);
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM users), false);
SELECT setval('cycles_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM cycles), false);
SELECT setval('teams_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM teams), false);
