-- V60: Create user_projects table for project-level access control
-- Users can access projects through:
-- 1. Being the project owner (owner_id in projects table)
-- 2. Team membership (via team_assignments → teams → cycles → projects)
-- 3. Direct assignment via this table
-- 4. Global ADMIN role (bypasses all checks)

CREATE TABLE user_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    project_role VARCHAR(50) NOT NULL DEFAULT 'VIEWER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by_user_id BIGINT,
    
    CONSTRAINT fk_user_projects_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_projects_project 
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_projects_granted_by 
        FOREIGN KEY (granted_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_user_project 
        UNIQUE (user_id, project_id),
    CONSTRAINT chk_project_role 
        CHECK (project_role IN ('VIEWER', 'CONTRIBUTOR', 'MANAGER'))
);

-- Index for fast lookups by user
CREATE INDEX idx_user_projects_user_id ON user_projects(user_id);

-- Index for fast lookups by project
CREATE INDEX idx_user_projects_project_id ON user_projects(project_id);

-- Migrate existing data: Grant MANAGER access to project owners
INSERT INTO user_projects (user_id, project_id, project_role, created_at)
SELECT p.owner_id, p.id, 'MANAGER', NOW()
FROM projects p
WHERE p.owner_id IS NOT NULL
ON DUPLICATE KEY UPDATE project_role = 'MANAGER';

-- Grant access to users who are already team members in project cycles
-- This ensures existing team members don't lose access
INSERT INTO user_projects (user_id, project_id, project_role, created_at)
SELECT DISTINCT u.id, p.id, 'CONTRIBUTOR', NOW()
FROM users u
JOIN persons per ON u.person_id = per.id
JOIN team_assignments ta ON ta.person_id = per.id
JOIN teams t ON ta.team_id = t.id
JOIN cycles c ON t.cycle_id = c.id
JOIN projects p ON c.project_id = p.id
WHERE u.id IS NOT NULL AND p.id IS NOT NULL
ON DUPLICATE KEY UPDATE id = id;  -- Keep existing role if already assigned
