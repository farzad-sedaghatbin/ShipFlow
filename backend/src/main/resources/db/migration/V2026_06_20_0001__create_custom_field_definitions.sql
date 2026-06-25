-- Custom Fields v1.8.0: field definition table
-- Org-wide definitions have project_id = NULL (ADMIN only).
-- Project-scoped definitions have project_id set (MANAGER or ADMIN).

CREATE TABLE custom_field_definitions (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    field_type    VARCHAR(50)  NOT NULL,
    entity_type   VARCHAR(50)  NOT NULL,
    project_id    BIGINT REFERENCES projects(id),
    required      BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order    INT     NOT NULL DEFAULT 0,
    options       TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMP WITH TIME ZONE,
    deleted_by_id BIGINT REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_cfd_entity_type ON custom_field_definitions(entity_type);
CREATE INDEX IF NOT EXISTS idx_cfd_project_id  ON custom_field_definitions(project_id);
CREATE INDEX IF NOT EXISTS idx_cfd_deleted     ON custom_field_definitions(deleted_at);
