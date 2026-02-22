-- =====================================================================
-- V96: Add API Keys and Webhook Subscriptions for Public API
-- =====================================================================

-- API Keys table: stores hashed keys for external integrations
CREATE TABLE IF NOT EXISTS api_keys (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    key_prefix      VARCHAR(8)   NOT NULL,
    key_hash        VARCHAR(64)  NOT NULL UNIQUE,
    user_id         BIGINT       NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at      TIMESTAMP    NULL,
    last_used_at    TIMESTAMP    NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at      TIMESTAMP    NULL,
    CONSTRAINT fk_api_keys_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_api_keys_key_hash ON api_keys(key_hash);
CREATE INDEX IF NOT EXISTS idx_api_keys_user_id  ON api_keys(user_id);

-- API Key Scopes join table
CREATE TABLE IF NOT EXISTS api_key_scopes (
    api_key_id BIGINT       NOT NULL,
    scope      VARCHAR(50)  NOT NULL,
    CONSTRAINT fk_key_scopes_api_key FOREIGN KEY (api_key_id) REFERENCES api_keys(id) ON DELETE CASCADE
);

-- Webhook Subscriptions table: stores outgoing webhook endpoints
CREATE TABLE IF NOT EXISTS webhook_subscriptions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    target_url    VARCHAR(2048) NOT NULL,
    secret        VARCHAR(255)  NOT NULL,
    event_types   TEXT          NOT NULL,
    user_id       BIGINT        NOT NULL,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    failure_count INT           NOT NULL DEFAULT 0,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NULL,
    CONSTRAINT fk_webhook_subs_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_webhook_user_id ON webhook_subscriptions(user_id);
