CREATE TABLE IF NOT EXISTS project_snapshots (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id  BIGINT NOT NULL,
    content     TEXT   NOT NULL,
    computed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_project_snapshots_project FOREIGN KEY (project_id) REFERENCES projects(id)
);
CREATE INDEX IF NOT EXISTS idx_project_snapshots_project_id ON project_snapshots (project_id);
