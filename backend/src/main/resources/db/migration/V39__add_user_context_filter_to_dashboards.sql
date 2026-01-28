-- Add user context filter toggle to custom dashboards
-- This allows users to toggle between personal context (their data) and organization-wide view

ALTER TABLE custom_dashboards ADD COLUMN IF NOT EXISTS user_context_filter BOOLEAN DEFAULT FALSE;

-- Update existing dashboards to have the filter disabled by default (show all data)
UPDATE custom_dashboards SET user_context_filter = FALSE WHERE user_context_filter IS NULL;

-- Developer Dashboard templates should default to enabled (personal context)
UPDATE custom_dashboards 
SET user_context_filter = TRUE 
WHERE is_template = TRUE AND name = 'Developer Dashboard';

-- QA Dashboard templates should default to enabled (personal context)
UPDATE custom_dashboards 
SET user_context_filter = TRUE 
WHERE is_template = TRUE AND name = 'QA Dashboard';
