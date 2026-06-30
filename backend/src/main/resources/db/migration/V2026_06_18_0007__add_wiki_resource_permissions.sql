-- V2026_06_18_0007__add_wiki_resource_permissions.sql
-- Seed WIKI resource permissions for all roles
-- Pattern: mirrors V58__update_rbac_for_simplified_roles.sql
-- Columns: role VARCHAR, resource_type VARCHAR, permission_type VARCHAR, description VARCHAR(500), created_at TIMESTAMP

-- ADMIN — full access
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'WIKI', 'CREATE', 'Admin can create wiki spaces and pages', CURRENT_TIMESTAMP),
('ADMIN', 'WIKI', 'READ', 'Admin can read wiki content', CURRENT_TIMESTAMP),
('ADMIN', 'WIKI', 'UPDATE', 'Admin can update wiki content', CURRENT_TIMESTAMP),
('ADMIN', 'WIKI', 'DELETE', 'Admin can delete wiki spaces and pages', CURRENT_TIMESTAMP),
('ADMIN', 'WIKI', 'MANAGE', 'Admin can manage wiki settings and permissions', CURRENT_TIMESTAMP);

-- MANAGER — full access
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MANAGER', 'WIKI', 'CREATE', 'Manager can create wiki spaces and pages', CURRENT_TIMESTAMP),
('MANAGER', 'WIKI', 'READ', 'Manager can read wiki content', CURRENT_TIMESTAMP),
('MANAGER', 'WIKI', 'UPDATE', 'Manager can update wiki content', CURRENT_TIMESTAMP),
('MANAGER', 'WIKI', 'DELETE', 'Manager can delete wiki content', CURRENT_TIMESTAMP),
('MANAGER', 'WIKI', 'MANAGE', 'Manager can manage wiki space permissions', CURRENT_TIMESTAMP);

-- MEMBER — create/read/update
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('MEMBER', 'WIKI', 'CREATE', 'Member can create wiki pages', CURRENT_TIMESTAMP),
('MEMBER', 'WIKI', 'READ', 'Member can read wiki content', CURRENT_TIMESTAMP),
('MEMBER', 'WIKI', 'UPDATE', 'Member can update wiki pages', CURRENT_TIMESTAMP);

-- READONLY — read only
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('READONLY', 'WIKI', 'READ', 'Readonly can read wiki content', CURRENT_TIMESTAMP);
