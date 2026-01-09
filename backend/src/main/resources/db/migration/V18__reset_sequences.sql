-- Reset all identity sequences after seed data insertion
-- This ensures auto-generated IDs don't conflict with manually inserted seed data

-- Reset projects sequence to max(id) + 1
ALTER TABLE projects ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM projects);

-- Reset persons sequence
ALTER TABLE persons ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM persons);

-- Reset users sequence
ALTER TABLE users ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM users);

-- Reset cycles sequence
ALTER TABLE cycles ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM cycles);

-- Reset teams sequence
ALTER TABLE teams ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM teams);

-- Reset pitches sequence
ALTER TABLE pitches ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM pitches);

-- Reset hill_chart_points sequence
ALTER TABLE hill_chart_points ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM hill_chart_points);

-- Reset work_logs sequence
ALTER TABLE work_logs ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM work_logs);

-- Reset meetings sequence
ALTER TABLE meetings ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM meetings);

-- Reset tasks sequence
ALTER TABLE tasks ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM tasks);

-- Reset uploaded_documents sequence
ALTER TABLE uploaded_documents ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM uploaded_documents);

-- Reset knowledge_items sequence
ALTER TABLE knowledge_items ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM knowledge_items);

-- Reset qa_interactions sequence
ALTER TABLE qa_interactions ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM qa_interactions);

-- Reset manual_notes sequence
ALTER TABLE manual_notes ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM manual_notes);

-- Reset test_cases sequence
ALTER TABLE test_cases ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM test_cases);

-- Reset bug_reports sequence
ALTER TABLE bug_reports ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM bug_reports);

-- Reset test_runs sequence
ALTER TABLE test_runs ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM test_runs);

-- Reset evidences table sequence
ALTER TABLE evidences ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM evidences);
