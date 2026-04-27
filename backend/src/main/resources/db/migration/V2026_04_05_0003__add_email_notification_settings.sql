-- S17: Add email notification settings to organization_settings table
ALTER TABLE organization_settings
    ADD COLUMN IF NOT EXISTS email_notifications_enabled BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS smtp_host VARCHAR(255),
    ADD COLUMN IF NOT EXISTS smtp_port INTEGER DEFAULT 587,
    ADD COLUMN IF NOT EXISTS smtp_username VARCHAR(255),
    ADD COLUMN IF NOT EXISTS smtp_from VARCHAR(255) DEFAULT 'noreply@shipflow.dev',
    ADD COLUMN IF NOT EXISTS smtp_tls_enabled BOOLEAN NOT NULL DEFAULT true;
