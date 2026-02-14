-- Fix Developer Dashboard Template Widget Sizes
-- Updates widget positions and sizes for better layout
-- Affects dashboard_id = 6 (Developer Template in seed data)

-- Fix "My Active Tasks" widget sizing (TABLE widget)
-- Change from position (0,0) size (2,2) to position (0,0) size (8,4)
UPDATE dashboard_widget_configs 
SET width = 8, 
    height = 4
WHERE dashboard_id IN (
    SELECT id FROM custom_dashboards 
    WHERE name = 'Developer Dashboard' 
    AND is_template = true
)
AND widget_type = 'TABLE'
AND settings LIKE '%My Active Tasks%'
AND width = 2
AND height = 2;

-- Fix "Code Review Time" CUSTOM_KPI widget positioning  
-- Change from position (0,2) size (1,1) to position (8,0) size (2,2)
UPDATE dashboard_widget_configs 
SET position_x = 8,
    position_y = 0,
    width = 2,
    height = 2
WHERE dashboard_id IN (
    SELECT id FROM custom_dashboards 
    WHERE name = 'Developer Dashboard' 
    AND is_template = true
)
AND widget_type = 'CUSTOM_KPI'
AND settings LIKE '%Code Review Time%'
AND width = 1
AND height = 1;

-- Fix "Tech Debt" CUSTOM_KPI widget positioning
-- Change from position (1,2) size (1,1) to position (10,0) size (2,2)
UPDATE dashboard_widget_configs 
SET position_x = 10,
    position_y = 0,
    width = 2,
    height = 2
WHERE dashboard_id IN (
    SELECT id FROM custom_dashboards 
    WHERE name = 'Developer Dashboard' 
    AND is_template = true
)
AND widget_type = 'CUSTOM_KPI'
AND settings LIKE '%Tech Debt%'
AND width = 1
AND height = 1;

-- Fix widget sizes for user-created dashboards from Developer template
-- This updates dashboards that users created from the template
UPDATE dashboard_widget_configs dwc
SET width = 8, 
    height = 4
WHERE dwc.dashboard_id IN (
    SELECT cd.id FROM custom_dashboards cd
    WHERE cd.name LIKE '%Developer%'
    AND cd.is_template = false
)
AND dwc.widget_type = 'TABLE'
AND dwc.settings LIKE '%My Active Tasks%'
AND dwc.width = 2
AND dwc.height = 2;

UPDATE dashboard_widget_configs dwc
SET position_x = 8,
    position_y = 0,
    width = 2,
    height = 2
WHERE dwc.dashboard_id IN (
    SELECT cd.id FROM custom_dashboards cd
    WHERE cd.name LIKE '%Developer%'
    AND cd.is_template = false
)
AND dwc.widget_type = 'CUSTOM_KPI'
AND dwc.settings LIKE '%Code Review Time%'
AND dwc.width = 1
AND dwc.height = 1;

UPDATE dashboard_widget_configs dwc
SET position_x = 10,
    position_y = 0,
    width = 2,
    height = 2
WHERE dwc.dashboard_id IN (
    SELECT cd.id FROM custom_dashboards cd
    WHERE cd.name LIKE '%Developer%'
    AND cd.is_template = false
)
AND dwc.widget_type = 'CUSTOM_KPI'
AND dwc.settings LIKE '%Tech Debt%'
AND dwc.width = 1
AND dwc.height = 1;
