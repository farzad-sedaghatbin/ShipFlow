-- ShipFlow is moving to an open-core model: the public build is the Community
-- Edition, and a signed licence file unlocks commercial features and lifts
-- Community limits (see `license` package / LICENSING.md-style docs in the
-- 30-licensing.md help guide).
--
-- This table holds exactly one row: the currently-uploaded licence file's raw
-- text (JSON: {"payload": {...}, "signature": "<base64 Ed25519 signature>"}).
-- Uploading a new licence via POST /api/license replaces the single row in
-- place rather than appending history — LicenseService always reads the most
-- recent row. No row here falls back to the file at `app.license.file`
-- (default ./config/shipflow.license); an uploaded row always wins over that
-- file. Deliberately NOT Envers-audited (@Audited), matching the sibling
-- single-row config tables `organization_settings` and `storage_config`.
CREATE TABLE IF NOT EXISTS license_file (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    content     TEXT NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    uploaded_by VARCHAR(255)
);
