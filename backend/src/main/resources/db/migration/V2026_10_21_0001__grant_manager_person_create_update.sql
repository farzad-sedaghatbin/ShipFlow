-- Let MANAGERs add and edit people.
--
-- V58 gave MANAGER only USER:READ ("Manager can read users"). The USER resource is checked in
-- exactly two places — PersonController's create (USER:CREATE) and update (USER:UPDATE) — so a
-- manager could open the People page but every "Add Person" / "Edit" was refused with
-- "You don't have permission to perform this action". Managing LOGIN ACCOUNTS is a separate,
-- ADMIN-only rule (UserController, and the admin carve-out in UserService.createUser) and is
-- deliberately not touched here.
--
-- Idempotent: permissions has a unique (role, resource_type, permission_type) constraint, so
-- re-running, or an installation where an admin already added these rows, is a no-op.

INSERT INTO permissions (role, resource_type, permission_type, description, created_at)
SELECT 'MANAGER', 'USER', 'CREATE', 'Manager can create people', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions
    WHERE role = 'MANAGER' AND resource_type = 'USER' AND permission_type = 'CREATE'
);

INSERT INTO permissions (role, resource_type, permission_type, description, created_at)
SELECT 'MANAGER', 'USER', 'UPDATE', 'Manager can update people', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM permissions
    WHERE role = 'MANAGER' AND resource_type = 'USER' AND permission_type = 'UPDATE'
);
