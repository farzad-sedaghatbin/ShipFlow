-- V13: Add tables for Q&A feature

-- Knowledge items table for storing embedded knowledge chunks
CREATE TABLE knowledge_items (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    title VARCHAR(500),
    cycle_id BIGINT,
    team_id BIGINT,
    pitch_id BIGINT,
    author_id BIGINT,
    embedding_id VARCHAR(100),
    is_embedded BOOLEAN NOT NULL DEFAULT FALSE,
    chunk_index INTEGER NOT NULL DEFAULT 0,
    total_chunks INTEGER NOT NULL DEFAULT 1,
    content_hash VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for knowledge items
CREATE INDEX idx_knowledge_entity ON knowledge_items(entity_type, entity_id);
CREATE INDEX idx_knowledge_cycle ON knowledge_items(cycle_id);
CREATE INDEX idx_knowledge_team ON knowledge_items(team_id);
CREATE INDEX idx_knowledge_embedded ON knowledge_items(is_embedded);

-- Q&A interactions table for storing questions and answers
CREATE TABLE qa_interactions (
    id BIGSERIAL PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT,
    context_type VARCHAR(50),
    context_id BIGINT,
    cycle_id BIGINT,
    team_id BIGINT,
    user_id BIGINT NOT NULL,
    source_knowledge_ids VARCHAR(500),
    confidence_score INTEGER,
    feedback_type VARCHAR(20),
    feedback_correction TEXT,
    is_embedded BOOLEAN NOT NULL DEFAULT FALSE,
    processing_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    feedback_at TIMESTAMP
);

-- Indexes for Q&A interactions
CREATE INDEX idx_qa_user ON qa_interactions(user_id);
CREATE INDEX idx_qa_context ON qa_interactions(context_type, context_id);
CREATE INDEX idx_qa_created ON qa_interactions(created_at);

-- Manual notes table for user-created notes
CREATE TABLE manual_notes (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    context_type VARCHAR(50) NOT NULL,
    context_id BIGINT NOT NULL,
    cycle_id BIGINT,
    team_id BIGINT,
    pitch_id BIGINT,
    author_id BIGINT NOT NULL,
    include_in_knowledge BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for manual notes
CREATE INDEX idx_note_context ON manual_notes(context_type, context_id);
CREATE INDEX idx_note_author ON manual_notes(author_id);

-- Foreign key constraints (optional - depends on your referential integrity needs)
-- ALTER TABLE knowledge_items ADD CONSTRAINT fk_knowledge_cycle FOREIGN KEY (cycle_id) REFERENCES cycles(id);
-- ALTER TABLE knowledge_items ADD CONSTRAINT fk_knowledge_team FOREIGN KEY (team_id) REFERENCES teams(id);
-- ALTER TABLE knowledge_items ADD CONSTRAINT fk_knowledge_pitch FOREIGN KEY (pitch_id) REFERENCES pitches(id);
-- ALTER TABLE qa_interactions ADD CONSTRAINT fk_qa_user FOREIGN KEY (user_id) REFERENCES users(id);
-- ALTER TABLE manual_notes ADD CONSTRAINT fk_note_author FOREIGN KEY (author_id) REFERENCES users(id);
