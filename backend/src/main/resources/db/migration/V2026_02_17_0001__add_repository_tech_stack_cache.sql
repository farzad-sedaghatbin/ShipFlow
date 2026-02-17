-- Add table to cache detected tech stacks for repositories
-- Tech stacks are typically stable, so we cache them to avoid repeated detection

CREATE TABLE repository_tech_stacks (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    stack_type VARCHAR(50) NOT NULL,
    confidence_score INTEGER,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detected_by_files TEXT,
    
    CONSTRAINT fk_repo_tech_stack_repo 
        FOREIGN KEY (repository_id) 
        REFERENCES github_repositories(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT uk_repo_stack 
        UNIQUE (repository_id, stack_type)
);

CREATE INDEX idx_repo_tech_stacks_repository 
    ON repository_tech_stacks(repository_id);

CREATE INDEX idx_repo_tech_stacks_stack_type 
    ON repository_tech_stacks(stack_type);

COMMENT ON TABLE repository_tech_stacks IS 'Cached tech stack detections for repositories';
COMMENT ON COLUMN repository_tech_stacks.stack_type IS 'Type of technology stack detected (e.g., MOBILE_KOTLIN, WEB_REACT)';
COMMENT ON COLUMN repository_tech_stacks.confidence_score IS 'Detection confidence percentage (0-100)';
COMMENT ON COLUMN repository_tech_stacks.detected_by_files IS 'Comma-separated list of key files that identified this stack';
