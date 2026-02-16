-- V94: Fix any remaining MINOR priority values in test_cases
-- TestCasePriority enum only supports: LOW, MEDIUM, HIGH, CRITICAL
-- This is a safety migration to catch any MINOR values that weren't fixed by V75 or V81

-- Update test cases with MINOR priority to LOW
UPDATE test_cases SET priority = 'LOW' WHERE priority = 'MINOR';

-- Ensure no MINOR values exist in bug_reports severity either
UPDATE bug_reports SET severity = 'LOW' WHERE severity = 'MINOR';
