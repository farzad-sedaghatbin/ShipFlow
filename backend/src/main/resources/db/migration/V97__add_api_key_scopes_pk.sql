-- Add composite primary key to api_key_scopes to prevent duplicate scope entries.
-- V96 created the table without a PK; this migration adds it safely.

-- Remove any existing duplicates before adding the constraint
DELETE FROM api_key_scopes a
    USING api_key_scopes b
WHERE a.ctid < b.ctid
  AND a.api_key_id = b.api_key_id
  AND a.scope = b.scope;

ALTER TABLE api_key_scopes
    ADD PRIMARY KEY (api_key_id, scope);
