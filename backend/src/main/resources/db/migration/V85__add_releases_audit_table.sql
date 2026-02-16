-- V85: Add releases audit table for Hibernate Envers
-- Tracks changes to Release entities (name, status, risk_level changes)

CREATE TABLE releases_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    name VARCHAR(255),
    status VARCHAR(50),
    risk_level VARCHAR(50),
    PRIMARY KEY (id, rev)
);

CREATE INDEX idx_releases_aud_rev ON releases_aud(rev);
CREATE INDEX idx_releases_aud_id ON releases_aud(id);

COMMENT ON TABLE releases_aud IS 'Audit history for releases - tracks name, status and risk level changes';
