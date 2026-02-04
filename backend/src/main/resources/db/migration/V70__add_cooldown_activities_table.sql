-- V70: Add cooldown_activities table for tracking Shape Up cooldown phase work
-- Cooldown is the 2-week period between cycles for cleanup, bugs, tech debt, and preparation

CREATE TABLE cooldown_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cycle_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    assignee_id BIGINT,
    created_by_id BIGINT NOT NULL,
    estimated_hours INT,
    actual_hours INT,
    priority INT DEFAULT 3,
    related_pitch_id BIGINT,
    notes TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_cooldown_cycle FOREIGN KEY (cycle_id) REFERENCES cycles(id),
    CONSTRAINT fk_cooldown_assignee FOREIGN KEY (assignee_id) REFERENCES users(id),
    CONSTRAINT fk_cooldown_created_by FOREIGN KEY (created_by_id) REFERENCES users(id),
    CONSTRAINT fk_cooldown_pitch FOREIGN KEY (related_pitch_id) REFERENCES pitches(id)
);

-- Indexes for efficient querying
CREATE INDEX idx_cooldown_activity_cycle ON cooldown_activities(cycle_id);
CREATE INDEX idx_cooldown_activity_type ON cooldown_activities(activity_type);
CREATE INDEX idx_cooldown_activity_status ON cooldown_activities(status);
CREATE INDEX idx_cooldown_activity_assignee ON cooldown_activities(assignee_id);

-- Check constraints for valid enums
ALTER TABLE cooldown_activities ADD CONSTRAINT chk_activity_type 
    CHECK (activity_type IN ('TECH_DEBT', 'BUG_FIX', 'REFACTORING', 'KICKOFF_PREP', 
                              'LEARNING', 'QA', 'DOCUMENTATION', 'INFRASTRUCTURE', 
                              'RESEARCH', 'PLANNING', 'OTHER'));

ALTER TABLE cooldown_activities ADD CONSTRAINT chk_activity_status 
    CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED', 'BLOCKED'));
