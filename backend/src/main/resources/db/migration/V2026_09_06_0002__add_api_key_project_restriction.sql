-- Adds an optional project restriction to API keys, so a key can be scoped to a single
-- project instead of implicitly granting org-wide access to every project/pitch/task/bug/
-- release/cycle/epic reachable through /api/v1/public/** and the data export/import endpoints.
--
-- NULL = unrestricted (the existing behavior for every key created before this migration,
-- and the default for any new key that doesn't opt in). This is purely additive/non-breaking.
ALTER TABLE api_keys ADD COLUMN restricted_to_project_id BIGINT;
ALTER TABLE api_keys ADD CONSTRAINT fk_api_keys_project FOREIGN KEY (restricted_to_project_id) REFERENCES projects(id);
