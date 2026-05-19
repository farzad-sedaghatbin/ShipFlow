-- Hibernate Envers audit table for Task requires project_id because Task.project
-- is annotated @Audited(targetAuditMode=NOT_AUDITED). Without this column the
-- SessionFactory schema validation fails on startup.
-- The column is nullable because historical audit rows predate the project reference.
ALTER TABLE tasks_aud ADD COLUMN IF NOT EXISTS project_id BIGINT;
