-- V82__add_roadmap_and_releases.sql
-- Add roadmap features: Initiatives, Epics, and Releases

-- =====================================================
-- Create initiatives table
-- Initiatives are strategic themes spanning multiple quarters
-- =====================================================
CREATE TABLE IF NOT EXISTS initiatives (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    color VARCHAR(7),
    target_start_date DATE,
    target_end_date DATE,
    project_id BIGINT NOT NULL,
    owner_id BIGINT,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by_id BIGINT,
    CONSTRAINT fk_initiative_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_initiative_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_initiative_deleted_by FOREIGN KEY (deleted_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_initiative_project ON initiatives(project_id);
CREATE INDEX idx_initiative_status ON initiatives(status);
CREATE INDEX idx_initiative_owner ON initiatives(owner_id);
CREATE INDEX idx_initiative_target_dates ON initiatives(target_start_date, target_end_date);
CREATE INDEX idx_initiative_deleted_at ON initiatives(deleted_at);

COMMENT ON TABLE initiatives IS 'Strategic initiatives for roadmap planning (e.g., "Mobile Experience 2026")';
COMMENT ON COLUMN initiatives.color IS 'Hex color for timeline visualization';
COMMENT ON COLUMN initiatives.sort_order IS 'Display order in roadmap views';

-- =====================================================
-- Create epics table
-- Epics are large features grouping related pitches
-- =====================================================
CREATE TABLE IF NOT EXISTS epics (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    color VARCHAR(7),
    target_start_date DATE,
    target_end_date DATE,
    project_id BIGINT NOT NULL,
    initiative_id BIGINT,
    owner_id BIGINT,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by_id BIGINT,
    CONSTRAINT fk_epic_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_epic_initiative FOREIGN KEY (initiative_id) REFERENCES initiatives(id) ON DELETE SET NULL,
    CONSTRAINT fk_epic_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_epic_deleted_by FOREIGN KEY (deleted_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_epic_project ON epics(project_id);
CREATE INDEX idx_epic_initiative ON epics(initiative_id);
CREATE INDEX idx_epic_status ON epics(status);
CREATE INDEX idx_epic_owner ON epics(owner_id);
CREATE INDEX idx_epic_target_dates ON epics(target_start_date, target_end_date);
CREATE INDEX idx_epic_deleted_at ON epics(deleted_at);

COMMENT ON TABLE epics IS 'Large features grouping related pitches (e.g., "Mobile Checkout Redesign")';
COMMENT ON COLUMN epics.initiative_id IS 'Optional parent initiative - epics can exist without one';

-- =====================================================
-- Create releases table
-- Releases are delivery milestones containing items from multiple epics
-- =====================================================
CREATE TABLE IF NOT EXISTS releases (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    version VARCHAR(50),
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNING',
    risk_level VARCHAR(50) DEFAULT 'LOW',
    target_date DATE,
    release_date DATE,
    release_notes TEXT,
    project_id BIGINT NOT NULL,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by_id BIGINT,
    CONSTRAINT fk_release_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_release_deleted_by FOREIGN KEY (deleted_by_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_release_project ON releases(project_id);
CREATE INDEX idx_release_status ON releases(status);
CREATE INDEX idx_release_target_date ON releases(target_date);
CREATE INDEX idx_release_version ON releases(version);
CREATE INDEX idx_release_deleted_at ON releases(deleted_at);

COMMENT ON TABLE releases IS 'Delivery milestones that can span multiple cycles';
COMMENT ON COLUMN releases.version IS 'Version string (e.g., "v2.4.0", "2026.Q2")';
COMMENT ON COLUMN releases.risk_level IS 'Current risk level: LOW, MEDIUM, HIGH, CRITICAL';

-- =====================================================
-- Create release_cycles join table
-- Links releases to cycles (many-to-many)
-- =====================================================
CREATE TABLE IF NOT EXISTS release_cycles (
    release_id BIGINT NOT NULL,
    cycle_id BIGINT NOT NULL,
    PRIMARY KEY (release_id, cycle_id),
    CONSTRAINT fk_release_cycles_release FOREIGN KEY (release_id) REFERENCES releases(id) ON DELETE CASCADE,
    CONSTRAINT fk_release_cycles_cycle FOREIGN KEY (cycle_id) REFERENCES cycles(id) ON DELETE CASCADE
);

CREATE INDEX idx_release_cycles_release ON release_cycles(release_id);
CREATE INDEX idx_release_cycles_cycle ON release_cycles(cycle_id);

COMMENT ON TABLE release_cycles IS 'Many-to-many relationship between releases and cycles';

-- =====================================================
-- Add epic_id and target_release_id to pitches
-- =====================================================
ALTER TABLE pitches
ADD COLUMN epic_id BIGINT;

ALTER TABLE pitches
ADD COLUMN target_release_id BIGINT;

ALTER TABLE pitches
ADD CONSTRAINT fk_pitch_epic
    FOREIGN KEY (epic_id) REFERENCES epics(id) ON DELETE SET NULL;

ALTER TABLE pitches
ADD CONSTRAINT fk_pitch_target_release
    FOREIGN KEY (target_release_id) REFERENCES releases(id) ON DELETE SET NULL;

CREATE INDEX idx_pitch_epic ON pitches(epic_id);
CREATE INDEX idx_pitch_target_release ON pitches(target_release_id);

COMMENT ON COLUMN pitches.epic_id IS 'Optional parent epic for roadmap organization';
COMMENT ON COLUMN pitches.target_release_id IS 'Target release for this pitch';

-- =====================================================
-- Add target_release_id to tasks
-- =====================================================
ALTER TABLE tasks
ADD COLUMN target_release_id BIGINT;

ALTER TABLE tasks
ADD CONSTRAINT fk_task_target_release
    FOREIGN KEY (target_release_id) REFERENCES releases(id) ON DELETE SET NULL;

CREATE INDEX idx_task_target_release ON tasks(target_release_id);

COMMENT ON COLUMN tasks.target_release_id IS 'Target release for this task';

-- =====================================================
-- Add target_release_id and fixed_in_release_id to bug_reports
-- =====================================================
ALTER TABLE bug_reports
ADD COLUMN target_release_id BIGINT;

ALTER TABLE bug_reports
ADD COLUMN fixed_in_release_id BIGINT;

ALTER TABLE bug_reports
ADD CONSTRAINT fk_bug_target_release
    FOREIGN KEY (target_release_id) REFERENCES releases(id) ON DELETE SET NULL;

ALTER TABLE bug_reports
ADD CONSTRAINT fk_bug_fixed_in_release
    FOREIGN KEY (fixed_in_release_id) REFERENCES releases(id) ON DELETE SET NULL;

CREATE INDEX idx_bug_target_release ON bug_reports(target_release_id);
CREATE INDEX idx_bug_fixed_in_release ON bug_reports(fixed_in_release_id);

COMMENT ON COLUMN bug_reports.target_release_id IS 'Release where bug is expected to be fixed';
COMMENT ON COLUMN bug_reports.fixed_in_release_id IS 'Release where bug was actually fixed (may differ from target)';
