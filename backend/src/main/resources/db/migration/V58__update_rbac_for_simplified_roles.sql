-- V58__update_rbac_for_simplified_roles.sql
-- Update RBAC permission matrix for simplified 4-tier role model
-- Roles: ADMIN, MANAGER, MEMBER, READONLY

-- Step 1: Delete old role permissions
DELETE FROM permissions WHERE role IN ('PROJECT_MANAGER', 'PRODUCT', 'DEVELOPER', 'QA');

-- ============================================
-- MANAGER ROLE PERMISSIONS
-- ============================================
-- Combines capabilities of former PROJECT_MANAGER and PRODUCT roles

-- CYCLE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MANAGER', 'CYCLE', 'CREATE', 'Manager can create cycles', CURRENT_TIMESTAMP),
('MANAGER', 'CYCLE', 'READ', 'Manager can read cycles', CURRENT_TIMESTAMP),
('MANAGER', 'CYCLE', 'UPDATE', 'Manager can update cycles', CURRENT_TIMESTAMP),
('MANAGER', 'CYCLE', 'MANAGE', 'Manager can manage cycle settings', CURRENT_TIMESTAMP);

-- PITCH permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MANAGER', 'PITCH', 'CREATE', 'Manager can create pitches', CURRENT_TIMESTAMP),
('MANAGER', 'PITCH', 'READ', 'Manager can read pitches', CURRENT_TIMESTAMP),
('MANAGER', 'PITCH', 'UPDATE', 'Manager can update pitches', CURRENT_TIMESTAMP),
('MANAGER', 'PITCH', 'DELETE', 'Manager can delete pitches', CURRENT_TIMESTAMP),
('MANAGER', 'PITCH', 'APPROVE', 'Manager can approve/reject pitches', CURRENT_TIMESTAMP);

-- BUG permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MANAGER', 'BUG', 'CREATE', 'Manager can create bugs', CURRENT_TIMESTAMP),
('MANAGER', 'BUG', 'READ', 'Manager can read bugs', CURRENT_TIMESTAMP),
('MANAGER', 'BUG', 'UPDATE', 'Manager can update bugs', CURRENT_TIMESTAMP),
('MANAGER', 'BUG', 'DELETE', 'Manager can delete bugs', CURRENT_TIMESTAMP);

-- REPORT permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MANAGER', 'REPORT', 'CREATE', 'Manager can create reports', CURRENT_TIMESTAMP),
('MANAGER', 'REPORT', 'READ', 'Manager can read reports', CURRENT_TIMESTAMP),
('MANAGER', 'REPORT', 'EXECUTE', 'Manager can execute reports', CURRENT_TIMESTAMP);

-- PROJECT permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MANAGER', 'PROJECT', 'CREATE', 'Manager can create projects', CURRENT_TIMESTAMP),
('MANAGER', 'PROJECT', 'READ', 'Manager can read projects', CURRENT_TIMESTAMP),
('MANAGER', 'PROJECT', 'UPDATE', 'Manager can update projects', CURRENT_TIMESTAMP),
('MANAGER', 'PROJECT', 'DELETE', 'Manager can delete projects', CURRENT_TIMESTAMP);

-- TEAM permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MANAGER', 'TEAM', 'CREATE', 'Manager can create teams', CURRENT_TIMESTAMP),
('MANAGER', 'TEAM', 'READ', 'Manager can read teams', CURRENT_TIMESTAMP),
('MANAGER', 'TEAM', 'UPDATE', 'Manager can update teams', CURRENT_TIMESTAMP),
('MANAGER', 'TEAM', 'DELETE', 'Manager can delete teams', CURRENT_TIMESTAMP),
('MANAGER', 'TEAM', 'MANAGE', 'Manager can manage team assignments', CURRENT_TIMESTAMP);

-- USER permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MANAGER', 'USER', 'READ', 'Manager can read users', CURRENT_TIMESTAMP);

-- DASHBOARD permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MANAGER', 'DASHBOARD', 'CREATE', 'Manager can create dashboards', CURRENT_TIMESTAMP),
('MANAGER', 'DASHBOARD', 'READ', 'Manager can read dashboards', CURRENT_TIMESTAMP),
('MANAGER', 'DASHBOARD', 'UPDATE', 'Manager can update dashboards', CURRENT_TIMESTAMP),
('MANAGER', 'DASHBOARD', 'DELETE', 'Manager can delete dashboards', CURRENT_TIMESTAMP);

-- AI_FEATURES permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MANAGER', 'AI_FEATURES', 'READ', 'Manager can use AI features', CURRENT_TIMESTAMP),
('MANAGER', 'AI_FEATURES', 'EXECUTE', 'Manager can execute AI operations', CURRENT_TIMESTAMP);

-- RISK permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MANAGER', 'RISK', 'CREATE', 'Manager can create risks', CURRENT_TIMESTAMP),
('MANAGER', 'RISK', 'READ', 'Manager can read risks', CURRENT_TIMESTAMP),
('MANAGER', 'RISK', 'UPDATE', 'Manager can update risks', CURRENT_TIMESTAMP),
('MANAGER', 'RISK', 'DELETE', 'Manager can delete risks', CURRENT_TIMESTAMP),
('MANAGER', 'RISK', 'MANAGE', 'Manager can manage risk settings', CURRENT_TIMESTAMP);

-- RETROSPECTIVE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MANAGER', 'RETROSPECTIVE', 'CREATE', 'Manager can create retrospectives', CURRENT_TIMESTAMP),
('MANAGER', 'RETROSPECTIVE', 'READ', 'Manager can read retrospectives', CURRENT_TIMESTAMP),
('MANAGER', 'RETROSPECTIVE', 'UPDATE', 'Manager can update retrospectives', CURRENT_TIMESTAMP),
('MANAGER', 'RETROSPECTIVE', 'DELETE', 'Manager can delete retrospectives', CURRENT_TIMESTAMP),
('MANAGER', 'RETROSPECTIVE', 'MANAGE', 'Manager can manage retrospectives', CURRENT_TIMESTAMP);

