-- V41__add_organization_settings.sql
-- Add organization-wide settings table for configurable system parameters

CREATE TABLE organization_settings (
    id BIGSERIAL PRIMARY KEY,
    organization_name VARCHAR(255) NOT NULL DEFAULT 'My Organization',
    
    -- Cycle Configuration
    default_cycle_length_weeks INTEGER NOT NULL DEFAULT 6,
    default_cooldown_weeks INTEGER NOT NULL DEFAULT 2,
    
    -- Risk Thresholds (stored as JSON)
    risk_thresholds_json TEXT,
    
    -- Categories (stored as JSON arrays)
    task_categories_json TEXT,
    pitch_categories_json TEXT,
    
    -- Colors (stored as JSON)
    colors_json TEXT,
    
    -- Bug Configuration (stored as JSON arrays)
    bug_statuses_json TEXT,
    severity_levels_json TEXT,
    
    -- Other Settings
    time_zone VARCHAR(100) NOT NULL DEFAULT 'UTC',
    date_format VARCHAR(20) NOT NULL DEFAULT 'MM/DD/YYYY',
    enable_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    enable_ai_features BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255) NOT NULL DEFAULT 'system',
    
    CONSTRAINT chk_cycle_length CHECK (default_cycle_length_weeks >= 1 AND default_cycle_length_weeks <= 12),
    CONSTRAINT chk_cooldown CHECK (default_cooldown_weeks >= 0 AND default_cooldown_weeks <= 4)
);

-- Create index on updated_at for tracking changes
CREATE INDEX idx_organization_settings_updated_at ON organization_settings(updated_at);

-- Insert default settings
INSERT INTO organization_settings (
    organization_name,
    default_cycle_length_weeks,
    default_cooldown_weeks,
    risk_thresholds_json,
    task_categories_json,
    pitch_categories_json,
    colors_json,
    bug_statuses_json,
    severity_levels_json,
    time_zone,
    date_format,
    enable_notifications,
    enable_ai_features,
    updated_by
) VALUES (
    'ShipFlow',
    6,
    2,
    '{"lowMax":30,"mediumMax":60,"highMax":85}',
    '[{"name":"PITCH_SCOPE","description":"Work related to pitch deliverables","color":"#3B82F6","isActive":true,"order":1},{"name":"DEBT_IMPROVEMENT","description":"Technical debt and improvements","color":"#F59E0B","isActive":true,"order":2}]',
    '[{"name":"FEATURE","description":"New feature development","color":"#10B981","isActive":true,"order":1},{"name":"INFRASTRUCTURE","description":"Infrastructure and architecture","color":"#6366F1","isActive":true,"order":2},{"name":"REFACTOR","description":"Code refactoring","color":"#8B5CF6","isActive":true,"order":3},{"name":"BUG_FIX","description":"Bug fixes","color":"#EF4444","isActive":true,"order":4}]',
    '{"appetiteHours":"#3B82F6","actualHours":"#10B981","overBudget":"#EF4444","underBudget":"#22C55E"}',
    '[{"name":"NEW","description":"Newly reported","color":"#3B82F6","isActive":true,"order":1,"isClosed":false},{"name":"IN_PROGRESS","description":"Being worked on","color":"#F59E0B","isActive":true,"order":2,"isClosed":false},{"name":"FIXED","description":"Fix implemented","color":"#10B981","isActive":true,"order":3,"isClosed":true},{"name":"VERIFIED","description":"Fix verified","color":"#22C55E","isActive":true,"order":4,"isClosed":true},{"name":"WONT_FIX","description":"Will not fix","color":"#6B7280","isActive":true,"order":5,"isClosed":true}]',
    '[{"name":"CRITICAL","description":"System down or data loss","color":"#DC2626","isActive":true,"order":1,"priority":1},{"name":"HIGH","description":"Major feature broken","color":"#F59E0B","isActive":true,"order":2,"priority":2},{"name":"MEDIUM","description":"Feature partially broken","color":"#3B82F6","isActive":true,"order":3,"priority":3},{"name":"LOW","description":"Minor issue or cosmetic","color":"#10B981","isActive":true,"order":4,"priority":4}]',
    'UTC',
    'MM/DD/YYYY',
    TRUE,
    TRUE,
    'system'
);

COMMENT ON TABLE organization_settings IS 'Organization-wide configuration settings. Only one record should exist.';
COMMENT ON COLUMN organization_settings.risk_thresholds_json IS 'Risk threshold configuration stored as JSON: {lowMax, mediumMax, highMax}';
COMMENT ON COLUMN organization_settings.task_categories_json IS 'Task categories configuration stored as JSON array';
COMMENT ON COLUMN organization_settings.pitch_categories_json IS 'Pitch categories configuration stored as JSON array';
