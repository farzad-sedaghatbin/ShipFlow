-- Add MCP (Model Context Protocol) configuration fields to organization_settings
-- These fields allow organizations to configure GitHub and Figma access for Wise Architecture

-- GitHub MCP configuration
ALTER TABLE organization_settings ADD COLUMN IF NOT EXISTS github_access_token TEXT;
ALTER TABLE organization_settings ADD COLUMN IF NOT EXISTS default_github_owner VARCHAR(255);
ALTER TABLE organization_settings ADD COLUMN IF NOT EXISTS default_github_repo VARCHAR(255);
ALTER TABLE organization_settings ADD COLUMN IF NOT EXISTS default_github_branch VARCHAR(255) DEFAULT 'main';

-- Figma MCP configuration (figma_access_token already exists, just add default file key)
ALTER TABLE organization_settings ADD COLUMN IF NOT EXISTS default_figma_file_key VARCHAR(255);

COMMENT ON COLUMN organization_settings.github_access_token IS 'GitHub personal access token for Wise Architecture code context';
COMMENT ON COLUMN organization_settings.default_github_owner IS 'Default GitHub repository owner (organization or username)';
COMMENT ON COLUMN organization_settings.default_github_repo IS 'Default GitHub repository name';
COMMENT ON COLUMN organization_settings.default_github_branch IS 'Default GitHub branch name';
COMMENT ON COLUMN organization_settings.default_figma_file_key IS 'Default Figma file key for design context';
