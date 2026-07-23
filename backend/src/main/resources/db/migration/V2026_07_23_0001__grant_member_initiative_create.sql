-- Grant MEMBER role permission to create initiatives (previously ADMIN/MANAGER only, per V87)
INSERT INTO permissions (role, resource_type, permission_type, description, created_at)
SELECT 'MEMBER', 'INITIATIVE', 'CREATE', 'Members can create initiatives', NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE role='MEMBER' AND resource_type='INITIATIVE' AND permission_type='CREATE');
