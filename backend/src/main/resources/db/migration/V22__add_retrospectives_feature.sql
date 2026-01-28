-- Add enable_retrospectives setting to projects (default ON)
ALTER TABLE projects ADD COLUMN enable_retrospectives BOOLEAN NOT NULL DEFAULT TRUE;

-- Retrospectives table
CREATE TABLE retrospectives (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    notes TEXT,
    status VARCHAR(50) NOT NULL,
    cycle_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    created_by_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    closed_at TIMESTAMP,
    CONSTRAINT fk_retrospectives_cycle FOREIGN KEY (cycle_id) REFERENCES cycles(id) ON DELETE CASCADE,
    CONSTRAINT fk_retrospectives_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_retrospectives_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Retro items table
CREATE TABLE retro_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    content TEXT NOT NULL,
    column_type VARCHAR(50) NOT NULL,
    retrospective_id BIGINT NOT NULL,
    author_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_retro_items_retrospective FOREIGN KEY (retrospective_id) REFERENCES retrospectives(id) ON DELETE CASCADE,
    CONSTRAINT fk_retro_items_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for better query performance
CREATE INDEX idx_retrospectives_cycle_id ON retrospectives(cycle_id);
CREATE INDEX idx_retrospectives_project_id ON retrospectives(project_id);
CREATE INDEX idx_retrospectives_status ON retrospectives(status);
CREATE INDEX idx_retro_items_retrospective_id ON retro_items(retrospective_id);
CREATE INDEX idx_retro_items_column_type ON retro_items(column_type);
