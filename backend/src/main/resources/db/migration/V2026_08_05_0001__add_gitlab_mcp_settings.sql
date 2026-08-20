-- S61: GitLab MCP client provider settings
-- Adds a Personal Access Token and default project columns for the GitLab MCP integration,
-- mirroring the GitHub MCP settings (github_access_token / default_github_owner / ...).
ALTER TABLE organization_settings
    ADD COLUMN IF NOT EXISTS gitlab_access_token TEXT,
    ADD COLUMN IF NOT EXISTS default_gitlab_project_id VARCHAR(255);
