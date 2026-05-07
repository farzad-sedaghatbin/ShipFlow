-- V71: Make betting_decisions.reason column nullable
-- The UI treats reason as optional (users can commit/reject without explanation)

ALTER TABLE betting_decisions ALTER COLUMN reason DROP NOT NULL;

COMMENT ON COLUMN betting_decisions.reason IS 'Optional explanation for why this decision was made';
