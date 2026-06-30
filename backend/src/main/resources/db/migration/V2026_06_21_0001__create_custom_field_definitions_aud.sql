-- Hibernate Envers audit table for CustomFieldDefinition (@Audited).
-- Tracks changes to field name, description, type, entity target, and required flag.
-- @NotAudited fields (project_id, sort_order, options, timestamps) are excluded.
CREATE TABLE custom_field_definitions_aud (
    id          BIGINT      NOT NULL,
    rev         INTEGER     NOT NULL REFERENCES revinfo(rev),
    revtype     SMALLINT,
    name        VARCHAR(255),
    description TEXT,
    field_type  VARCHAR(50),
    entity_type VARCHAR(50),
    required    BOOLEAN,
    PRIMARY KEY (id, rev)
);

CREATE INDEX IF NOT EXISTS idx_cfd_aud_rev ON custom_field_definitions_aud(rev);
