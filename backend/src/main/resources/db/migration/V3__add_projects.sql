-- Projects table
CREATE TABLE projects (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    project_key VARCHAR(10) NOT NULL UNIQUE,
    description TEXT,
    color VARCHAR(20),
    logo_url VARCHAR(500),
    owner_id BIGINT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Add project_id to cycles table
ALTER TABLE cycles ADD COLUMN project_id BIGINT;
ALTER TABLE cycles ADD CONSTRAINT fk_cycles_project FOREIGN KEY (project_id) REFERENCES projects(id);

-- Create index for better query performance
CREATE INDEX idx_cycles_project_id ON cycles(project_id);
CREATE INDEX idx_projects_is_active ON projects(is_active);
CREATE INDEX idx_projects_project_key ON projects(project_key);
