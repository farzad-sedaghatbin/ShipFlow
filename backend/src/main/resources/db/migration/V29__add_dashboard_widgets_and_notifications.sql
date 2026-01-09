-- V29: Add dashboard widgets and notifications
-- This migration adds support for customizable dashboard widgets and notifications

-- Dashboard Widgets table
CREATE TABLE dashboard_widgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    widget_type VARCHAR(50) NOT NULL,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    layout_config TEXT,
    settings TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_widget (user_id, widget_type),
    INDEX idx_widget_user (user_id),
    INDEX idx_widget_order (user_id, display_order)
);

-- Dashboard Notifications table
CREATE TABLE dashboard_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    action_url VARCHAR(500),
    entity_type VARCHAR(50),
    entity_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notification_user (user_id),
    INDEX idx_notification_read (is_read),
    INDEX idx_notification_created (created_at),
    INDEX idx_notification_type (user_id, type),
    INDEX idx_notification_entity (entity_type, entity_id)
);

-- Create default widgets for existing users
INSERT INTO dashboard_widgets (user_id, widget_type, is_visible, display_order)
SELECT id, 'STATS_CARDS', TRUE, 0 FROM users WHERE id NOT IN (SELECT DISTINCT user_id FROM dashboard_widgets);

INSERT INTO dashboard_widgets (user_id, widget_type, is_visible, display_order)
SELECT id, 'QUICK_LINKS', TRUE, 1 FROM users WHERE id NOT IN (SELECT DISTINCT user_id FROM dashboard_widgets WHERE widget_type = 'QUICK_LINKS');

INSERT INTO dashboard_widgets (user_id, widget_type, is_visible, display_order)
SELECT id, 'ACTIVE_CYCLES', TRUE, 2 FROM users WHERE id NOT IN (SELECT DISTINCT user_id FROM dashboard_widgets WHERE widget_type = 'ACTIVE_CYCLES');

INSERT INTO dashboard_widgets (user_id, widget_type, is_visible, display_order)
SELECT id, 'RECENT_PITCHES', TRUE, 3 FROM users WHERE id NOT IN (SELECT DISTINCT user_id FROM dashboard_widgets WHERE widget_type = 'RECENT_PITCHES');

INSERT INTO dashboard_widgets (user_id, widget_type, is_visible, display_order)
SELECT id, 'HILL_CHART', TRUE, 4 FROM users WHERE id NOT IN (SELECT DISTINCT user_id FROM dashboard_widgets WHERE widget_type = 'HILL_CHART');

INSERT INTO dashboard_widgets (user_id, widget_type, is_visible, display_order)
SELECT id, 'RECENT_ACTIVITY', TRUE, 5 FROM users WHERE id NOT IN (SELECT DISTINCT user_id FROM dashboard_widgets WHERE widget_type = 'RECENT_ACTIVITY');

INSERT INTO dashboard_widgets (user_id, widget_type, is_visible, display_order)
SELECT id, 'RISK_OVERVIEW', TRUE, 6 FROM users WHERE id NOT IN (SELECT DISTINCT user_id FROM dashboard_widgets WHERE widget_type = 'RISK_OVERVIEW');
