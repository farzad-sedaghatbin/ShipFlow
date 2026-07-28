-- WebAuthn ephemeral challenge storage — S59, v1.11.0 "Mobile PWA" (biometric/passkey auth)
-- Mirrors password_reset_tokens (V2__add_password_reset.sql): a short-lived,
-- DB-backed, single-use token bridging the two round-trips of a WebAuthn
-- ceremony (options -> browser -> verify). No Redis/new ephemeral store
-- introduced — reuses the existing pattern in this codebase.
--
-- user_id is nullable: during the username-first login flow, if the supplied
-- username does not match any user we still issue a generically-shaped
-- challenge (to avoid username enumeration) with no associated user_id;
-- username_hint records the username the flow was started with either way.
--
-- challenge is VARCHAR(255) UNIQUE rather than TEXT, matching
-- password_reset_tokens.token's existing VARCHAR(255) UNIQUE convention.
CREATE TABLE webauthn_challenges (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    challenge VARCHAR(255) NOT NULL,
    user_id BIGINT,
    username_hint VARCHAR(255),
    purpose VARCHAR(20) NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_webauthn_challenges_users FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_webauthn_challenges_challenge UNIQUE (challenge)
);

CREATE INDEX IF NOT EXISTS idx_webauthn_challenges_user_id ON webauthn_challenges(user_id);
