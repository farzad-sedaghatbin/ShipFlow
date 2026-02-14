-- V87: Add permissions for roadmap features (Initiatives, Epics, Releases)
-- Assigns CREATE, READ, UPDATE, DELETE permissions to ADMIN and MANAGER roles

-- INITIATIVE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'INITIATIVE', 'CREATE', 'Admin can create initiatives', CURRENT_TIMESTAMP),
('ADMIN', 'INITIATIVE', 'READ', 'Admin can read initiatives', CURRENT_TIMESTAMP),
('ADMIN', 'INITIATIVE', 'UPDATE', 'Admin can update initiatives', CURRENT_TIMESTAMP),
('ADMIN', 'INITIATIVE', 'DELETE', 'Admin can delete initiatives', CURRENT_TIMESTAMP),
('MANAGER', 'INITIATIVE', 'CREATE', 'Manager can create initiatives', CURRENT_TIMESTAMP),
('MANAGER', 'INITIATIVE', 'READ', 'Manager can read initiatives', CURRENT_TIMESTAMP),
('MANAGER', 'INITIATIVE', 'UPDATE', 'Manager can update initiatives', CURRENT_TIMESTAMP),
('MANAGER', 'INITIATIVE', 'DELETE', 'Manager can delete initiatives', CURRENT_TIMESTAMP)
ON CONFLICT (role, resource_type, permission_type) DO NOTHING;

-- EPIC permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'EPIC', 'CREATE', 'Admin can create epics', CURRENT_TIMESTAMP),
('ADMIN', 'EPIC', 'READ', 'Admin can read epics', CURRENT_TIMESTAMP),
('ADMIN', 'EPIC', 'UPDATE', 'Admin can update epics', CURRENT_TIMESTAMP),
('ADMIN', 'EPIC', 'DELETE', 'Admin can delete epics', CURRENT_TIMESTAMP),
('MANAGER', 'EPIC', 'CREATE', 'Manager can create epics', CURRENT_TIMESTAMP),
('MANAGER', 'EPIC', 'READ', 'Manager can read epics', CURRENT_TIMESTAMP),
('MANAGER', 'EPIC', 'UPDATE', 'Manager can update epics', CURRENT_TIMESTAMP),
('MANAGER', 'EPIC', 'DELETE', 'Manager can delete epics', CURRENT_TIMESTAMP)
ON CONFLICT (role, resource_type, permission_type) DO NOTHING;

-- RELEASE permissions
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'RELEASE', 'CREATE', 'Admin can create releases', CURRENT_TIMESTAMP),
('ADMIN', 'RELEASE', 'READ', 'Admin can read releases', CURRENT_TIMESTAMP),
('ADMIN', 'RELEASE', 'UPDATE', 'Admin can update releases', CURRENT_TIMESTAMP),
('ADMIN', 'RELEASE', 'DELETE', 'Admin can delete releases', CURRENT_TIMESTAMP),
('MANAGER', 'RELEASE', 'CREATE', 'Manager can create releases', CURRENT_TIMESTAMP),
('MANAGER', 'RELEASE', 'READ', 'Manager can read releases', CURRENT_TIMESTAMP),
('MANAGER', 'RELEASE', 'UPDATE', 'Manager can update releases', CURRENT_TIMESTAMP),
('MANAGER', 'RELEASE', 'DELETE', 'Manager can delete releases', CURRENT_TIMESTAMP)
ON CONFLICT (role, resource_type, permission_type) DO NOTHING;
