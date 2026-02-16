-- V83: Add epics audit table for Hibernate Envers
-- Tracks changes to Epic entities (name, status changes)

CREATE TABLE epics_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    name VARCHAR(255),
    status VARCHAR(50),
    PRIMARY KEY (id, rev)
);

CREATE INDEX idx_epics_aud_rev ON epics_aud(rev);
CREATE INDEX idx_epics_aud_id ON epics_aud(id);

COMMENT ON TABLE epics_aud IS 'Audit history for epics - tracks name and status changes';
