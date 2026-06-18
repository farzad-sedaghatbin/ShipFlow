-- SSO: identity_providers table
-- Stores SAML2 and OIDC identity provider configuration per organization.
CREATE TABLE IF NOT EXISTS identity_providers (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    organization_id   BIGINT,
    name              VARCHAR(255) NOT NULL,
    provider_type     VARCHAR(50)  NOT NULL,
    client_id         VARCHAR(500),
    client_secret     TEXT,
    metadata_url      TEXT,
    entity_id         TEXT,
    sso_url           TEXT,
    certificate       TEXT,
    is_enabled        BOOLEAN NOT NULL DEFAULT FALSE,
    enforce_sso       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_identity_providers_organization_id
    ON identity_providers (organization_id);

CREATE INDEX IF NOT EXISTS idx_identity_providers_is_enabled
    ON identity_providers (is_enabled);
