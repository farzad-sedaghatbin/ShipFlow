-- Fix: Members who create pitches should be able to change pitch status (APPROVE permission).
-- Previously MEMBER lacked PITCH APPROVE, so clicking the status dropdown on any pitch
-- they created (or were assigned to) returned 403.
-- Also adds MEMBER EPIC UPDATE/CREATE so members can edit epics they created,
-- matching the same owner-can-edit policy already in place for pitches.

-- PITCH: grant APPROVE to MEMBER (change own pitch status: IDEA→DRAFT→SHAPED etc.)
INSERT INTO permissions (role, resource_type, permission_type, description, created_at)
SELECT 'MEMBER', 'PITCH', 'APPROVE', 'Member can change status of their own pitches', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM permissions
    WHERE role = 'MEMBER' AND resource_type = 'PITCH' AND permission_type = 'APPROVE'
);

-- EPIC: grant CREATE and UPDATE to MEMBER so members can create and edit their own epics.
-- The EpicController @PreAuthorize gates are replaced by @RequirePermission-only checks
-- (see companion backend change), making these rows the effective gate.
INSERT INTO permissions (role, resource_type, permission_type, description, created_at)
SELECT 'MEMBER', 'EPIC', 'CREATE', 'Member can create epics', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM permissions
    WHERE role = 'MEMBER' AND resource_type = 'EPIC' AND permission_type = 'CREATE'
);

INSERT INTO permissions (role, resource_type, permission_type, description, created_at)
SELECT 'MEMBER', 'EPIC', 'UPDATE', 'Member can update their own epics', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM permissions
    WHERE role = 'MEMBER' AND resource_type = 'EPIC' AND permission_type = 'UPDATE'
);
