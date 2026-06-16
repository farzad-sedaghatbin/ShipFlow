-- SharePoint Graph API credentials for direct Microsoft Graph integration.
-- Tenant ID + client credentials are used for Azure AD client_credentials OAuth2 flow.
-- The client secret is treated like an access token — stored plaintext, never returned via REST.
ALTER TABLE organization_settings
    ADD COLUMN IF NOT EXISTS sharepoint_tenant_id    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS sharepoint_client_id    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS sharepoint_client_secret TEXT,
    ADD COLUMN IF NOT EXISTS sharepoint_site_url     VARCHAR(500);
