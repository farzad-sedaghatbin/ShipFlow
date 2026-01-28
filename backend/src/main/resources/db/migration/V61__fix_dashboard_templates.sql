-- V61__fix_dashboard_templates.sql
-- Fix dashboard templates that may not have been created due to admin user dependency
-- This ensures templates exist regardless of when admin user was created

-- First, check if templates already exist and create them if missing
-- Use a system user (user_id = 0) or the first admin user that exists

-- Create templates using the first admin user if one exists, otherwise use system placeholder
WITH first_admin AS (
    SELECT COALESCE(MIN(id), 0) as admin_id 
    FROM users 
    WHERE role = 'ADMIN' 
    LIMIT 1
)
INSERT INTO custom_dashboards (user_id, name, description, is_template, template_category, layout_config, created_by)
SELECT 
    admin_id,
    'Executive Summary',
    'High-level overview with key metrics, cycle progress, and risk distribution',
    true,
    'EXECUTIVE',
    '{"columns": 12, "rowHeight": 60, "breakpoints": {"lg": 1200, "md": 996, "sm": 768}}',
    admin_id
FROM first_admin
WHERE NOT EXISTS (
    SELECT 1 FROM custom_dashboards 
    WHERE name = 'Executive Summary' AND is_template = true
);

WITH first_admin AS (
    SELECT COALESCE(MIN(id), 0) as admin_id 
    FROM users 
    WHERE role = 'ADMIN' 
    LIMIT 1
)
INSERT INTO custom_dashboards (user_id, name, description, is_template, template_category, layout_config, created_by)
SELECT 
    admin_id,
    'Developer Dashboard',
    'Task-focused view with my tasks, team workload, and recent activity',
    true,
    'DEVELOPER',
    '{"columns": 12, "rowHeight": 60, "breakpoints": {"lg": 1200, "md": 996, "sm": 768}}',
    admin_id
FROM first_admin
WHERE NOT EXISTS (
    SELECT 1 FROM custom_dashboards 
    WHERE name = 'Developer Dashboard' AND is_template = true
);

WITH first_admin AS (
    SELECT COALESCE(MIN(id), 0) as admin_id 
    FROM users 
    WHERE role = 'ADMIN' 
    LIMIT 1
)
INSERT INTO custom_dashboards (user_id, name, description, is_template, template_category, layout_config, created_by)
SELECT 
    admin_id,
    'Manager Overview',
    'Team performance, cycle health, budget tracking, and delivery metrics',
    true,
    'MANAGER',
    '{"columns": 12, "rowHeight": 60, "breakpoints": {"lg": 1200, "md": 996, "sm": 768}}',
    admin_id
FROM first_admin
WHERE NOT EXISTS (
    SELECT 1 FROM custom_dashboards 
    WHERE name = 'Manager Overview' AND is_template = true
);

WITH first_admin AS (
    SELECT COALESCE(MIN(id), 0) as admin_id 
    FROM users 
    WHERE role = 'ADMIN' 
    LIMIT 1
)
INSERT INTO custom_dashboards (user_id, name, description, is_template, template_category, layout_config, created_by)
SELECT 
    admin_id,
    'QA Dashboard',
    'Bug tracking, test coverage, and quality metrics',
    true,
    'QA',
    '{"columns": 12, "rowHeight": 60, "breakpoints": {"lg": 1200, "md": 996, "sm": 768}}',
    admin_id
FROM first_admin
WHERE NOT EXISTS (
    SELECT 1 FROM custom_dashboards 
    WHERE name = 'QA Dashboard' AND is_template = true
);

