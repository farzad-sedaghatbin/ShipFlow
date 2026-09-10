-- Client-generated key that makes create-bug-report requests idempotent. Nullable — old rows
-- and any caller that doesn't send one (e.g. MCP write tools) have no key. A standard SQL unique
-- constraint treats multiple NULLs as distinct, so this doesn't collide with itself.
-- See BugReportService#createBugReport for how it's used.
ALTER TABLE bug_reports ADD COLUMN idempotency_key VARCHAR(100);
ALTER TABLE bug_reports ADD CONSTRAINT uq_bug_report_idempotency_key UNIQUE (idempotency_key);
