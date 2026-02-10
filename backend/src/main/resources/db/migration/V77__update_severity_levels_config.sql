-- V77: Update organization settings severity levels to match BugSeverity enum
-- The severity_levels_json configuration must match the actual BugSeverity enum values
-- Old: CRITICAL, HIGH, MEDIUM, LOW
-- New: BLOCKER, CRITICAL, MAJOR, MINOR, TRIVIAL

UPDATE organization_settings 
SET severity_levels_json = '[{"name":"BLOCKER","description":"Blocks release or critical functionality","color":"#7C3AED","isActive":true,"order":1,"priority":1},{"name":"CRITICAL","description":"System down or data loss","color":"#DC2626","isActive":true,"order":2,"priority":2},{"name":"MAJOR","description":"Major feature broken","color":"#F59E0B","isActive":true,"order":3,"priority":3},{"name":"MINOR","description":"Minor feature issue","color":"#3B82F6","isActive":true,"order":4,"priority":4},{"name":"TRIVIAL","description":"Cosmetic or trivial issue","color":"#10B981","isActive":true,"order":5,"priority":5}]'
WHERE severity_levels_json LIKE '%"LOW"%' 
   OR severity_levels_json LIKE '%"MEDIUM"%' 
   OR severity_levels_json LIKE '%"HIGH"%';
