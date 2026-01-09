-- V16: Add tables for QA Test Management feature

-- Test cases table
CREATE TABLE test_cases (
    id BIGSERIAL PRIMARY KEY,
    test_case_key VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    preconditions TEXT,
    steps TEXT,
    expected_result TEXT,
    pitch_id BIGINT REFERENCES pitches(id),
    cycle_id BIGINT REFERENCES cycles(id),
    team_id BIGINT REFERENCES teams(id),
    type VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    tags VARCHAR(500),
    estimated_minutes INTEGER,
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    updated_by_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for test cases
CREATE INDEX idx_test_case_pitch ON test_cases(pitch_id);
CREATE INDEX idx_test_case_cycle ON test_cases(cycle_id);
CREATE INDEX idx_test_case_status ON test_cases(status);
CREATE INDEX idx_test_case_created_by ON test_cases(created_by_id);
CREATE INDEX idx_test_case_type ON test_cases(type);
CREATE INDEX idx_test_case_priority ON test_cases(priority);

-- Test runs table
CREATE TABLE test_runs (
    id BIGSERIAL PRIMARY KEY,
    test_case_id BIGINT NOT NULL REFERENCES test_cases(id),
    cycle_id BIGINT REFERENCES cycles(id),
    pitch_id BIGINT REFERENCES pitches(id),
    status VARCHAR(20) NOT NULL,
    executed_by_id BIGINT NOT NULL REFERENCES users(id),
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration_seconds INTEGER,
    notes TEXT,
    actual_result TEXT,
    build_version VARCHAR(255),
    environment VARCHAR(500),
    attachments TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for test runs
CREATE INDEX idx_test_run_test_case ON test_runs(test_case_id);
CREATE INDEX idx_test_run_cycle ON test_runs(cycle_id);
CREATE INDEX idx_test_run_pitch ON test_runs(pitch_id);
CREATE INDEX idx_test_run_status ON test_runs(status);
CREATE INDEX idx_test_run_executed_by ON test_runs(executed_by_id);
CREATE INDEX idx_test_run_executed_at ON test_runs(executed_at);

-- Bug reports table
CREATE TABLE bug_reports (
    id BIGSERIAL PRIMARY KEY,
    bug_key VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    steps_to_reproduce TEXT,
    expected_behavior TEXT,
    actual_behavior TEXT,
    environment TEXT,
    pitch_id BIGINT REFERENCES pitches(id),
    cycle_id BIGINT REFERENCES cycles(id),
    team_id BIGINT REFERENCES teams(id),
    test_run_id BIGINT REFERENCES test_runs(id),
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    tags VARCHAR(500),
    attachments TEXT,
    reporter_id BIGINT NOT NULL REFERENCES users(id),
    assignee_id BIGINT REFERENCES users(id),
    resolution TEXT,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for bug reports
CREATE INDEX idx_bug_report_pitch ON bug_reports(pitch_id);
CREATE INDEX idx_bug_report_cycle ON bug_reports(cycle_id);
CREATE INDEX idx_bug_report_status ON bug_reports(status);
CREATE INDEX idx_bug_report_severity ON bug_reports(severity);
CREATE INDEX idx_bug_report_assignee ON bug_reports(assignee_id);
CREATE INDEX idx_bug_report_reporter ON bug_reports(reporter_id);
CREATE INDEX idx_bug_report_test_run ON bug_reports(test_run_id);
