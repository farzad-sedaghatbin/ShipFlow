-- Fix QA Dashboard widget filters to use valid field values
-- Problem: Original widgets used non-existent fields (category='QA', overdue='true')
-- Solution: Update to use valid status and category values

-- Widget 1: Blocked Tasks (keep as is - BLOCKED status exists)
UPDATE dashboard_widget_configs
SET settings = '{"title":"Blocked Tasks","pageSize":10,"searchable":true,"sourceType":"TASK_LIST","sortBy":"createdAt","sortOrder":"desc","limit":10,"filters":[{"field":"status","operator":"equals","value":"BLOCKED"}]}'
WHERE dashboard_id IN (SELECT id FROM custom_dashboards WHERE name = 'QA Dashboard' AND is_template = true)
  AND settings LIKE '%Blocked Tasks%';

-- Widget 2: High Priority Tasks (replace Overdue Tasks)
UPDATE dashboard_widget_configs
SET settings = '{"title":"High Priority Tasks","pageSize":10,"searchable":true,"sourceType":"TASK_LIST","sortBy":"priority","sortOrder":"desc","limit":10,"filters":[{"field":"priority","operator":"equals","value":"HIGH"}]}'
WHERE dashboard_id IN (SELECT id FROM custom_dashboards WHERE name = 'QA Dashboard' AND is_template = true)
  AND settings LIKE '%Overdue Tasks%';

-- Widget 3: In Progress Tasks (replace My Test Tasks)
UPDATE dashboard_widget_configs
SET settings = '{"title":"In Progress Tasks","pageSize":10,"searchable":true,"sourceType":"TASK_LIST","sortBy":"updatedAt","sortOrder":"desc","limit":10,"filters":[{"field":"status","operator":"equals","value":"IN_PROGRESS"}]}'
WHERE dashboard_id IN (SELECT id FROM custom_dashboards WHERE name = 'QA Dashboard' AND is_template = true)
  AND settings LIKE '%My Test Tasks%';

-- Widget 4: Recently Completed (replace Recent Test Activity)
UPDATE dashboard_widget_configs
SET settings = '{"title":"Recently Completed","pageSize":10,"searchable":true,"sourceType":"TASK_LIST","sortBy":"updatedAt","sortOrder":"desc","limit":10,"filters":[{"field":"status","operator":"equals","value":"DONE"}]}'
WHERE dashboard_id IN (SELECT id FROM custom_dashboards WHERE name = 'QA Dashboard' AND is_template = true)
  AND settings LIKE '%Recent Test Activity%';

COMMENT ON TABLE dashboard_widget_configs IS 'Widget configurations for custom dashboards - filters updated to use valid enum values';
