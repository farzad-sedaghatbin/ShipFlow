-- ============================================================================
-- Custom Metrics and Dashboard Customization Feature
-- ============================================================================
-- This migration adds support for:
-- 1. Custom metrics with formula-based calculations
-- 2. Multiple custom dashboards per user
-- 3. Dashboard templates for quick setup
-- 4. Enhanced widget configuration with filters and chart settings
-- ============================================================================

-- Custom Metrics Table
CREATE TABLE custom_metrics (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    formula TEXT NOT NULL,
    data_source VARCHAR(50) NOT NULL, -- PITCH, CYCLE, TASK, WORK_LOG, CUSTOM
    aggregation_type VARCHAR(20), -- SUM, AVG, COUNT, MIN, MAX, RATIO
    filters TEXT, -- JSON: {"cycleId": 1, "teamIds": [1,2], "dateRange": {...}}
    display_format VARCHAR(20) DEFAULT 'NUMBER', -- NUMBER, PERCENTAGE, CURRENCY, DURATION
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_user_metric_name UNIQUE (user_id, name)
);

CREATE INDEX idx_custom_metrics_user_id ON custom_metrics(user_id);
CREATE INDEX idx_custom_metrics_active ON custom_metrics(is_active);

-- Metric Data Points (historical values)
CREATE TABLE metric_data_points (
    id BIGSERIAL PRIMARY KEY,
    metric_id BIGINT NOT NULL,
    calculated_value DECIMAL(20, 4),
    calculation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT, -- JSON: {"cycleId": 1, "pitchCount": 10, etc.}
    FOREIGN KEY (metric_id) REFERENCES custom_metrics(id) ON DELETE CASCADE
);

CREATE INDEX idx_metric_data_points_metric_id ON metric_data_points(metric_id);
CREATE INDEX idx_metric_data_points_date ON metric_data_points(calculation_date DESC);

-- Custom Dashboards Table
CREATE TABLE custom_dashboards (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT false,
    layout_config TEXT, -- JSON: grid layout configuration
    is_template BOOLEAN DEFAULT false, -- System templates vs user dashboards
    template_category VARCHAR(50), -- EXECUTIVE, DEVELOPER, MANAGER, QA, CUSTOM
    created_by BIGINT, -- For templates created by admins
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT unique_user_dashboard_name UNIQUE (user_id, name)
);

CREATE INDEX idx_custom_dashboards_user_id ON custom_dashboards(user_id);
CREATE INDEX idx_custom_dashboards_default ON custom_dashboards(user_id, is_default);
CREATE INDEX idx_custom_dashboards_templates ON custom_dashboards(is_template);

-- Dashboard Widget Configurations (links dashboards to widgets)
CREATE TABLE dashboard_widget_configs (
    id BIGSERIAL PRIMARY KEY,
    dashboard_id BIGINT NOT NULL,
    widget_id BIGINT, -- References existing dashboard_widgets (can be NULL for custom widgets)
    widget_type VARCHAR(50) NOT NULL, -- CUSTOM_KPI, CHART, TABLE, etc.
    metric_id BIGINT, -- For CUSTOM_KPI widgets
    position_x INTEGER DEFAULT 0,
    position_y INTEGER DEFAULT 0,
    width INTEGER DEFAULT 1,
    height INTEGER DEFAULT 1,
    data_filters TEXT, -- JSON: {"teamIds": [1,2], "cycleIds": [3], "dateRange": {...}}
    chart_config TEXT, -- JSON: {"chartType": "BAR", "colors": [...], "legend": {...}}
    refresh_interval INTEGER, -- Seconds (NULL = no auto-refresh)
    custom_query TEXT, -- Advanced: custom SQL-like query
    settings TEXT, -- JSON: widget-specific settings
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (dashboard_id) REFERENCES custom_dashboards(id) ON DELETE CASCADE,
    FOREIGN KEY (widget_id) REFERENCES dashboard_widgets(id) ON DELETE SET NULL,
    FOREIGN KEY (metric_id) REFERENCES custom_metrics(id) ON DELETE SET NULL
);

CREATE INDEX idx_dashboard_widget_configs_dashboard_id ON dashboard_widget_configs(dashboard_id);
CREATE INDEX idx_dashboard_widget_configs_widget_id ON dashboard_widget_configs(widget_id);
CREATE INDEX idx_dashboard_widget_configs_metric_id ON dashboard_widget_configs(metric_id);

