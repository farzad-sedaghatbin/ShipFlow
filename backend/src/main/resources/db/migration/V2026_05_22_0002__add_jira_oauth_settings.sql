-- V2026_05_22_0002__add_jira_oauth_settings.sql
-- Jira API OAuth storage for competitor migration tooling (v1.2.0 S30)

ALTER TABLE organization_settings
  ADD COLUMN IF NOT EXISTS jira_access_token TEXT,
  ADD COLUMN IF NOT EXISTS jira_refresh_token TEXT,
  ADD COLUMN IF NOT EXISTS jira_cloud_id VARCHAR(255),
  ADD COLUMN IF NOT EXISTS jira_cloud_name VARCHAR(255);
