-- Add a dedicated QA assignee (the person who tests/verifies a bug's fix),
-- distinct from the existing assignee (the person who fixes it).
ALTER TABLE bug_reports ADD COLUMN qa_assignee_id BIGINT;

ALTER TABLE bug_reports
    ADD CONSTRAINT fk_bug_reports_qa_assignee
    FOREIGN KEY (qa_assignee_id) REFERENCES persons (id);

CREATE INDEX IF NOT EXISTS idx_bug_reports_qa_assignee_id ON bug_reports (qa_assignee_id);

-- Mirror the column into the Envers audit table so QA reassignment shows in bug history.
ALTER TABLE bug_reports_aud ADD COLUMN qa_assignee_id BIGINT;
