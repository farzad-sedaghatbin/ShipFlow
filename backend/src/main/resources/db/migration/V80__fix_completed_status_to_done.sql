-- Fix invalid 'COMPLETED' status values to 'DONE'
-- The TaskStatus enum only supports: BACKLOG, TODO, IN_PROGRESS, BLOCKED, IN_REVIEW, DONE, CANCELLED
-- Some seed data may have incorrectly used 'COMPLETED' instead of 'DONE'

UPDATE tasks 
SET status = 'DONE', updated_at = CURRENT_TIMESTAMP 
WHERE status = 'COMPLETED';
