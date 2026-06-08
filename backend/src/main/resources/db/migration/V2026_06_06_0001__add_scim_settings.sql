-- Add provisioning fields to users table (needed for SCIM 2.0 user provisioning)
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS external_user_id VARCHAR(500),
  ADD COLUMN IF NOT EXISTS provisioned_via VARCHAR(50) NOT NULL DEFAULT 'LOCAL';

-- SCIM 2.0 provisioning settings on the organisation settings table
ALTER TABLE organization_settings
  ADD COLUMN IF NOT EXISTS scim_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS scim_bearer_token VARCHAR(500),
  ADD COLUMN IF NOT EXISTS scim_token_hash VARCHAR(500);

-- Audit log for SCIM provisioning events
CREATE TABLE IF NOT EXISTS scim_audit_log (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  event_type VARCHAR(50) NOT NULL,
  external_id VARCHAR(500),
  username VARCHAR(255),
  details TEXT,
  occurred_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scim_audit_log_occurred_at ON scim_audit_log (occurred_at DESC);