-- Enhance existing dashboard_widgets table with new fields
ALTER TABLE dashboard_widgets ADD COLUMN IF NOT EXISTS data_filters TEXT;
ALTER TABLE dashboard_widgets ADD COLUMN IF NOT EXISTS chart_config TEXT;
ALTER TABLE dashboard_widgets ADD COLUMN IF NOT EXISTS refresh_interval INTEGER;

-- Create default dashboard templates
INSERT INTO custom_dashboards (user_id, name, description, is_template, template_category, layout_config, created_by)
SELECT 
    1, -- Admin user
    'Executive Summary',
    'High-level overview with key metrics, cycle progress, and risk distribution',
    true,
    'EXECUTIVE',
    '{"columns": 12, "rowHeight": 60, "breakpoints": {"lg": 1200, "md": 996, "sm": 768}}',
    1
WHERE EXISTS (SELECT 1 FROM users WHERE id = 1);

INSERT INTO custom_dashboards (user_id, name, description, is_template, template_category, layout_config, created_by)
SELECT 
    1,
    'Developer Dashboard',
    'Task-focused view with my tasks, team workload, and recent activity',
    true,
    'DEVELOPER',
    '{"columns": 12, "rowHeight": 60, "breakpoints": {"lg": 1200, "md": 996, "sm": 768}}',
    1
WHERE EXISTS (SELECT 1 FROM users WHERE id = 1);

INSERT INTO custom_dashboards (user_id, name, description, is_template, template_category, layout_config, created_by)
SELECT 
    1,
    'Manager Overview',
    'Team performance, cycle health, budget tracking, and delivery metrics',
    true,
    'MANAGER',
    '{"columns": 12, "rowHeight": 60, "breakpoints": {"lg": 1200, "md": 996, "sm": 768}}',
    1
WHERE EXISTS (SELECT 1 FROM users WHERE id = 1);

INSERT INTO custom_dashboards (user_id, name, description, is_template, template_category, layout_config, created_by)
SELECT 
    1,
    'QA Dashboard',
    'Bug tracking, test coverage, and quality metrics',
    true,
    'QA',
    '{"columns": 12, "rowHeight": 60, "breakpoints": {"lg": 1200, "md": 996, "sm": 768}}',
    1
WHERE EXISTS (SELECT 1 FROM users WHERE id = 1);

-- Create sample custom metrics for demo purposes
INSERT INTO custom_metrics (user_id, name, description, formula, data_source, aggregation_type, display_format)
SELECT 
    1,
    'Delivery Success Rate',
    'Percentage of pitches delivered on time and within appetite',
    '(COUNT(pitch WHERE status=DONE AND actualHours <= appetiteHours) / COUNT(pitch WHERE status=DONE)) * 100',
    'PITCH',
    'RATIO',
    'PERCENTAGE'
WHERE EXISTS (SELECT 1 FROM users WHERE id = 1);

INSERT INTO custom_metrics (user_id, name, description, formula, data_source, aggregation_type, display_format)
SELECT 
    1,
    'Average Cycle Velocity',
    'Average number of pitches completed per cycle',
    'COUNT(pitch WHERE status=DONE) / COUNT(DISTINCT cycle)',
    'CYCLE',
    'AVG',
    'NUMBER'
WHERE EXISTS (SELECT 1 FROM users WHERE id = 1);

INSERT INTO custom_metrics (user_id, name, description, formula, data_source, aggregation_type, display_format)
SELECT 
    1,
    'Team Utilization',
    'Percentage of available capacity being used',
    '(SUM(workLog.hours) / (COUNT(DISTINCT person) * 40 * 6)) * 100',
    'WORK_LOG',
    'RATIO',
    'PERCENTAGE'
WHERE EXISTS (SELECT 1 FROM users WHERE id = 1);

COMMENT ON TABLE custom_metrics IS 'User-defined custom metrics with formula-based calculations';
COMMENT ON TABLE metric_data_points IS 'Historical calculated values for custom metrics';
COMMENT ON TABLE custom_dashboards IS 'User-created dashboards and system templates';
COMMENT ON TABLE dashboard_widget_configs IS 'Widget configurations for custom dashboards';
