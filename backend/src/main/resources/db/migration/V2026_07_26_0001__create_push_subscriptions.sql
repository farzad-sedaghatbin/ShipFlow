-- Web Push notification delivery (v1.11.0 Mobile PWA)
-- Stores one row per browser subscription (endpoint + encryption keys) registered via the
-- frontend service worker (frontend/src/sw.ts). A user may have multiple subscriptions
-- (one per browser/device). The endpoint URL is globally unique per browser subscription,
-- so re-subscribing the same browser upserts rather than duplicating.

CREATE TABLE push_subscriptions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    endpoint TEXT NOT NULL,
    p256dh_key TEXT NOT NULL,
    auth_key TEXT NOT NULL,
    user_agent VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_push_subscriptions_user ON push_subscriptions(user_id);

-- A browser subscription endpoint is globally unique; re-subscribing the same browser upserts
-- (see PushSubscriptionService) instead of creating a duplicate row.
ALTER TABLE push_subscriptions ADD CONSTRAINT uq_push_subscriptions_endpoint UNIQUE (endpoint);

-- Per-user opt-out of Web Push delivery (defaults to enabled so existing users start subscribed).
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS push_enabled BOOLEAN NOT NULL DEFAULT TRUE;
