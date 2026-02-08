-- Migration to change bug_reports.assignee_id from users to persons reference
-- This aligns with the Task entity which also uses Person for assignee

-- Drop the existing foreign key constraint
ALTER TABLE bug_reports DROP CONSTRAINT IF EXISTS bug_reports_assignee_id_fkey;

-- Add new foreign key constraint referencing persons table
ALTER TABLE bug_reports 
    ADD CONSTRAINT bug_reports_assignee_id_fkey 
    FOREIGN KEY (assignee_id) REFERENCES persons(id);
