-- Custom Fields v1.8.0: RBAC permission rows for CUSTOM_FIELD resource type

INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN',    'CUSTOM_FIELD', 'CREATE', 'Admin can create custom field definitions', CURRENT_TIMESTAMP),
('ADMIN',    'CUSTOM_FIELD', 'READ',   'Admin can read custom field definitions',   CURRENT_TIMESTAMP),
('ADMIN',    'CUSTOM_FIELD', 'UPDATE', 'Admin can update custom field definitions', CURRENT_TIMESTAMP),
('ADMIN',    'CUSTOM_FIELD', 'DELETE', 'Admin can delete custom field definitions', CURRENT_TIMESTAMP),
('MANAGER',  'CUSTOM_FIELD', 'CREATE', 'Manager can create project-scoped custom fields', CURRENT_TIMESTAMP),
('MANAGER',  'CUSTOM_FIELD', 'READ',   'Manager can read custom field definitions',       CURRENT_TIMESTAMP),
('MANAGER',  'CUSTOM_FIELD', 'UPDATE', 'Manager can update custom field definitions',     CURRENT_TIMESTAMP),
('MANAGER',  'CUSTOM_FIELD', 'DELETE', 'Manager can delete project-scoped custom fields', CURRENT_TIMESTAMP),
('MEMBER',   'CUSTOM_FIELD', 'READ',   'Member can view custom field definitions and values', CURRENT_TIMESTAMP),
('MEMBER',   'CUSTOM_FIELD', 'UPDATE', 'Member can set custom field values on entities',       CURRENT_TIMESTAMP),
('READONLY', 'CUSTOM_FIELD', 'READ',   'Read-only users can view custom field data',           CURRENT_TIMESTAMP)
ON CONFLICT (role, resource_type, permission_type) DO NOTHING;
