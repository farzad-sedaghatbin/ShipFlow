-- Add widget configurations to dashboard templates
-- This migration adds pre-configured widgets to the system dashboard templates
-- Widgets are configured with data sources to show actual data from the system

-- Get the template dashboard IDs
-- Executive Summary widgets
INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    0, 0, 6, 4, 1,
    '{"title":"Active Cycles","pageSize":5,"searchable":false,"sourceType":"CYCLE_SUMMARY","sortBy":"startDate","sortOrder":"desc","limit":5}'
FROM custom_dashboards d
WHERE d.name = 'Executive Summary' AND d.is_template = true
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    6, 0, 6, 4, 2,
    '{"title":"Team Workload","pageSize":5,"searchable":false,"sourceType":"TEAM_STATS","limit":5}'
FROM custom_dashboards d
WHERE d.name = 'Executive Summary' AND d.is_template = true
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    0, 4, 6, 4, 3,
    '{"title":"Upcoming Deadlines","pageSize":5,"searchable":false,"sourceType":"TASK_LIST","sortBy":"dueDate","sortOrder":"asc","limit":5,"filters":[{"field":"status","operator":"not_equals","value":"COMPLETED"}]}'
FROM custom_dashboards d
WHERE d.name = 'Executive Summary' AND d.is_template = true
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    6, 4, 6, 4, 4,
    '{"title":"Recent Activity","pageSize":5,"searchable":false,"sourceType":"PITCH_LIST","sortBy":"updatedAt","sortOrder":"desc","limit":5}'
FROM custom_dashboards d
WHERE d.name = 'Executive Summary' AND d.is_template = true
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
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    8, 0, 4, 4, 2,
    '{"title":"Recent Activity","pageSize":5,"searchable":false,"sourceType":"TASK_LIST","sortBy":"updatedAt","sortOrder":"desc","limit":5}'
FROM custom_dashboards d
WHERE d.name = 'Developer Dashboard' AND d.is_template = true
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    0, 4, 6, 4, 3,
    '{"title":"Team Workload","pageSize":5,"searchable":false,"sourceType":"TEAM_STATS","limit":5}'
FROM custom_dashboards d
WHERE d.name = 'Developer Dashboard' AND d.is_template = true
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    6, 4, 6, 4, 4,
    '{"title":"Upcoming Deadlines","pageSize":5,"searchable":false,"sourceType":"TASK_LIST","sortBy":"dueDate","sortOrder":"asc","limit":5,"filters":[{"field":"status","operator":"not_equals","value":"COMPLETED"}]}'
FROM custom_dashboards d
WHERE d.name = 'Developer Dashboard' AND d.is_template = true
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
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    6, 0, 6, 4, 2,
    '{"title":"Cycle Progress","pageSize":5,"searchable":false,"sourceType":"CYCLE_SUMMARY","sortBy":"progress","sortOrder":"asc","limit":5}'
FROM custom_dashboards d
WHERE d.name = 'Manager Overview' AND d.is_template = true
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    0, 4, 6, 4, 3,
    '{"title":"Upcoming Deadlines","pageSize":5,"searchable":false,"sourceType":"TASK_LIST","sortBy":"dueDate","sortOrder":"asc","limit":5,"filters":[{"field":"status","operator":"not_equals","value":"COMPLETED"}]}'
FROM custom_dashboards d
WHERE d.name = 'Manager Overview' AND d.is_template = true
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    6, 4, 6, 4, 4,
    '{"title":"Blocked Tasks","pageSize":5,"searchable":true,"sourceType":"TASK_LIST","sortBy":"updatedAt","sortOrder":"desc","limit":5,"filters":[{"field":"status","operator":"equals","value":"BLOCKED"}]}'
FROM custom_dashboards d
WHERE d.name = 'Manager Overview' AND d.is_template = true
LIMIT 1;

-- QA Dashboard widgets
INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    0, 0, 6, 4, 1,
    '{"title":"Blocked Tasks","pageSize":5,"searchable":true,"sourceType":"TASK_LIST","sortBy":"createdAt","sortOrder":"desc","limit":5,"filters":[{"field":"status","operator":"equals","value":"BLOCKED"}]}'
FROM custom_dashboards d
WHERE d.name = 'QA Dashboard' AND d.is_template = true
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    6, 0, 6, 4, 2,
    '{"title":"Overdue Tasks","pageSize":5,"searchable":true,"sourceType":"TASK_LIST","sortBy":"dueDate","sortOrder":"asc","limit":5,"filters":[{"field":"overdue","operator":"equals","value":"true"}]}'
FROM custom_dashboards d
WHERE d.name = 'QA Dashboard' AND d.is_template = true
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    0, 4, 6, 4, 3,
    '{"title":"My Test Tasks","pageSize":5,"searchable":true,"sourceType":"TASK_LIST","sortBy":"priority","sortOrder":"desc","limit":5,"filters":[{"field":"category","operator":"equals","value":"QA"}]}'
FROM custom_dashboards d
WHERE d.name = 'QA Dashboard' AND d.is_template = true
LIMIT 1;

INSERT INTO dashboard_widget_configs (dashboard_id, widget_type, position_x, position_y, width, height, display_order, settings)
SELECT 
    d.id,
    'TABLE',
    6, 4, 6, 4, 4,
    '{"title":"Recent Test Activity","pageSize":5,"searchable":false,"sourceType":"TASK_LIST","sortBy":"updatedAt","sortOrder":"desc","limit":5,"filters":[{"field":"category","operator":"equals","value":"QA"}]}'
FROM custom_dashboards d
WHERE d.name = 'QA Dashboard' AND d.is_template = true
LIMIT 1;


COMMENT ON TABLE dashboard_widget_configs IS 'Widget configurations for custom dashboards - includes template widgets';
