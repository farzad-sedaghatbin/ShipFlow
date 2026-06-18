-- Knowledge Center: knowledge_sources table + knowledge_items.knowledge_source_id link
-- Single-org deployment: organization_id intentionally omitted. ORG scope means
-- "visible to all authenticated users".

CREATE TABLE knowledge_sources (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  provider_type VARCHAR(32) NOT NULL,
  scope VARCHAR(16) NOT NULL,
  team_id BIGINT,
  project_id BIGINT,
  config TEXT NOT NULL,
  status VARCHAR(16) NOT NULL,
  last_ingested_at TIMESTAMP WITH TIME ZONE,
  last_error TEXT,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at TIMESTAMP WITH TIME ZONE,
  CONSTRAINT fk_knowledge_sources_team FOREIGN KEY (team_id) REFERENCES teams(id),
  CONSTRAINT fk_knowledge_sources_project FOREIGN KEY (project_id) REFERENCES projects(id),
  CONSTRAINT fk_knowledge_sources_user FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_sources_team ON knowledge_sources(team_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_sources_project ON knowledge_sources(project_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_sources_status ON knowledge_sources(status);

ALTER TABLE knowledge_items ADD COLUMN knowledge_source_id BIGINT;
ALTER TABLE knowledge_items ADD CONSTRAINT fk_knowledge_items_source
  FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources(id);
CREATE INDEX IF NOT EXISTS idx_knowledge_items_source ON knowledge_items(knowledge_source_id);
