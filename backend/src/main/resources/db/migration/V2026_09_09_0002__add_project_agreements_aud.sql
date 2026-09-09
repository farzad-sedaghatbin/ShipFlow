-- Hibernate Envers audit table for ProjectAgreement entity
-- Required because ProjectAgreement is annotated with @Audited and dev/prod
-- run with hibernate.ddl-auto=validate (tests auto-create it, dev/prod do not).

CREATE TABLE IF NOT EXISTS project_agreements_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    title VARCHAR(255),
    content TEXT,
    agreed_date DATE,
    PRIMARY KEY (id, rev)
);

CREATE INDEX IF NOT EXISTS idx_project_agreements_aud_rev ON project_agreements_aud(rev);
CREATE INDEX IF NOT EXISTS idx_project_agreements_aud_id ON project_agreements_aud(id);
