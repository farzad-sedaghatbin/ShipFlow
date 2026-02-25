-- Add permissions for resources that were missing from the RBAC matrix
-- Covers: BACKLOG, WORKLOG, MEETING, METRIC, TEST_CASE, INTEGRATION, WISE_ARCHITECTURE
-- Also adds MEMBER/READONLY READ permissions for INITIATIVE, EPIC, RELEASE

-- INITIATIVE: add MEMBER and READONLY read access (V87 only granted ADMIN+MANAGER)
INSERT INTO permissions (role, resource_type, permission_type, description, created_at)
SELECT 'MEMBER', 'INITIATIVE', 'READ', 'Members can view initiatives', NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE role='MEMBER' AND resource_type='INITIATIVE' AND permission_type='READ');
INSERT INTO permissions (role, resource_type, permission_type, description, created_at)
SELECT 'READONLY', 'INITIATIVE', 'READ', 'Read-only users can view initiatives', NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE role='READONLY' AND resource_type='INITIATIVE' AND permission_type='READ');

-- EPIC: add MEMBER and READONLY read access
INSERT INTO permissions (role, resource_type, permission_type, description, created_at)
SELECT 'MEMBER', 'EPIC', 'READ', 'Members can view epics', NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE role='MEMBER' AND resource_type='EPIC' AND permission_type='READ');
INSERT INTO permissions (role, resource_type, permission_type, description, created_at)
SELECT 'READONLY', 'EPIC', 'READ', 'Read-only users can view epics', NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE role='READONLY' AND resource_type='EPIC' AND permission_type='READ');

-- RELEASE: add MEMBER and READONLY read access
INSERT INTO permissions (role, resource_type, permission_type, description, created_at)
SELECT 'MEMBER', 'RELEASE', 'READ', 'Members can view releases', NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE role='MEMBER' AND resource_type='RELEASE' AND permission_type='READ');
INSERT INTO permissions (role, resource_type, permission_type, description, created_at)
SELECT 'READONLY', 'RELEASE', 'READ', 'Read-only users can view releases', NOW()
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE role='READONLY' AND resource_type='RELEASE' AND permission_type='READ');

-- BACKLOG (Task management)
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'BACKLOG', 'CREATE', 'Admins can create tasks', NOW()),
('ADMIN', 'BACKLOG', 'READ', 'Admins can view tasks', NOW()),
('ADMIN', 'BACKLOG', 'UPDATE', 'Admins can update tasks', NOW()),
('ADMIN', 'BACKLOG', 'DELETE', 'Admins can delete tasks', NOW()),
('MANAGER', 'BACKLOG', 'CREATE', 'Managers can create tasks', NOW()),
('MANAGER', 'BACKLOG', 'READ', 'Managers can view tasks', NOW()),
('MANAGER', 'BACKLOG', 'UPDATE', 'Managers can update tasks', NOW()),
('MANAGER', 'BACKLOG', 'DELETE', 'Managers can delete tasks', NOW()),
('MEMBER', 'BACKLOG', 'CREATE', 'Members can create tasks', NOW()),
('MEMBER', 'BACKLOG', 'READ', 'Members can view tasks', NOW()),
('MEMBER', 'BACKLOG', 'UPDATE', 'Members can update own tasks', NOW()),
('READONLY', 'BACKLOG', 'READ', 'Read-only users can view tasks', NOW())
ON CONFLICT DO NOTHING;

-- WORKLOG (Time tracking)
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'WORKLOG', 'CREATE', 'Admins can create work logs', NOW()),
('ADMIN', 'WORKLOG', 'READ', 'Admins can view work logs', NOW()),
('ADMIN', 'WORKLOG', 'UPDATE', 'Admins can update work logs', NOW()),
('ADMIN', 'WORKLOG', 'DELETE', 'Admins can delete work logs', NOW()),
('ADMIN', 'WORKLOG', 'MANAGE', 'Admins can manage team work logs', NOW()),
('MANAGER', 'WORKLOG', 'CREATE', 'Managers can create work logs', NOW()),
('MANAGER', 'WORKLOG', 'READ', 'Managers can view work logs', NOW()),
('MANAGER', 'WORKLOG', 'UPDATE', 'Managers can update work logs', NOW()),
('MANAGER', 'WORKLOG', 'DELETE', 'Managers can delete work logs', NOW()),
('MANAGER', 'WORKLOG', 'MANAGE', 'Managers can manage team work logs', NOW()),
('MEMBER', 'WORKLOG', 'CREATE', 'Members can log own work', NOW()),
('MEMBER', 'WORKLOG', 'READ', 'Members can view work logs', NOW()),
('MEMBER', 'WORKLOG', 'UPDATE', 'Members can update own work logs', NOW()),
('MEMBER', 'WORKLOG', 'DELETE', 'Members can delete own work logs', NOW()),
('READONLY', 'WORKLOG', 'READ', 'Read-only users can view work logs', NOW())
ON CONFLICT DO NOTHING;

