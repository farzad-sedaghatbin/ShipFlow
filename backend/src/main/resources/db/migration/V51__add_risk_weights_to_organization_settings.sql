-- V51: Add risk weights configuration to organization settings
-- Allows administrators to configure how much each factor (budget, bugs, scope, time)
-- contributes to the overall risk score calculation

ALTER TABLE organization_settings 
ADD COLUMN risk_weights_json TEXT;

-- Set default balanced weights (25% budget, 30% bugs, 25% scope, 20% time) for existing records
UPDATE organization_settings 
SET risk_weights_json = '{"budgetWeight":25,"bugsWeight":30,"scopeWeight":25,"timeWeight":20}'
WHERE risk_weights_json IS NULL;

-- Note: The weights must sum to 100% (validated in application layer)
-- Available preset profiles: balanced, conservative, aggressive, quality_focused, time_critical
