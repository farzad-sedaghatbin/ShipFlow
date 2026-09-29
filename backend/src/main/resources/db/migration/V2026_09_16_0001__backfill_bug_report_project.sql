-- Backfill bug_reports.project_id for bugs that were created without one.
--
-- Every bug list, board and backlog view is project-scoped (see BugReportSpecification:
-- project_id = ? OR cycle.project_id = ?), so a bug with a NULL project_id is invisible in the
-- UI unless the viewer switches to "All Projects" — it can otherwise only be opened by its
-- direct URL. BugReportService now refuses to create such a bug, but rows created before that
-- guard still exist.
--
-- This recovers every orphan whose project can be derived from something it is already linked
-- to. Orphans with no cycle, pitch, test run or task at all cannot be derived and are left
-- alone — reassign those from the UI ("Move" on the bug detail dialog).

-- 1. Derive from the bug's own cycle.
UPDATE bug_reports
SET project_id = (SELECT c.project_id FROM cycles c WHERE c.id = bug_reports.cycle_id)
WHERE project_id IS NULL
  AND cycle_id IS NOT NULL
  AND (SELECT c.project_id FROM cycles c WHERE c.id = bug_reports.cycle_id) IS NOT NULL;

-- 2. Derive from the pitch's cycle.
UPDATE bug_reports
SET project_id = (SELECT c.project_id
                  FROM pitches p
                  JOIN cycles c ON c.id = p.cycle_id
                  WHERE p.id = bug_reports.pitch_id)
WHERE project_id IS NULL
  AND pitch_id IS NOT NULL
  AND (SELECT c.project_id
       FROM pitches p
       JOIN cycles c ON c.id = p.cycle_id
       WHERE p.id = bug_reports.pitch_id) IS NOT NULL;

-- 3. Derive from the linked task, directly or through the task's cycle.
UPDATE bug_reports
SET project_id = (SELECT COALESCE(t.project_id, (SELECT c.project_id FROM cycles c WHERE c.id = t.cycle_id))
                  FROM tasks t
                  WHERE t.id = bug_reports.task_id)
WHERE project_id IS NULL
  AND task_id IS NOT NULL
  AND (SELECT COALESCE(t.project_id, (SELECT c.project_id FROM cycles c WHERE c.id = t.cycle_id))
       FROM tasks t
       WHERE t.id = bug_reports.task_id) IS NOT NULL;

-- 4. Derive from the test run the bug was filed from (its cycle, else its pitch's cycle).
UPDATE bug_reports
SET project_id = (SELECT COALESCE(
                           (SELECT c.project_id FROM cycles c WHERE c.id = tr.cycle_id),
                           (SELECT c2.project_id FROM pitches p JOIN cycles c2 ON c2.id = p.cycle_id WHERE p.id = tr.pitch_id))
                  FROM test_runs tr
                  WHERE tr.id = bug_reports.test_run_id)
WHERE project_id IS NULL
  AND test_run_id IS NOT NULL
  AND (SELECT COALESCE(
                 (SELECT c.project_id FROM cycles c WHERE c.id = tr.cycle_id),
                 (SELECT c2.project_id FROM pitches p JOIN cycles c2 ON c2.id = p.cycle_id WHERE p.id = tr.pitch_id))
       FROM test_runs tr
       WHERE tr.id = bug_reports.test_run_id) IS NOT NULL;
