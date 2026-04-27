-- V74: Add widgets to Executive Summary template if missing
-- This fixes templates created without widgets in earlier deployments

-- Add Executive Summary Template widgets (only if they don't already exist)
-- We check for existing widgets by counting them for dashboard_id = 5

DO $$
DECLARE
  widget_count INTEGER;
BEGIN
  -- Check if Executive Summary template (dashboard_id = 5) has widgets
  SELECT COUNT(*) INTO widget_count 
  FROM dashboard_widget_configs 
  WHERE dashboard_id = 5;

  -- Only add widgets if template exists and has no widgets
  IF widget_count = 0 AND EXISTS (SELECT 1 FROM custom_dashboards WHERE id = 5 AND is_template = true) THEN
    INSERT INTO dashboard_widget_configs (dashboard_id, widget_id, widget_type, metric_id,
        position_x, position_y, width, height, data_filters, chart_config, settings, display_order, created_at, updated_at)
    VALUES
    (5, NULL, 'CUSTOM_KPI', 1, 0, 0, 1, 1, NULL, NULL,
     '{"title": "Team Velocity", "icon": "trending-up", "color": "#22c55e"}', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    (5, NULL, 'CUSTOM_KPI', 2, 1, 0, 1, 1, NULL, NULL,
     '{"title": "On-Time Delivery", "icon": "check-circle", "color": "#3b82f6"}', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    (5, NULL, 'CUSTOM_KPI', 3, 2, 0, 1, 1, NULL, NULL,
     '{"title": "Bug Escape Rate", "icon": "bug", "color": "#ef4444"}', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    (5, NULL, 'TABLE', NULL, 0, 1, 2, 2, NULL, NULL,
     '{"title": "Active Pitches", "pageSize": 5, "searchable": false, "sourceType": "PITCH_LIST", "sortBy": "createdAt", "sortOrder": "desc", "limit": 5, "filters": [{"field": "status", "operator": "in", "value": ["PENDING", "IN_PROGRESS"]}]}', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    (5, NULL, 'TABLE', NULL, 2, 1, 1, 2, NULL, NULL,
     '{"title": "Critical Bugs", "pageSize": 5, "searchable": false, "sourceType": "BUG_LIST", "sortBy": "severity", "sortOrder": "desc", "limit": 5, "filters": [{"field": "severity", "operator": "equals", "value": "CRITICAL"}]}', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    RAISE NOTICE 'Added 5 widgets to Executive Summary template';
  ELSE
    RAISE NOTICE 'Executive Summary template already has widgets or does not exist';
  END IF;
END $$;
