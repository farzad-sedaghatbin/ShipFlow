-- V42__add_colors_and_bug_config.sql
-- Backward compatibility: Add color and bug configuration columns if they don't exist
-- This migration ensures existing databases are updated with new fields

-- Add colors_json column (will fail silently if exists)
ALTER TABLE organization_settings ADD COLUMN IF NOT EXISTS colors_json TEXT;

-- Add bug_statuses_json column (will fail silently if exists)
ALTER TABLE organization_settings ADD COLUMN IF NOT EXISTS bug_statuses_json TEXT;

-- Add severity_levels_json column (will fail silently if exists)
ALTER TABLE organization_settings ADD COLUMN IF NOT EXISTS severity_levels_json TEXT;

-- Update existing records with default values where NULL
UPDATE organization_settings 
SET 
    colors_json = COALESCE(colors_json, '{"appetiteHours":"#3B82F6","actualHours":"#10B981","overBudget":"#EF4444","underBudget":"#22C55E"}'),
    bug_statuses_json = COALESCE(bug_statuses_json, '[{"name":"NEW","description":"Newly reported","color":"#3B82F6","isActive":true,"order":1,"isClosed":false},{"name":"IN_PROGRESS","description":"Being worked on","color":"#F59E0B","isActive":true,"order":2,"isClosed":false},{"name":"FIXED","description":"Fix implemented","color":"#10B981","isActive":true,"order":3,"isClosed":true},{"name":"VERIFIED","description":"Fix verified","color":"#22C55E","isActive":true,"order":4,"isClosed":true},{"name":"WONT_FIX","description":"Will not fix","color":"#6B7280","isActive":true,"order":5,"isClosed":true}]'),
    severity_levels_json = COALESCE(severity_levels_json, '[{"name":"BLOCKER","description":"Blocks release or critical functionality","color":"#7C3AED","isActive":true,"order":1,"priority":1},{"name":"CRITICAL","description":"System down or data loss","color":"#DC2626","isActive":true,"order":2,"priority":2},{"name":"MAJOR","description":"Major feature broken","color":"#F59E0B","isActive":true,"order":3,"priority":3},{"name":"MINOR","description":"Minor feature issue","color":"#3B82F6","isActive":true,"order":4,"priority":4},{"name":"TRIVIAL","description":"Cosmetic or trivial issue","color":"#10B981","isActive":true,"order":5,"priority":5}]')
WHERE id IS NOT NULL;
