-- Stores per-user external IDs for notification providers (Slack, Teams, etc.)
-- Each user can have one external ID per provider for @mention support.
CREATE TABLE notification_user_mapping (
    id BIGSERIAL PRIMARY KEY,
    person_id BIGINT NOT NULL,
    provider_name VARCHAR(50) NOT NULL,
    external_user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_notification_user_mapping_person
        FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE,
    CONSTRAINT uq_notification_user_mapping
        UNIQUE (person_id, provider_name)
);

CREATE INDEX idx_notification_user_mapping_person ON notification_user_mapping(person_id);
CREATE INDEX idx_notification_user_mapping_provider ON notification_user_mapping(provider_name);
