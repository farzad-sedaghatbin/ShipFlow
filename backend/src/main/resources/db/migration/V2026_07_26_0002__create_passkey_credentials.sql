-- Passkey (WebAuthn) credentials — S59, v1.11.0 "Mobile PWA" (biometric/passkey auth)
-- One row per registered authenticator (e.g. a device's Touch ID / Face ID / security key).
-- A user may register multiple passkeys (multiple devices).
--
-- NOTE: credential_id is VARCHAR(500) rather than TEXT so it can carry a UNIQUE
-- constraint without relying on CLOB-unique-index support, consistent with the
-- existing password_reset_tokens.token (VARCHAR(255) UNIQUE) and
-- api_keys.key_hash (VARCHAR UNIQUE) columns in this codebase. WebAuthn
-- credential IDs are typically well under 200 base64url characters, so 500
-- leaves comfortable headroom.
CREATE TABLE passkey_credentials (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    credential_id VARCHAR(500) NOT NULL,
    public_key_cose TEXT NOT NULL,
    sign_count BIGINT NOT NULL DEFAULT 0,
    attestation_type VARCHAR(50),
    transports VARCHAR(255),
    device_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_passkey_credentials_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_passkey_credentials_credential_id UNIQUE (credential_id)
);

CREATE INDEX IF NOT EXISTS idx_passkey_credentials_user_id ON passkey_credentials(user_id);
