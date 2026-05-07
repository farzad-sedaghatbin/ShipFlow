-- V73: Add Figma access token to organization settings for Wise Architecture
-- This allows organizations to provide their own Figma token for design context analysis

ALTER TABLE organization_settings
ADD COLUMN IF NOT EXISTS figma_access_token TEXT;

-- Add comment explaining the field
COMMENT ON COLUMN organization_settings.figma_access_token IS 'Figma personal access token for reading design files via MCP (encrypted at rest recommended)';
