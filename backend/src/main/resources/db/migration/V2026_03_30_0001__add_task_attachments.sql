-- S07: File attachments on tasks
-- Stores metadata for files attached to tasks; the actual files live on disk
-- under the upload directory configured by app.upload.dir.

CREATE TABLE task_attachments (
    id             BIGSERIAL PRIMARY KEY,
    task_id        BIGINT        NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    file_name      VARCHAR(255)  NOT NULL,
    file_path      TEXT          NOT NULL,
    file_size      BIGINT        NOT NULL,
    content_type   VARCHAR(100)  NOT NULL,
    uploaded_by    BIGINT        REFERENCES users(id) ON DELETE SET NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_task_attachments_task_id ON task_attachments(task_id);
CREATE INDEX idx_task_attachments_uploaded_by ON task_attachments(uploaded_by);
