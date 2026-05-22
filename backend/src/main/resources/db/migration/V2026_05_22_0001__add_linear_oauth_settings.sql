-- V2026_05_22_0001__add_linear_oauth_settings.sql
-- Linear API OAuth storage for competitor migration tooling (v1.2.0 S29)

ALTER TABLE organization_settings
  ADD COLUMN IF NOT EXISTS linear_access_token TEXT,
  ADD COLUMN IF NOT EXISTS linear_team_id VARCHAR(64),
  ADD COLUMN IF NOT EXISTS linear_team_name VARCHAR(255);
