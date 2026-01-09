-- V19: Add betting_slots table for Shape Up Betting Table feature
-- Supports assigning shaped pitches to team slots within cycles

CREATE TABLE IF NOT EXISTS betting_slots (
    id BIGSERIAL PRIMARY KEY,
    cycle_id BIGINT NOT NULL REFERENCES cycles(id) ON DELETE CASCADE,
    team_id BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    pitch_id BIGINT REFERENCES pitches(id) ON DELETE SET NULL,
    position INTEGER NOT NULL DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_betting_slot_position UNIQUE (cycle_id, team_id, position)
);

-- Add indexes for common queries
CREATE INDEX idx_betting_slots_cycle_id ON betting_slots(cycle_id);
CREATE INDEX idx_betting_slots_team_id ON betting_slots(team_id);
CREATE INDEX idx_betting_slots_pitch_id ON betting_slots(pitch_id);

-- Comment on the table
COMMENT ON TABLE betting_slots IS 'Betting Table slots for assigning shaped pitches to team tracks within cycles';
COMMENT ON COLUMN betting_slots.position IS 'Position of the slot within the team track (0, 1, 2, etc.)';
COMMENT ON COLUMN betting_slots.start_date IS 'Start date of the slot window';
COMMENT ON COLUMN betting_slots.end_date IS 'End date of the slot window';
