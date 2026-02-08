-- Reset all identity sequences after seed data insertion
-- This ensures auto-generated IDs don't conflict with manually inserted seed data

-- Reset projects sequence to max(id) + 1
SELECT setval(pg_get_serial_sequence('projects', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM projects), false);

-- Reset persons sequence
SELECT setval(pg_get_serial_sequence('persons', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM persons), false);

-- Reset users sequence
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM users), false);

-- Reset cycles sequence
SELECT setval(pg_get_serial_sequence('cycles', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM cycles), false);

-- Reset teams sequence
SELECT setval(pg_get_serial_sequence('teams', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM teams), false);

-- Reset pitches sequence
SELECT setval(pg_get_serial_sequence('pitches', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM pitches), false);

-- Reset hill_chart_points sequence
SELECT setval(pg_get_serial_sequence('hill_chart_points', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM hill_chart_points), false);

-- Reset work_logs sequence
SELECT setval(pg_get_serial_sequence('work_logs', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM work_logs), false);

-- Reset meetings sequence
SELECT setval(pg_get_serial_sequence('meetings', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM meetings), false);

-- Reset tasks sequence
SELECT setval(pg_get_serial_sequence('tasks', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM tasks), false);

-- Reset uploaded_documents sequence
SELECT setval(pg_get_serial_sequence('uploaded_documents', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM uploaded_documents), false);

-- Reset knowledge_items sequence
SELECT setval(pg_get_serial_sequence('knowledge_items', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM knowledge_items), false);

-- Reset qa_interactions sequence
SELECT setval(pg_get_serial_sequence('qa_interactions', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM qa_interactions), false);

-- Reset manual_notes sequence
SELECT setval(pg_get_serial_sequence('manual_notes', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM manual_notes), false);

-- Reset test_cases sequence
SELECT setval(pg_get_serial_sequence('test_cases', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM test_cases), false);

-- Reset bug_reports sequence
SELECT setval(pg_get_serial_sequence('bug_reports', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM bug_reports), false);

-- Reset test_runs sequence
SELECT setval(pg_get_serial_sequence('test_runs', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM test_runs), false);

-- Reset evidences table sequence
SELECT setval(pg_get_serial_sequence('evidences', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM evidences), false);
