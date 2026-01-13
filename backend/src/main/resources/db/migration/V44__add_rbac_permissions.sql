-- V44__add_rbac_permissions.sql
-- Add RBAC (Role-Based Access Control) permission system

-- Create permissions table
CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    permission_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_permission UNIQUE (role, resource_type, permission_type)
);

-- Create index for faster permission lookups
CREATE INDEX idx_permissions_role ON permissions(role);
CREATE INDEX idx_permissions_resource ON permissions(resource_type);

-- Insert default permissions for ADMIN role (full access)
-- CYCLE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'CYCLE', 'CREATE', 'Admin can create cycles', CURRENT_TIMESTAMP),
('ADMIN', 'CYCLE', 'READ', 'Admin can read cycles', CURRENT_TIMESTAMP),
('ADMIN', 'CYCLE', 'UPDATE', 'Admin can update cycles', CURRENT_TIMESTAMP),
('ADMIN', 'CYCLE', 'DELETE', 'Admin can delete cycles', CURRENT_TIMESTAMP),
('ADMIN', 'CYCLE', 'MANAGE', 'Admin can manage cycle settings', CURRENT_TIMESTAMP);

-- PITCH permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'PITCH', 'CREATE', 'Admin can create pitches', CURRENT_TIMESTAMP),
('ADMIN', 'PITCH', 'READ', 'Admin can read pitches', CURRENT_TIMESTAMP),
('ADMIN', 'PITCH', 'UPDATE', 'Admin can update pitches', CURRENT_TIMESTAMP),
('ADMIN', 'PITCH', 'DELETE', 'Admin can delete pitches', CURRENT_TIMESTAMP),
('ADMIN', 'PITCH', 'APPROVE', 'Admin can approve/reject pitches', CURRENT_TIMESTAMP);

-- BUG permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'BUG', 'CREATE', 'Admin can create bugs', CURRENT_TIMESTAMP),
('ADMIN', 'BUG', 'READ', 'Admin can read bugs', CURRENT_TIMESTAMP),
('ADMIN', 'BUG', 'UPDATE', 'Admin can update bugs', CURRENT_TIMESTAMP),
('ADMIN', 'BUG', 'DELETE', 'Admin can delete bugs', CURRENT_TIMESTAMP);

-- REPORT permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'REPORT', 'CREATE', 'Admin can create reports', CURRENT_TIMESTAMP),
('ADMIN', 'REPORT', 'READ', 'Admin can read reports', CURRENT_TIMESTAMP),
('ADMIN', 'REPORT', 'EXECUTE', 'Admin can execute reports', CURRENT_TIMESTAMP);

-- PROJECT permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'PROJECT', 'CREATE', 'Admin can create projects', CURRENT_TIMESTAMP),
('ADMIN', 'PROJECT', 'READ', 'Admin can read projects', CURRENT_TIMESTAMP),
('ADMIN', 'PROJECT', 'UPDATE', 'Admin can update projects', CURRENT_TIMESTAMP),
('ADMIN', 'PROJECT', 'DELETE', 'Admin can delete projects', CURRENT_TIMESTAMP),
('ADMIN', 'PROJECT', 'MANAGE', 'Admin can manage project settings', CURRENT_TIMESTAMP);

-- TEAM permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'TEAM', 'CREATE', 'Admin can create teams', CURRENT_TIMESTAMP),
('ADMIN', 'TEAM', 'READ', 'Admin can read teams', CURRENT_TIMESTAMP),
('ADMIN', 'TEAM', 'UPDATE', 'Admin can update teams', CURRENT_TIMESTAMP),
('ADMIN', 'TEAM', 'DELETE', 'Admin can delete teams', CURRENT_TIMESTAMP),
('ADMIN', 'TEAM', 'MANAGE', 'Admin can manage team assignments', CURRENT_TIMESTAMP);

