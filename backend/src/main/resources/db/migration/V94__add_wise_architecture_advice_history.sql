-- Migration to add WISE Architecture advice history table
-- This table stores AI-generated solutions for later review and follow-up conversations

CREATE TABLE wise_architecture_advice (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(100) NOT NULL,
    pitch_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    user_message TEXT,
    ai_response TEXT,
    tech_stacks VARCHAR(500),
    repository_ids VARCHAR(500),
    has_figma_context BOOLEAN DEFAULT FALSE,
    has_github_context BOOLEAN DEFAULT FALSE,
    has_roadmap_context BOOLEAN DEFAULT FALSE,
    processing_time_ms BIGINT,
    feedback_helpful BOOLEAN,
    feedback_text TEXT,
    feedback_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_advice_pitch FOREIGN KEY (pitch_id) REFERENCES pitches(id) ON DELETE CASCADE,
    CONSTRAINT fk_advice_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index for fast lookups by user
CREATE INDEX idx_advice_user_id ON wise_architecture_advice(user_id);

-- Index for fast lookups by conversation
CREATE INDEX idx_advice_conversation_id ON wise_architecture_advice(conversation_id);

-- Index for fast lookups by pitch
CREATE INDEX idx_advice_pitch_id ON wise_architecture_advice(pitch_id);

-- Index for chronological ordering
CREATE INDEX idx_advice_created_at ON wise_architecture_advice(created_at DESC);

-- Composite index for user conversations query
CREATE INDEX idx_advice_user_type_created ON wise_architecture_advice(user_id, message_type, created_at DESC);

COMMENT ON TABLE wise_architecture_advice IS 'Stores WISE Architecture AI-generated solutions and conversation history';
COMMENT ON COLUMN wise_architecture_advice.conversation_id IS 'UUID grouping messages in the same conversation thread';
COMMENT ON COLUMN wise_architecture_advice.message_type IS 'Type of message: INITIAL_SOLUTION or FOLLOW_UP';
COMMENT ON COLUMN wise_architecture_advice.tech_stacks IS 'Comma-separated list of tech stack types analyzed';
COMMENT ON COLUMN wise_architecture_advice.repository_ids IS 'Comma-separated list of repository IDs analyzed';
