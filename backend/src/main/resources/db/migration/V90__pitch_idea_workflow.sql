-- V90: Enable Idea-to-Pitch workflow with Shape Up lifecycle
-- Allows pitches to exist without a cycle (IDEA, DRAFT, SHAPED statuses)
-- Cycle assignment happens at betting table when SHAPED -> PENDING

-- Make cycle_id nullable for pre-cycle pitches (IDEA, DRAFT, SHAPED)
ALTER TABLE pitches ALTER COLUMN cycle_id DROP NOT NULL;

-- Make appetite_days nullable for ideas (not yet estimated)
ALTER TABLE pitches ALTER COLUMN appetite_days DROP NOT NULL;

-- Add index for querying unassigned pitches efficiently
CREATE INDEX IF NOT EXISTS idx_pitches_unassigned ON pitches(status) WHERE cycle_id IS NULL AND deleted_at IS NULL;

-- Add index for querying by status (for Ideas Pool, Shaping Pool, Betting Table)
CREATE INDEX IF NOT EXISTS idx_pitches_status ON pitches(status) WHERE deleted_at IS NULL;

-- Add index for querying shaped pitches ready for betting
CREATE INDEX IF NOT EXISTS idx_pitches_betting_candidates ON pitches(status, epic_id) 
WHERE status = 'SHAPED' AND cycle_id IS NULL AND deleted_at IS NULL;

-- Update any existing PENDING pitches without cycles to IDEA (data cleanup)
-- This shouldn't happen in practice, but ensures data consistency
UPDATE pitches SET status = 'IDEA' WHERE cycle_id IS NULL AND status = 'PENDING';

-- Add comment explaining the status lifecycle
COMMENT ON COLUMN pitches.status IS 'Shape Up lifecycle: IDEA (raw concept) -> DRAFT (being shaped) -> SHAPED (ready for betting) -> PENDING (assigned to cycle) -> STARTED -> IN_PROGRESS -> DONE';
COMMENT ON COLUMN pitches.cycle_id IS 'Required for statuses PENDING and beyond. NULL for IDEA, DRAFT, SHAPED (pre-betting).';
COMMENT ON COLUMN pitches.appetite_days IS 'Required for SHAPED status and beyond. NULL for IDEA status.';