-- MEETING
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'MEETING', 'CREATE', 'Admins can create meetings', NOW()),
('ADMIN', 'MEETING', 'READ', 'Admins can view meetings', NOW()),
('ADMIN', 'MEETING', 'UPDATE', 'Admins can update meetings', NOW()),
('ADMIN', 'MEETING', 'DELETE', 'Admins can delete meetings', NOW()),
('MANAGER', 'MEETING', 'CREATE', 'Managers can create meetings', NOW()),
('MANAGER', 'MEETING', 'READ', 'Managers can view meetings', NOW()),
('MANAGER', 'MEETING', 'UPDATE', 'Managers can update meetings', NOW()),
('MANAGER', 'MEETING', 'DELETE', 'Managers can delete meetings', NOW()),
('MEMBER', 'MEETING', 'CREATE', 'Members can create meetings', NOW()),
('MEMBER', 'MEETING', 'READ', 'Members can view meetings', NOW()),
('MEMBER', 'MEETING', 'UPDATE', 'Members can update own meetings', NOW()),
('READONLY', 'MEETING', 'READ', 'Read-only users can view meetings', NOW())
ON CONFLICT DO NOTHING;

-- METRIC (Custom Metrics)
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'METRIC', 'CREATE', 'Admins can create metrics', NOW()),
('ADMIN', 'METRIC', 'READ', 'Admins can view metrics', NOW()),
('ADMIN', 'METRIC', 'UPDATE', 'Admins can update metrics', NOW()),
('ADMIN', 'METRIC', 'DELETE', 'Admins can delete metrics', NOW()),
('MANAGER', 'METRIC', 'CREATE', 'Managers can create metrics', NOW()),
('MANAGER', 'METRIC', 'READ', 'Managers can view metrics', NOW()),
('MANAGER', 'METRIC', 'UPDATE', 'Managers can update metrics', NOW()),
('MANAGER', 'METRIC', 'DELETE', 'Managers can delete metrics', NOW()),
('MEMBER', 'METRIC', 'READ', 'Members can view metrics', NOW()),
('READONLY', 'METRIC', 'READ', 'Read-only users can view metrics', NOW())
ON CONFLICT DO NOTHING;

-- TEST_CASE (QA Test Cases)
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'TEST_CASE', 'CREATE', 'Admins can create test cases', NOW()),
('ADMIN', 'TEST_CASE', 'READ', 'Admins can view test cases', NOW()),
('ADMIN', 'TEST_CASE', 'UPDATE', 'Admins can update test cases', NOW()),
('ADMIN', 'TEST_CASE', 'DELETE', 'Admins can delete test cases', NOW()),
('ADMIN', 'TEST_CASE', 'EXECUTE', 'Admins can execute test runs', NOW()),
('MANAGER', 'TEST_CASE', 'CREATE', 'Managers can create test cases', NOW()),
('MANAGER', 'TEST_CASE', 'READ', 'Managers can view test cases', NOW()),
('MANAGER', 'TEST_CASE', 'UPDATE', 'Managers can update test cases', NOW()),
('MANAGER', 'TEST_CASE', 'DELETE', 'Managers can delete test cases', NOW()),
('MANAGER', 'TEST_CASE', 'EXECUTE', 'Managers can execute test runs', NOW()),
('MEMBER', 'TEST_CASE', 'CREATE', 'Members can create test cases', NOW()),
('MEMBER', 'TEST_CASE', 'READ', 'Members can view test cases', NOW()),
('MEMBER', 'TEST_CASE', 'UPDATE', 'Members can update test cases', NOW()),
('MEMBER', 'TEST_CASE', 'EXECUTE', 'Members can execute test runs', NOW()),
('READONLY', 'TEST_CASE', 'READ', 'Read-only users can view test cases', NOW())
ON CONFLICT DO NOTHING;

-- INTEGRATION
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'INTEGRATION', 'CREATE', 'Admins can create integrations', NOW()),
('ADMIN', 'INTEGRATION', 'READ', 'Admins can view integrations', NOW()),
('ADMIN', 'INTEGRATION', 'UPDATE', 'Admins can update integrations', NOW()),
('ADMIN', 'INTEGRATION', 'DELETE', 'Admins can delete integrations', NOW()),
('ADMIN', 'INTEGRATION', 'MANAGE', 'Admins can manage integrations', NOW()),
('MANAGER', 'INTEGRATION', 'READ', 'Managers can view integrations', NOW()),
('MEMBER', 'INTEGRATION', 'READ', 'Members can view integrations', NOW())
ON CONFLICT DO NOTHING;

-- WISE_ARCHITECTURE (R&D)
INSERT INTO permissions (role, resource_type, permission_type, description, created_at) VALUES
('ADMIN', 'WISE_ARCHITECTURE', 'READ', 'Admins can view WISE architecture', NOW()),
('ADMIN', 'WISE_ARCHITECTURE', 'EXECUTE', 'Admins can run architecture analysis', NOW()),
('ADMIN', 'WISE_ARCHITECTURE', 'MANAGE', 'Admins can manage WISE architecture', NOW()),
('MANAGER', 'WISE_ARCHITECTURE', 'READ', 'Managers can view WISE architecture', NOW()),
('MANAGER', 'WISE_ARCHITECTURE', 'EXECUTE', 'Managers can run architecture analysis', NOW()),
('MEMBER', 'WISE_ARCHITECTURE', 'READ', 'Members can view WISE architecture', NOW()),
('MEMBER', 'WISE_ARCHITECTURE', 'EXECUTE', 'Members can run architecture analysis', NOW())
ON CONFLICT DO NOTHING;
