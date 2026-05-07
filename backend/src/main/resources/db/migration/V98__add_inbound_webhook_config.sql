-- ============================================================================
-- V98: Add inbound_webhook_config table for admin-managed inbound providers
-- ============================================================================

CREATE TABLE inbound_webhook_config (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider_name   VARCHAR(100) NOT NULL,
    display_name    VARCHAR(255),
    webhook_secret  VARCHAR(1000),
    signature_header VARCHAR(255),
    hmac_algorithm  VARCHAR(50) DEFAULT 'HmacSHA256',
    description     VARCHAR(2000),
    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    CONSTRAINT uk_inbound_provider_name UNIQUE (provider_name)
);

CREATE INDEX idx_inbound_webhook_enabled ON inbound_webhook_config(is_enabled);
