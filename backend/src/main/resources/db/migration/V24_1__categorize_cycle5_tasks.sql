-- V24_1__categorize_cycle5_tasks.sql
-- Update Cycle 5 tasks to set proper category (runs after V24 adds the category column)

-- Update tech debt and improvement tasks to DEBT_IMPROVEMENT category
UPDATE tasks 
SET category = 'DEBT_IMPROVEMENT'
WHERE cycle_id = 5 
  AND (
    tags LIKE '%tech-debt%' 
    OR tags LIKE '%improvement%'
  );

-- Note: All other tasks will retain the default PITCH_SCOPE category set by V24