-- BETTING_TABLE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MANAGER', 'BETTING_TABLE', 'CREATE', 'Manager can create betting slots', CURRENT_TIMESTAMP),
('MANAGER', 'BETTING_TABLE', 'READ', 'Manager can read betting table', CURRENT_TIMESTAMP),
('MANAGER', 'BETTING_TABLE', 'UPDATE', 'Manager can update betting slots', CURRENT_TIMESTAMP),
('MANAGER', 'BETTING_TABLE', 'DELETE', 'Manager can delete betting slots', CURRENT_TIMESTAMP),
('MANAGER', 'BETTING_TABLE', 'MANAGE', 'Manager can manage betting table', CURRENT_TIMESTAMP);

-- ============================================
-- MEMBER ROLE PERMISSIONS
-- ============================================
-- Combines capabilities of former DEVELOPER, QA, and contributor roles

-- CYCLE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MEMBER', 'CYCLE', 'READ', 'Member can read cycles', CURRENT_TIMESTAMP);

-- PITCH permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MEMBER', 'PITCH', 'CREATE', 'Member can create pitches', CURRENT_TIMESTAMP),
('MEMBER', 'PITCH', 'READ', 'Member can read pitches', CURRENT_TIMESTAMP),
('MEMBER', 'PITCH', 'UPDATE', 'Member can update own pitches', CURRENT_TIMESTAMP);

-- BUG permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MEMBER', 'BUG', 'CREATE', 'Member can create bugs', CURRENT_TIMESTAMP),
('MEMBER', 'BUG', 'READ', 'Member can read bugs', CURRENT_TIMESTAMP),
('MEMBER', 'BUG', 'UPDATE', 'Member can update bugs', CURRENT_TIMESTAMP);

-- REPORT permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MEMBER', 'REPORT', 'READ', 'Member can read reports', CURRENT_TIMESTAMP);

-- PROJECT permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MEMBER', 'PROJECT', 'READ', 'Member can read projects', CURRENT_TIMESTAMP);

-- TEAM permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MEMBER', 'TEAM', 'READ', 'Member can read teams', CURRENT_TIMESTAMP);

-- USER permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MEMBER', 'USER', 'READ', 'Member can read users', CURRENT_TIMESTAMP);

-- DASHBOARD permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MEMBER', 'DASHBOARD', 'CREATE', 'Member can create own dashboards', CURRENT_TIMESTAMP),
('MEMBER', 'DASHBOARD', 'READ', 'Member can read dashboards', CURRENT_TIMESTAMP),
('MEMBER', 'DASHBOARD', 'UPDATE', 'Member can update own dashboards', CURRENT_TIMESTAMP),
('MEMBER', 'DASHBOARD', 'DELETE', 'Member can delete own dashboards', CURRENT_TIMESTAMP);

-- AI_FEATURES permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MEMBER', 'AI_FEATURES', 'READ', 'Member can use AI features', CURRENT_TIMESTAMP),
('MEMBER', 'AI_FEATURES', 'EXECUTE', 'Member can execute AI operations', CURRENT_TIMESTAMP);

-- RISK permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MEMBER', 'RISK', 'READ', 'Member can read risks', CURRENT_TIMESTAMP);

-- RETROSPECTIVE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MEMBER', 'RETROSPECTIVE', 'CREATE', 'Member can create retrospectives', CURRENT_TIMESTAMP),
('MEMBER', 'RETROSPECTIVE', 'READ', 'Member can read retrospectives', CURRENT_TIMESTAMP),
('MEMBER', 'RETROSPECTIVE', 'UPDATE', 'Member can update retrospectives', CURRENT_TIMESTAMP);

-- BETTING_TABLE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MEMBER', 'BETTING_TABLE', 'READ', 'Member can read betting table', CURRENT_TIMESTAMP);

-- ============================================
-- READONLY ROLE PERMISSIONS
-- ============================================
-- Read-only observer access to all resources

-- CYCLE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('READONLY', 'CYCLE', 'READ', 'Readonly can read cycles', CURRENT_TIMESTAMP);

-- PITCH permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('READONLY', 'PITCH', 'READ', 'Readonly can read pitches', CURRENT_TIMESTAMP);

-- BUG permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('READONLY', 'BUG', 'READ', 'Readonly can read bugs', CURRENT_TIMESTAMP);

-- REPORT permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('READONLY', 'REPORT', 'READ', 'Readonly can read reports', CURRENT_TIMESTAMP);

-- PROJECT permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('READONLY', 'PROJECT', 'READ', 'Readonly can read projects', CURRENT_TIMESTAMP);

-- TEAM permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('READONLY', 'TEAM', 'READ', 'Readonly can read teams', CURRENT_TIMESTAMP);

-- USER permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('READONLY', 'USER', 'READ', 'Readonly can read users', CURRENT_TIMESTAMP);

-- DASHBOARD permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('READONLY', 'DASHBOARD', 'READ', 'Readonly can read dashboards', CURRENT_TIMESTAMP);

-- RISK permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('READONLY', 'RISK', 'READ', 'Readonly can read risks', CURRENT_TIMESTAMP);

-- RETROSPECTIVE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('READONLY', 'RETROSPECTIVE', 'READ', 'Readonly can read retrospectives', CURRENT_TIMESTAMP);

-- BETTING_TABLE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('READONLY', 'BETTING_TABLE', 'READ', 'Readonly can read betting table', CURRENT_TIMESTAMP);
