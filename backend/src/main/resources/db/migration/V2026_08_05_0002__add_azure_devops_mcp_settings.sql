-- S62: Azure DevOps MCP client settings
-- Adds a Personal Access Token (PAT) column for the Azure DevOps MCP integration,
-- mirroring the github_access_token / notion_access_token / confluence_access_token columns.
ALTER TABLE organization_settings
    ADD COLUMN IF NOT EXISTS azure_devops_access_token TEXT;
