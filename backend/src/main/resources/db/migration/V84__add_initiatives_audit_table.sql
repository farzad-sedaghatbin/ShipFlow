-- V84: Add initiatives audit table for Hibernate Envers
-- Tracks changes to Initiative entities (name, status changes)

CREATE TABLE initiatives_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    name VARCHAR(255),
    status VARCHAR(50),
    PRIMARY KEY (id, rev)
);

CREATE INDEX idx_initiatives_aud_rev ON initiatives_aud(rev);
CREATE INDEX idx_initiatives_aud_id ON initiatives_aud(id);

COMMENT ON TABLE initiatives_aud IS 'Audit history for initiatives - tracks name and status changes';
