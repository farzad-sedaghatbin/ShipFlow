-- Pitch-to-pitch dependencies (enforced: BLOCKS/DEPENDS_ON block reordering)
CREATE TABLE IF NOT EXISTS pitch_dependencies (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_pitch_id BIGINT NOT NULL REFERENCES pitches(id),
    target_pitch_id BIGINT NOT NULL REFERENCES pitches(id),
    dependency_type VARCHAR(50) NOT NULL DEFAULT 'BLOCKS',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_pitch_dep UNIQUE (source_pitch_id, target_pitch_id)
);

CREATE INDEX IF NOT EXISTS idx_pitch_dep_source ON pitch_dependencies (source_pitch_id);
CREATE INDEX IF NOT EXISTS idx_pitch_dep_target ON pitch_dependencies (target_pitch_id);

-- Epic-to-epic dependencies (informational only)
CREATE TABLE IF NOT EXISTS epic_dependencies (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_epic_id BIGINT NOT NULL REFERENCES epics(id),
    target_epic_id BIGINT NOT NULL REFERENCES epics(id),
    dependency_type VARCHAR(50) NOT NULL DEFAULT 'BLOCKS',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_epic_dep UNIQUE (source_epic_id, target_epic_id)
);

CREATE INDEX IF NOT EXISTS idx_epic_dep_source ON epic_dependencies (source_epic_id);
CREATE INDEX IF NOT EXISTS idx_epic_dep_target ON epic_dependencies (target_epic_id);
