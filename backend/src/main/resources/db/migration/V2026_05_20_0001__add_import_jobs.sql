-- V2026_05_20_0001__add_import_jobs.sql
-- Competitor migration tooling (v1.2.0): tracks CSV import jobs from Jira/Linear/Asana

CREATE TABLE import_jobs (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  file_name VARCHAR(255) NOT NULL,
  source_format VARCHAR(50) NOT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  total_rows INTEGER NOT NULL DEFAULT 0,
  imported_rows INTEGER NOT NULL DEFAULT 0,
  failed_rows INTEGER NOT NULL DEFAULT 0,
  error_log TEXT,
  project_id BIGINT,
  created_by_id BIGINT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  completed_at TIMESTAMP WITH TIME ZONE,
  CONSTRAINT fk_import_jobs_project FOREIGN KEY (project_id) REFERENCES projects(id),
  CONSTRAINT fk_import_jobs_user FOREIGN KEY (created_by_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_import_jobs_created_by ON import_jobs(created_by_id);
CREATE INDEX IF NOT EXISTS idx_import_jobs_status ON import_jobs(status);
