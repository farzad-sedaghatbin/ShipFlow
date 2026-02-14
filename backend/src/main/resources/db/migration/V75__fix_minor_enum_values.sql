-- V75: Fix MINOR enum values to LOW for bug reports and test cases
-- The TestCasePriority and BugSeverity enums only support: LOW, MEDIUM, HIGH, CRITICAL
-- Existing seed data incorrectly used MINOR which needs to be changed to LOW

-- Update bug reports with MINOR severity to LOW
UPDATE bug_reports SET severity = 'LOW' WHERE severity = 'MINOR';

-- Update test cases with MINOR priority to LOW  
UPDATE test_cases SET priority = 'LOW' WHERE priority = 'MINOR';