-- USER permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'USER', 'CREATE', 'Admin can create users', CURRENT_TIMESTAMP),
('ADMIN', 'USER', 'READ', 'Admin can read users', CURRENT_TIMESTAMP),
('ADMIN', 'USER', 'UPDATE', 'Admin can update users', CURRENT_TIMESTAMP),
('ADMIN', 'USER', 'DELETE', 'Admin can delete users', CURRENT_TIMESTAMP),
('ADMIN', 'USER', 'MANAGE', 'Admin can manage user roles', CURRENT_TIMESTAMP);

-- DASHBOARD permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'DASHBOARD', 'CREATE', 'Admin can create dashboards', CURRENT_TIMESTAMP),
('ADMIN', 'DASHBOARD', 'READ', 'Admin can read dashboards', CURRENT_TIMESTAMP),
('ADMIN', 'DASHBOARD', 'UPDATE', 'Admin can update dashboards', CURRENT_TIMESTAMP),
('ADMIN', 'DASHBOARD', 'DELETE', 'Admin can delete dashboards', CURRENT_TIMESTAMP);

-- AI_FEATURES permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'AI_FEATURES', 'READ', 'Admin can use AI features', CURRENT_TIMESTAMP),
('ADMIN', 'AI_FEATURES', 'EXECUTE', 'Admin can execute AI operations', CURRENT_TIMESTAMP),
('ADMIN', 'AI_FEATURES', 'MANAGE', 'Admin can manage AI settings', CURRENT_TIMESTAMP);

-- SYSTEM permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'SYSTEM', 'MANAGE', 'Admin can manage system settings', CURRENT_TIMESTAMP);

-- Insert permissions for PROJECT_MANAGER role
-- CYCLE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('PROJECT_MANAGER', 'CYCLE', 'CREATE', 'PM can create cycles', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'CYCLE', 'READ', 'PM can read cycles', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'CYCLE', 'UPDATE', 'PM can update cycles', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'CYCLE', 'MANAGE', 'PM can manage cycle settings', CURRENT_TIMESTAMP);

-- PITCH permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('PROJECT_MANAGER', 'PITCH', 'CREATE', 'PM can create pitches', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'PITCH', 'READ', 'PM can read pitches', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'PITCH', 'UPDATE', 'PM can update pitches', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'PITCH', 'DELETE', 'PM can delete pitches', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'PITCH', 'APPROVE', 'PM can approve/reject pitches', CURRENT_TIMESTAMP);

-- BUG permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('PROJECT_MANAGER', 'BUG', 'READ', 'PM can read bugs', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'BUG', 'UPDATE', 'PM can update bugs', CURRENT_TIMESTAMP);

-- REPORT permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('PROJECT_MANAGER', 'REPORT', 'CREATE', 'PM can create reports', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'REPORT', 'READ', 'PM can read reports', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'REPORT', 'EXECUTE', 'PM can execute reports', CURRENT_TIMESTAMP);

-- PROJECT permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('PROJECT_MANAGER', 'PROJECT', 'READ', 'PM can read projects', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'PROJECT', 'UPDATE', 'PM can update projects', CURRENT_TIMESTAMP);

-- TEAM permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('PROJECT_MANAGER', 'TEAM', 'READ', 'PM can read teams', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'TEAM', 'UPDATE', 'PM can update teams', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'TEAM', 'MANAGE', 'PM can manage team assignments', CURRENT_TIMESTAMP);

-- DASHBOARD permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('PROJECT_MANAGER', 'DASHBOARD', 'CREATE', 'PM can create dashboards', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'DASHBOARD', 'READ', 'PM can read dashboards', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'DASHBOARD', 'UPDATE', 'PM can update dashboards', CURRENT_TIMESTAMP);

-- AI_FEATURES permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('PROJECT_MANAGER', 'AI_FEATURES', 'READ', 'PM can use AI features', CURRENT_TIMESTAMP),
('PROJECT_MANAGER', 'AI_FEATURES', 'EXECUTE', 'PM can execute AI operations', CURRENT_TIMESTAMP);