-- Add dashboard widget configurations for the templates (if they don't exist)
-- Executive Summary widgets
INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    0, 0, 6, 4, 1,
    '{"title":"Active Cycles","pageSize":5,"searchable":false,"sourceType":"CYCLE_SUMMARY","sortBy":"startDate","sortOrder":"desc","limit":5}'
FROM custom_dashboards d
WHERE d.name = 'Executive Summary' AND d.is_template = true
AND NOT EXISTS (
    SELECT 1 FROM dashboard_widget_configs 
    WHERE dashboard_id = d.id AND display_order = 1
)
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    6, 0, 6, 4, 2,
    '{"title":"Team Workload","pageSize":5,"searchable":false,"sourceType":"TEAM_STATS","limit":5}'
FROM custom_dashboards d
WHERE d.name = 'Executive Summary' AND d.is_template = true
AND NOT EXISTS (
    SELECT 1 FROM dashboard_widget_configs 
    WHERE dashboard_id = d.id AND display_order = 2
)
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    0, 4, 6, 4, 3,
    '{"title":"Upcoming Deadlines","pageSize":5,"searchable":false,"sourceType":"TASK_LIST","sortBy":"dueDate","sortOrder":"asc","limit":5,"filters":[{"field":"status","operator":"not_equals","value":"COMPLETED"}]}'
FROM custom_dashboards d
WHERE d.name = 'Executive Summary' AND d.is_template = true
AND NOT EXISTS (
    SELECT 1 FROM dashboard_widget_configs 
    WHERE dashboard_id = d.id AND display_order = 3
)
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    6, 4, 6, 4, 4,
    '{"title":"Recent Activity","pageSize":5,"searchable":false,"sourceType":"PITCH_LIST","sortBy":"updatedAt","sortOrder":"desc","limit":5}'
FROM custom_dashboards d
WHERE d.name = 'Executive Summary' AND d.is_template = true
AND NOT EXISTS (
    SELECT 1 FROM dashboard_widget_configs 
    WHERE dashboard_id = d.id AND display_order = 4
)
LIMIT 1;

-- Developer Dashboard widgets
INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    0, 0, 8, 4, 1,
    '{"title":"My Tasks","pageSize":10,"searchable":true,"sourceType":"TASK_LIST","sortBy":"priority","sortOrder":"desc","limit":10,"filters":[{"field":"status","operator":"in","value":["TODO","IN_PROGRESS"]}]}'
FROM custom_dashboards d
WHERE d.name = 'Developer Dashboard' AND d.is_template = true
AND NOT EXISTS (
    SELECT 1 FROM dashboard_widget_configs 
    WHERE dashboard_id = d.id AND display_order = 1
)
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    8, 0, 4, 4, 2,
    '{"title":"Recent Activity","pageSize":5,"searchable":false,"sourceType":"TASK_LIST","sortBy":"updatedAt","sortOrder":"desc","limit":5}'
FROM custom_dashboards d
WHERE d.name = 'Developer Dashboard' AND d.is_template = true
AND NOT EXISTS (
    SELECT 1 FROM dashboard_widget_configs 
    WHERE dashboard_id = d.id AND display_order = 2
)
LIMIT 1;

-- Manager Overview widgets
INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    0, 0, 6, 4, 1,
    '{"title":"Team Workload","pageSize":8,"searchable":true,"sourceType":"TEAM_STATS","limit":8}'
FROM custom_dashboards d
WHERE d.name = 'Manager Overview' AND d.is_template = true
AND NOT EXISTS (
    SELECT 1 FROM dashboard_widget_configs 
    WHERE dashboard_id = d.id AND display_order = 1
)
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    6, 0, 6, 4, 2,
    '{"title":"Cycle Progress","pageSize":5,"searchable":false,"sourceType":"CYCLE_SUMMARY","sortBy":"startDate","sortOrder":"desc","limit":5}'
FROM custom_dashboards d
WHERE d.name = 'Manager Overview' AND d.is_template = true
AND NOT EXISTS (
    SELECT 1 FROM dashboard_widget_configs 
    WHERE dashboard_id = d.id AND display_order = 2
)
LIMIT 1;

-- QA Dashboard widgets
INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    0, 0, 6, 4, 1,
    '{"title":"My Test Tasks","pageSize":8,"searchable":true,"sourceType":"TASK_LIST","sortBy":"priority","sortOrder":"desc","limit":8,"filters":[{"field":"category","operator":"equals","value":"QA"}]}'
FROM custom_dashboards d
WHERE d.name = 'QA Dashboard' AND d.is_template = true
AND NOT EXISTS (
    SELECT 1 FROM dashboard_widget_configs 
    WHERE dashboard_id = d.id AND display_order = 1
)
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    6, 0, 6, 4, 2,
    '{"title":"Bug Reports","pageSize":5,"searchable":false,"sourceType":"BUG_LIST","sortBy":"severity","sortOrder":"desc","limit":5}'
FROM custom_dashboards d
WHERE d.name = 'QA Dashboard' AND d.is_template = true
AND NOT EXISTS (
    SELECT 1 FROM dashboard_widget_configs 
    WHERE dashboard_id = d.id AND display_order = 2
)
LIMIT 1;

COMMENT ON COLUMN custom_dashboards.user_id IS 'Template owner - can be system user (0) or first admin user';