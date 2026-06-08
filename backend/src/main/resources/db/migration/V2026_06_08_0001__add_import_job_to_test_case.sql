-- Add import_job_id to test_cases so each imported row can be traced back to its import job.
-- FK constraint DDL is intentionally omitted for H2 test compatibility;
-- JPA @JoinColumn enforces the relationship at the ORM level in PostgreSQL.
ALTER TABLE test_cases ADD COLUMN IF NOT EXISTS import_job_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_test_cases_import_job_id ON test_cases(import_job_id);
