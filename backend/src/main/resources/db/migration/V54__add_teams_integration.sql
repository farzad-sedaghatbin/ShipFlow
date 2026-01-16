-- Microsoft Teams Integration Tables

-- Teams workspace configuration table
CREATE TABLE teams_configuration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_name VARCHAR(255) NOT NULL,
    webhook_url VARCHAR(1000) NOT NULL,
    default_channel VARCHAR(255),
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_teams_tenant UNIQUE (tenant_name)
);

-- Teams channel notification configuration
CREATE TABLE teams_channel_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teams_config_id BIGINT NOT NULL,
    channel_name VARCHAR(255) NOT NULL,
    channel_webhook_url VARCHAR(1000),
    notify_task_assigned BOOLEAN NOT NULL DEFAULT TRUE,
    notify_task_completed BOOLEAN NOT NULL DEFAULT TRUE,
    notify_task_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    notify_pitch_shaped BOOLEAN NOT NULL DEFAULT TRUE,
    notify_cycle_started BOOLEAN NOT NULL DEFAULT TRUE,
    notify_cycle_cooldown BOOLEAN NOT NULL DEFAULT TRUE,
    notify_betting_completed BOOLEAN NOT NULL DEFAULT FALSE,
    notify_sprint_started BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_teams_channel_config FOREIGN KEY (teams_config_id) REFERENCES teams_configuration(id) ON DELETE CASCADE,
    CONSTRAINT uk_teams_channel UNIQUE (teams_config_id, channel_name)
);

-- Teams notification history (for audit and troubleshooting)
CREATE TABLE teams_notification_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teams_config_id BIGINT NOT NULL,
    channel_name VARCHAR(255),
    notification_type VARCHAR(100) NOT NULL,
    message_text CLOB,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    sent_at TIMESTAMP NOT NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message CLOB,
    CONSTRAINT fk_teams_history_config FOREIGN KEY (teams_config_id) REFERENCES teams_configuration(id) ON DELETE CASCADE
);

-- Create indices for better query performance
CREATE INDEX idx_teams_history_sent_at ON teams_notification_history(sent_at);
CREATE INDEX idx_teams_history_entity ON teams_notification_history(entity_type, entity_id);
CREATE INDEX idx_teams_history_type ON teams_notification_history(notification_type);
