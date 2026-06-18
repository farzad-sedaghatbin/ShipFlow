-- Object-storage support: StorageConfig singleton table + new columns on task_attachments.
-- H2-safe: no jsonb/uuid/serial; separate ALTER statements per column for H2 compatibility.

CREATE TABLE storage_config (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    active_provider VARCHAR(16)              NOT NULL,
    config          TEXT                     NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

ALTER TABLE task_attachments ADD COLUMN storage_provider VARCHAR(16);
ALTER TABLE task_attachments ADD COLUMN storage_key TEXT;

-- Backfill: existing LOCAL_FS rows get storage_provider='LOCAL_FS' and storage_key copied from file_path.
UPDATE task_attachments SET storage_provider = 'LOCAL_FS', storage_key = file_path WHERE storage_key IS NULL;

-- Seed the default active configuration row.
INSERT INTO storage_config (active_provider, config, created_at, updated_at)
VALUES ('LOCAL_FS', '{}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
