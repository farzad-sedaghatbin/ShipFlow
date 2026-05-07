-- V62: Add GitHub App Installation table for organization-wide OAuth consent
-- This enables bulk repository access through a single authorization instead of adding repositories one by one

CREATE TABLE github_app_installations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    installation_id BIGINT NOT NULL UNIQUE,
    account_type VARCHAR(20) NOT NULL,
    account_login VARCHAR(255) NOT NULL,
    account_id BIGINT NOT NULL,
    repository_selection VARCHAR(20) NOT NULL,
    access_token VARCHAR(500),
    token_expires_at TIMESTAMP,
    permissions TEXT,
    subscribed_events VARCHAR(1000),
    target_id BIGINT,
    target_type VARCHAR(20),
    installed_by_login VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    webhooks_configured BOOLEAN NOT NULL DEFAULT FALSE,
    last_sync_at TIMESTAMP,
    repository_count INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_github_app_installation_account ON github_app_installations (account_login);
CREATE INDEX idx_github_app_installation_active ON github_app_installations (is_active);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_github_app_installations_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_github_app_installations_updated_at
BEFORE UPDATE ON github_app_installations
FOR EACH ROW
EXECUTE FUNCTION update_github_app_installations_updated_at();