-- Insert permissions for PRODUCT role
-- PITCH permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('PRODUCT', 'PITCH', 'CREATE', 'Product can create pitches', CURRENT_TIMESTAMP),
('PRODUCT', 'PITCH', 'READ', 'Product can read pitches', CURRENT_TIMESTAMP),
('PRODUCT', 'PITCH', 'UPDATE', 'Product can update pitches', CURRENT_TIMESTAMP);

-- CYCLE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('PRODUCT', 'CYCLE', 'READ', 'Product can read cycles', CURRENT_TIMESTAMP);

-- BUG permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('PRODUCT', 'BUG', 'CREATE', 'Product can create bugs', CURRENT_TIMESTAMP),
('PRODUCT', 'BUG', 'READ', 'Product can read bugs', CURRENT_TIMESTAMP),
('PRODUCT', 'BUG', 'UPDATE', 'Product can update bugs', CURRENT_TIMESTAMP);

-- REPORT permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('PRODUCT', 'REPORT', 'READ', 'Product can read reports', CURRENT_TIMESTAMP);

-- DASHBOARD permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('PRODUCT', 'DASHBOARD', 'READ', 'Product can read dashboards', CURRENT_TIMESTAMP),
('PRODUCT', 'DASHBOARD', 'CREATE', 'Product can create dashboards', CURRENT_TIMESTAMP);

-- AI_FEATURES permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('PRODUCT', 'AI_FEATURES', 'READ', 'Product can use AI features', CURRENT_TIMESTAMP);

-- Insert permissions for DEVELOPER role
-- PITCH permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('DEVELOPER', 'PITCH', 'READ', 'Developer can read pitches', CURRENT_TIMESTAMP);

-- CYCLE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('DEVELOPER', 'CYCLE', 'READ', 'Developer can read cycles', CURRENT_TIMESTAMP);

-- BUG permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('DEVELOPER', 'BUG', 'CREATE', 'Developer can create bugs', CURRENT_TIMESTAMP),
('DEVELOPER', 'BUG', 'READ', 'Developer can read bugs', CURRENT_TIMESTAMP),
('DEVELOPER', 'BUG', 'UPDATE', 'Developer can update bugs', CURRENT_TIMESTAMP);

-- REPORT permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('DEVELOPER', 'REPORT', 'READ', 'Developer can read reports', CURRENT_TIMESTAMP);

-- DASHBOARD permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('DEVELOPER', 'DASHBOARD', 'READ', 'Developer can read dashboards', CURRENT_TIMESTAMP);

-- AI_FEATURES permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('DEVELOPER', 'AI_FEATURES', 'READ', 'Developer can use AI features', CURRENT_TIMESTAMP);

-- Insert permissions for QA role
-- PITCH permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('QA', 'PITCH', 'READ', 'QA can read pitches', CURRENT_TIMESTAMP);

-- CYCLE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('QA', 'CYCLE', 'READ', 'QA can read cycles', CURRENT_TIMESTAMP);

-- BUG permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('QA', 'BUG', 'CREATE', 'QA can create bugs', CURRENT_TIMESTAMP),
('QA', 'BUG', 'READ', 'QA can read bugs', CURRENT_TIMESTAMP),
('QA', 'BUG', 'UPDATE', 'QA can update bugs', CURRENT_TIMESTAMP),
('QA', 'BUG', 'DELETE', 'QA can delete bugs', CURRENT_TIMESTAMP);

-- REPORT permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('QA', 'REPORT', 'READ', 'QA can read reports', CURRENT_TIMESTAMP),
('QA', 'REPORT', 'CREATE', 'QA can create reports', CURRENT_TIMESTAMP);

-- DASHBOARD permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('QA', 'DASHBOARD', 'READ', 'QA can read dashboards', CURRENT_TIMESTAMP);

-- AI_FEATURES permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('QA', 'AI_FEATURES', 'READ', 'QA can use AI features', CURRENT_TIMESTAMP);
