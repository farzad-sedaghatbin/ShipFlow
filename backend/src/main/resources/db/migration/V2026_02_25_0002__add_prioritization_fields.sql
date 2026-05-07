-- Add priority and sort_order fields for pitch prioritization within epics
-- and priority labels for initiatives, epics, and pitches

-- Pitches: add sort_order and priority
ALTER TABLE pitches ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
ALTER TABLE pitches ADD COLUMN IF NOT EXISTS priority VARCHAR(10);

-- Initiatives: add priority (sort_order already exists from V82)
ALTER TABLE initiatives ADD COLUMN IF NOT EXISTS priority VARCHAR(10);

-- Epics: add priority (sort_order already exists from V82)
ALTER TABLE epics ADD COLUMN IF NOT EXISTS priority VARCHAR(10);

-- Index for efficient ordering of pitches within an epic
CREATE INDEX IF NOT EXISTS idx_pitches_epic_sort ON pitches(epic_id, sort_order) WHERE deleted_at IS NULL;

-- Index for filtering by priority
CREATE INDEX IF NOT EXISTS idx_pitches_priority ON pitches(priority) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_initiatives_priority ON initiatives(priority) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_epics_priority ON epics(priority) WHERE deleted_at IS NULL;
