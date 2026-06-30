-- Hibernate Envers audit table for KnowledgeSource entity
-- Required because KnowledgeSource is annotated with @Audited and dev/prod
-- run with hibernate.ddl-auto=validate (tests auto-create it, dev/prod do not).

CREATE TABLE IF NOT EXISTS knowledge_sources_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    name VARCHAR(255),
    description TEXT,
    provider_type VARCHAR(32),
    scope VARCHAR(16),
    team_id BIGINT,
    project_id BIGINT,
    config TEXT,
    status VARCHAR(16),
    last_ingested_at TIMESTAMP WITH TIME ZONE,
    last_error TEXT,
    created_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id, rev)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_sources_aud_rev ON knowledge_sources_aud(rev);
CREATE INDEX IF NOT EXISTS idx_knowledge_sources_aud_id ON knowledge_sources_aud(id);
