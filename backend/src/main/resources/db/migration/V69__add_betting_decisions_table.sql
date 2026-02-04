-- V69: Add betting_decisions table for tracking commit/reject decisions with reasoning
-- Supports Shape Up methodology by recording WHY betting decisions were made

CREATE TABLE IF NOT EXISTS betting_decisions (
    id BIGSERIAL PRIMARY KEY,
    pitch_id BIGINT NOT NULL REFERENCES pitches(id) ON DELETE CASCADE,
    cycle_id BIGINT NOT NULL REFERENCES cycles(id) ON DELETE CASCADE,
    decision VARCHAR(50) NOT NULL,
    reason TEXT NOT NULL,
    decided_by_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    decided_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Appetite comparison fields
    requested_appetite_days INTEGER NOT NULL,
    available_capacity_days INTEGER,
    appetite_comparison_notes TEXT,
    
    -- Team and priority fields
    considered_team_id BIGINT REFERENCES teams(id) ON DELETE SET NULL,
    priority_score INTEGER,
    strategic_alignment_notes TEXT,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for common queries
CREATE INDEX idx_betting_decision_pitch ON betting_decisions(pitch_id);
CREATE INDEX idx_betting_decision_cycle ON betting_decisions(cycle_id);
CREATE INDEX idx_betting_decision_decided_at ON betting_decisions(decided_at);
CREATE INDEX idx_betting_decision_type ON betting_decisions(decision);
CREATE INDEX idx_betting_decision_decided_by ON betting_decisions(decided_by_id);
CREATE INDEX idx_betting_decision_considered_team ON betting_decisions(considered_team_id);

-- Comments for documentation
COMMENT ON TABLE betting_decisions IS 'Historical record of betting decisions for pitches - tracks commit/reject with reasoning';
COMMENT ON COLUMN betting_decisions.decision IS 'Decision type: COMMITTED, REJECTED, DEFERRED, NEEDS_SHAPING';
COMMENT ON COLUMN betting_decisions.reason IS 'Required explanation for why this decision was made';
COMMENT ON COLUMN betting_decisions.requested_appetite_days IS 'Appetite requested by the pitch in days';
COMMENT ON COLUMN betting_decisions.available_capacity_days IS 'Available team capacity in days when decision was made';
COMMENT ON COLUMN betting_decisions.appetite_comparison_notes IS 'Notes about how appetite compares to capacity';
COMMENT ON COLUMN betting_decisions.priority_score IS 'Relative priority score for comparing multiple pitches';
COMMENT ON COLUMN betting_decisions.strategic_alignment_notes IS 'How this pitch aligns with strategic goals';
