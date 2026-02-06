-- Add Wise Architecture experimental feature flag to organization settings
ALTER TABLE organization_settings ADD COLUMN enable_wise_architecture BOOLEAN NOT NULL DEFAULT FALSE;

-- Add comment for documentation
COMMENT ON COLUMN organization_settings.enable_wise_architecture IS 'Enable Wise Architecture experimental feature - AI-powered technical solution generator';
