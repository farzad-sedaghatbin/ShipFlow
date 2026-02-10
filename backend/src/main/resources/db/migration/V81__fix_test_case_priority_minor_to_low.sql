-- V81: Revert incorrect MINOR conversion in test_cases priority
-- TestCasePriority enum only supports: LOW, MEDIUM, HIGH, CRITICAL
-- V76 incorrectly converted LOW to MINOR for test_cases (it should have only affected bug_reports)

UPDATE test_cases SET priority = 'LOW' WHERE priority = 'MINOR';
