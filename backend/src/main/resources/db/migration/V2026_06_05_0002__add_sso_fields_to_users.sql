-- SSO: add SSO-related columns to users table.
-- identity_provider_id: which IdP provisioned this user (nullable for local users).
-- external_user_id: the subject/sub claim from the IdP token.
-- provisioned_via: how the user account was created (LOCAL, SAML2, OIDC, SCIM).
ALTER TABLE users ADD COLUMN IF NOT EXISTS identity_provider_id BIGINT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS external_user_id    VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS provisioned_via      VARCHAR(50) DEFAULT 'LOCAL';

CREATE INDEX IF NOT EXISTS idx_users_identity_provider_id
    ON users (identity_provider_id);

CREATE INDEX IF NOT EXISTS idx_users_external_user_id
    ON users (external_user_id);
