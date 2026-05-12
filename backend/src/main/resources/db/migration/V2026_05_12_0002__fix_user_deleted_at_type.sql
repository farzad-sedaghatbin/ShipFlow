-- Fix: drop and re-add deleted_at using plain TIMESTAMP to match existing
-- column conventions (created_at, updated_at) and correctly map to LocalDateTime.
DROP INDEX IF EXISTS idx_users_deleted_at;

ALTER TABLE users DROP COLUMN IF EXISTS deleted_at;
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at);
