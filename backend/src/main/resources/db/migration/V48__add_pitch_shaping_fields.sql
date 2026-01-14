-- Add Shape Up methodology fields to pitches table
-- These fields capture the full pitch narrative as described in Shape Up:
-- - Problem Statement: The problem being solved
-- - Solution: The hatched solution (fat-marker sketches)
-- - Rabbit Holes: Areas to avoid, edge cases that should be out of scope
-- - Risks: Known risks and unknowns
-- - No-Gos: Things explicitly out of scope
-- - Wireframe Links: Links to wireframes, prototypes, or visual references

ALTER TABLE pitches ADD COLUMN IF NOT EXISTS problem_statement TEXT;
ALTER TABLE pitches ADD COLUMN IF NOT EXISTS solution TEXT;
ALTER TABLE pitches ADD COLUMN IF NOT EXISTS rabbit_holes TEXT;
ALTER TABLE pitches ADD COLUMN IF NOT EXISTS risks TEXT;
ALTER TABLE pitches ADD COLUMN IF NOT EXISTS no_gos TEXT;
ALTER TABLE pitches ADD COLUMN IF NOT EXISTS wireframe_links TEXT;

-- Add index for searching shaped pitches with full content
CREATE INDEX IF NOT EXISTS idx_pitches_shaped_content ON pitches(status) WHERE status = 'SHAPED';
