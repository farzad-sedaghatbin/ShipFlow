-- S43: Notion and Confluence MCP client settings
-- Adds token and configuration columns for Notion and Confluence MCP integrations.
ALTER TABLE organization_settings
    ADD COLUMN IF NOT EXISTS notion_access_token TEXT,
    ADD COLUMN IF NOT EXISTS confluence_access_token TEXT,
    ADD COLUMN IF NOT EXISTS default_confluence_space_key VARCHAR(100),
    ADD COLUMN IF NOT EXISTS default_confluence_domain VARCHAR(255);
