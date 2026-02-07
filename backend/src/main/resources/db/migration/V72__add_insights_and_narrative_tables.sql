-- V72: Add insights and narrative tables for v0.5 "Insight, Not Metrics"
-- This migration adds:
-- 1. Action follow-through tracking for retrospective items
-- 2. Tag system for linking retro items and pitches
-- 3. Cycle narrative summaries (AI-generated or template-based)

-- ============================================
-- 1. Extend retro_items with follow-through tracking
-- ============================================
ALTER TABLE retro_items ADD COLUMN acted_on BOOLEAN DEFAULT FALSE;
ALTER TABLE retro_items ADD COLUMN acted_on_notes TEXT;
ALTER TABLE retro_items ADD COLUMN acted_on_at TIMESTAMP;
ALTER TABLE retro_items ADD COLUMN acted_on_by_id BIGINT;

ALTER TABLE retro_items ADD CONSTRAINT fk_retro_item_acted_on_by 
    FOREIGN KEY (acted_on_by_id) REFERENCES users(id);

-- ============================================
-- 2. Tags table for cross-entity correlation
-- ============================================
CREATE TABLE tags (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    project_id BIGINT NOT NULL,
    color VARCHAR(20) DEFAULT '#6366f1',
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id BIGINT,
    CONSTRAINT fk_tag_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_tag_created_by FOREIGN KEY (created_by_id) REFERENCES users(id),
    CONSTRAINT uk_tag_name_project UNIQUE (name, project_id)
);

CREATE INDEX idx_tag_project ON tags(project_id);
CREATE INDEX idx_tag_name ON tags(name);

-- ============================================
-- 3. Retro Item Tags join table
-- ============================================
CREATE TABLE retro_item_tags (
    retro_item_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (retro_item_id, tag_id),
    CONSTRAINT fk_retro_item_tag_retro_item FOREIGN KEY (retro_item_id) REFERENCES retro_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_retro_item_tag_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE INDEX idx_retro_item_tags_retro_item ON retro_item_tags(retro_item_id);
CREATE INDEX idx_retro_item_tags_tag ON retro_item_tags(tag_id);

-- ============================================
-- 4. Pitch Tags join table
-- ============================================
CREATE TABLE pitch_tags (
    pitch_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (pitch_id, tag_id),
    CONSTRAINT fk_pitch_tag_pitch FOREIGN KEY (pitch_id) REFERENCES pitches(id) ON DELETE CASCADE,
    CONSTRAINT fk_pitch_tag_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE INDEX idx_pitch_tags_pitch ON pitch_tags(pitch_id);
CREATE INDEX idx_pitch_tags_tag ON pitch_tags(tag_id);

-- ============================================
-- 5. Cycle Narratives table
-- ============================================
CREATE TABLE cycle_narratives (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cycle_id BIGINT NOT NULL,
    narrative_type VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    is_ai_generated BOOLEAN DEFAULT FALSE,
    ai_model VARCHAR(100),
    generation_prompt_hash VARCHAR(64),
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_by_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_narrative_cycle FOREIGN KEY (cycle_id) REFERENCES cycles(id) ON DELETE CASCADE,
    CONSTRAINT fk_narrative_generated_by FOREIGN KEY (generated_by_id) REFERENCES users(id)
);

CREATE INDEX idx_narrative_cycle ON cycle_narratives(cycle_id);
CREATE INDEX idx_narrative_type ON cycle_narratives(narrative_type);
CREATE INDEX idx_narrative_cycle_type ON cycle_narratives(cycle_id, narrative_type);

-- Enum constraint for narrative types
ALTER TABLE cycle_narratives ADD CONSTRAINT chk_narrative_type 
    CHECK (narrative_type IN ('WHAT_WE_BET', 'WHAT_SHIPPED', 'WHAT_WE_CUT', 'SURPRISES', 'FULL_SUMMARY'));

-- ============================================
-- 6. Cycle Signals cache table (optional, for expensive calculations)
-- ============================================
CREATE TABLE cycle_signal_cache (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT NOT NULL,
    signal_type VARCHAR(50) NOT NULL,
    signal_data JSONB NOT NULL,
    cycles_included TEXT, -- Comma-separated cycle IDs
    calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP,
    CONSTRAINT fk_signal_cache_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_signal_cache_project ON cycle_signal_cache(project_id);
CREATE INDEX idx_signal_cache_type ON cycle_signal_cache(signal_type);
CREATE INDEX idx_signal_cache_project_type ON cycle_signal_cache(project_id, signal_type);

-- Enum constraint for signal types
ALTER TABLE cycle_signal_cache ADD CONSTRAINT chk_signal_type 
    CHECK (signal_type IN ('APPETITE_ACCURACY', 'SHAPING_PATTERN', 'RISK_CORRELATION', 'RETRO_FOLLOW_THROUGH'));
