-- Hibernate Envers Audit Tables for Entity Change History
-- Tracks changes to Task, BugReport, Pitch, and TestCase entities

-- Revision info table (stores metadata about each revision)
CREATE TABLE revinfo (
    rev SERIAL PRIMARY KEY,
    revtstmp BIGINT NOT NULL,
    modified_by VARCHAR(255)
);

-- Tasks audit table (tracks selective fields: status, priority, category, assignee)
CREATE TABLE tasks_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    status VARCHAR(50),
    priority VARCHAR(50),
    category VARCHAR(50),
    assignee_id BIGINT,
    pair_assignee_id BIGINT,
    title VARCHAR(255),
    description TEXT,
    PRIMARY KEY (id, rev)
);

CREATE INDEX idx_tasks_aud_rev ON tasks_aud(rev);
CREATE INDEX idx_tasks_aud_id ON tasks_aud(id);

-- Bug reports audit table (tracks selective fields: status, severity, assignee, resolution)
CREATE TABLE bug_reports_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    status VARCHAR(50),
    severity VARCHAR(50),
    assignee_id BIGINT,
    resolution TEXT,
    title VARCHAR(255),
    description TEXT,
    PRIMARY KEY (id, rev)
);

CREATE INDEX idx_bug_reports_aud_rev ON bug_reports_aud(rev);
CREATE INDEX idx_bug_reports_aud_id ON bug_reports_aud(id);

-- Pitches audit table (tracks selective fields: status, team, circuit breaker)
CREATE TABLE pitches_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    status VARCHAR(50),
    team_id BIGINT,
    is_circuit_breaker_triggered BOOLEAN,
    circuit_breaker_reason TEXT,
    title VARCHAR(255),
    description TEXT,
    PRIMARY KEY (id, rev)
);

CREATE INDEX idx_pitches_aud_rev ON pitches_aud(rev);
CREATE INDEX idx_pitches_aud_id ON pitches_aud(id);

-- Test cases audit table (tracks selective fields: status, priority, type)
CREATE TABLE test_cases_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    status VARCHAR(50),
    priority VARCHAR(50),
    type VARCHAR(50),
    title VARCHAR(255),
    description TEXT,
    PRIMARY KEY (id, rev)
);

CREATE INDEX idx_test_cases_aud_rev ON test_cases_aud(rev);
CREATE INDEX idx_test_cases_aud_id ON test_cases_aud(id);

-- Add comments for documentation
COMMENT ON TABLE revinfo IS 'Hibernate Envers revision metadata - stores timestamp and user for each change';
COMMENT ON TABLE tasks_aud IS 'Audit history for tasks - tracks status, priority, category, and assignee changes';
COMMENT ON TABLE bug_reports_aud IS 'Audit history for bug reports - tracks status, severity, assignee, and resolution changes';
COMMENT ON TABLE pitches_aud IS 'Audit history for pitches - tracks status, team, and circuit breaker changes';
COMMENT ON TABLE test_cases_aud IS 'Audit history for test cases - tracks status, priority, and type changes';
