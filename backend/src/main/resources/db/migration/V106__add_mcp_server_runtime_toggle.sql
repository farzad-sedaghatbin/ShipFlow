-- Runtime toggle for the ShipFlow MCP server.
--
-- Previously the MCP server could only be enabled via the MCP_SERVER_ENABLED /
-- MCP_SERVER_WRITE_ENABLED environment variables, which required a restart and was
-- invisible to admins in the UI. These nullable columns let an ADMIN flip the server
-- on/off (and toggle write tools) at runtime from Integrations -> MCP.
--
-- NULL means "no override" -> fall back to the environment-variable default
-- (mcp.server.enabled / mcp.server.write-enabled). This keeps existing self-hosted
-- deployments behaving exactly as before until an admin explicitly sets a value.

ALTER TABLE organization_settings
    ADD COLUMN IF NOT EXISTS mcp_server_enabled BOOLEAN;

ALTER TABLE organization_settings
    ADD COLUMN IF NOT EXISTS mcp_server_write_enabled BOOLEAN;
