-- Add task dependencies table for lightweight dependency tracking
-- Supports "blocks" relationships to help teams identify blockers

CREATE TABLE task_dependencies (
    id BIGSERIAL PRIMARY KEY,
    source_task_id BIGINT NOT NULL,
    target_task_id BIGINT NOT NULL,
    dependency_type VARCHAR(50) NOT NULL DEFAULT 'BLOCKS',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_dep_source FOREIGN KEY (source_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_dep_target FOREIGN KEY (target_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT chk_task_dep_no_self_reference CHECK (source_task_id != target_task_id),
    CONSTRAINT uk_task_dependency UNIQUE (source_task_id, target_task_id)
);

-- Index for efficient lookups
CREATE INDEX idx_task_dep_source ON task_dependencies(source_task_id);
CREATE INDEX idx_task_dep_target ON task_dependencies(target_task_id);
CREATE INDEX idx_task_dep_type ON task_dependencies(dependency_type);

-- Add comments for clarity
COMMENT ON TABLE task_dependencies IS 'Tracks dependencies between tasks to identify blockers';
COMMENT ON COLUMN task_dependencies.source_task_id IS 'The task that blocks/depends on the target';
COMMENT ON COLUMN task_dependencies.target_task_id IS 'The task being blocked/depended upon';
COMMENT ON COLUMN task_dependencies.dependency_type IS 'Type of dependency: BLOCKS, DEPENDS_ON, RELATED_TO';
