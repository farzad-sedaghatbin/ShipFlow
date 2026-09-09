-- Project Agreements: a per-project list of discrete, dated agreement/contract-term
-- entries. Not everything from a meeting should become a Task — some things are
-- agreements ("we agreed X") rather than work items. Each entry is optionally linked
-- to the Meeting it originated from; most agreements are logged directly with no
-- meeting at all.

CREATE TABLE project_agreements (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  project_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  meeting_id BIGINT,
  agreed_date DATE NOT NULL,
  created_by_id BIGINT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at TIMESTAMP WITH TIME ZONE,
  deleted_by_id BIGINT,
  CONSTRAINT fk_project_agreements_project FOREIGN KEY (project_id) REFERENCES projects(id),
  CONSTRAINT fk_project_agreements_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id),
  CONSTRAINT fk_project_agreements_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL,
  CONSTRAINT fk_project_agreements_deleted_by FOREIGN KEY (deleted_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_project_agreements_project ON project_agreements(project_id);
CREATE INDEX IF NOT EXISTS idx_project_agreements_meeting ON project_agreements(meeting_id);
CREATE INDEX IF NOT EXISTS idx_project_agreements_deleted_at ON project_agreements(deleted_at);
