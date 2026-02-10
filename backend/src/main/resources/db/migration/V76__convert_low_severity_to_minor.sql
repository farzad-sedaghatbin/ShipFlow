-- V76: Convert LOW severity values to MINOR for bug reports and test cases
-- The BugSeverity enum has been updated to only support: TRIVIAL, MINOR, MAJOR, CRITICAL, BLOCKER
-- The LOW value was incorrectly used in seed data and should be MINOR

-- Update bug reports with LOW severity to MINOR
UPDATE bug_reports SET severity = 'MINOR' WHERE severity = 'LOW';

-- Update test cases with LOW priority to MINOR  
UPDATE test_cases SET priority = 'MINOR' WHERE priority = 'LOW';
