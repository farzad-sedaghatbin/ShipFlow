-- V57__migrate_to_simplified_roles.sql
-- Migrate from 5-tier role model (ADMIN, PROJECT_MANAGER, PRODUCT, DEVELOPER, QA)
-- to 4-tier model (ADMIN, MANAGER, MEMBER, READONLY)

-- Step 1: Add temporary column to track old roles
ALTER TABLE users ADD COLUMN old_role VARCHAR(50);

-- Step 2: Backup existing roles
UPDATE users SET old_role = role;

-- Step 3: Migrate roles according to mapping:
-- ADMIN -> ADMIN (unchanged)
-- PROJECT_MANAGER -> MANAGER
-- PRODUCT -> MANAGER (product managers have similar authority)
-- DEVELOPER -> MEMBER
-- QA -> MEMBER

UPDATE users SET role = 'MANAGER' WHERE role = 'PROJECT_MANAGER';
UPDATE users SET role = 'MANAGER' WHERE role = 'PRODUCT';
UPDATE users SET role = 'MEMBER' WHERE role = 'DEVELOPER';
UPDATE users SET role = 'MEMBER' WHERE role = 'QA';

-- Note: old_role column preserved for audit trail
-- Can be dropped in future migration once migration is verified
