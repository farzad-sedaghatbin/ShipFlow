-- Task management for independent work during cycles
-- V15: Add tasks table for Shape Up cooldown and independent work

CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'BACKLOG',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    estimate_hours DECIMAL(5,2),
    actual_hours DECIMAL(5,2),
    cycle_id BIGINT NOT NULL,
    assignee_id BIGINT,
    pair_assignee_id BIGINT,
    created_by_id BIGINT,
    due_date DATE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tags TEXT,
    
    CONSTRAINT fk_task_cycle FOREIGN KEY (cycle_id) REFERENCES cycles(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES persons(id) ON DELETE SET NULL,
    CONSTRAINT fk_task_pair_assignee FOREIGN KEY (pair_assignee_id) REFERENCES persons(id) ON DELETE SET NULL,
    CONSTRAINT fk_task_created_by FOREIGN KEY (created_by_id) REFERENCES persons(id) ON DELETE SET NULL
);

-- Indexes for efficient queries
CREATE INDEX idx_task_cycle ON tasks(cycle_id);
CREATE INDEX idx_task_assignee ON tasks(assignee_id);
CREATE INDEX idx_task_pair_assignee ON tasks(pair_assignee_id);
CREATE INDEX idx_task_status ON tasks(status);
CREATE INDEX idx_task_priority ON tasks(priority);
CREATE INDEX idx_task_due_date ON tasks(due_date);
