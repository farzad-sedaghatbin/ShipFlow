-- Wiki feature: spaces, pages, space permissions, attachments + Envers _aud tables
-- H2-compatible DDL (no jsonb, no uuid, no SERIAL, no CREATE INDEX CONCURRENTLY)

-- ============================================================
-- wiki_spaces
-- ============================================================
CREATE TABLE wiki_spaces (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    space_key VARCHAR(64) NOT NULL,
    description TEXT,
    project_id BIGINT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_wiki_spaces_space_key UNIQUE (space_key)
);

CREATE INDEX IF NOT EXISTS idx_wiki_spaces_project ON wiki_spaces(project_id);
CREATE INDEX IF NOT EXISTS idx_wiki_spaces_deleted_at ON wiki_spaces(deleted_at);

-- ============================================================
-- wiki_pages
-- ============================================================
CREATE TABLE wiki_pages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    space_id BIGINT NOT NULL,
    parent_id BIGINT,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    content TEXT,
    content_text TEXT,
    position INT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_wiki_pages_space FOREIGN KEY (space_id) REFERENCES wiki_spaces(id),
    CONSTRAINT fk_wiki_pages_parent FOREIGN KEY (parent_id) REFERENCES wiki_pages(id)
);

CREATE INDEX IF NOT EXISTS idx_wiki_pages_space_parent ON wiki_pages(space_id, parent_id);
CREATE INDEX IF NOT EXISTS idx_wiki_pages_space ON wiki_pages(space_id);
CREATE INDEX IF NOT EXISTS idx_wiki_pages_deleted_at ON wiki_pages(deleted_at);

-- ============================================================
-- wiki_space_permissions
-- ============================================================
CREATE TABLE wiki_space_permissions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    space_id BIGINT NOT NULL,
    grantee_type VARCHAR(8) NOT NULL,
    grantee_ref VARCHAR(255) NOT NULL,
    level VARCHAR(8) NOT NULL,
    CONSTRAINT fk_wiki_space_perms_space FOREIGN KEY (space_id) REFERENCES wiki_spaces(id)
);

CREATE INDEX IF NOT EXISTS idx_wiki_space_perms_space ON wiki_space_permissions(space_id);

-- ============================================================
-- wiki_attachments
-- ============================================================
CREATE TABLE wiki_attachments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    page_id BIGINT NOT NULL,
    storage_provider VARCHAR(16) NOT NULL DEFAULT 'LOCAL_FS',
    storage_key TEXT,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128),
    file_size BIGINT,
    uploaded_by BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_wiki_attachments_page FOREIGN KEY (page_id) REFERENCES wiki_pages(id)
);

CREATE INDEX IF NOT EXISTS idx_wiki_attachments_page ON wiki_attachments(page_id);
CREATE INDEX IF NOT EXISTS idx_wiki_attachments_deleted_at ON wiki_attachments(deleted_at);

-- ============================================================
-- Hibernate Envers _aud tables for @Audited entities
-- (revinfo table already exists from prior audited entities)
-- ============================================================

CREATE TABLE IF NOT EXISTS wiki_spaces_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    name VARCHAR(255),
    space_key VARCHAR(64),
    description TEXT,
    project_id BIGINT,
    created_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id, rev)
);

CREATE INDEX IF NOT EXISTS idx_wiki_spaces_aud_rev ON wiki_spaces_aud(rev);
CREATE INDEX IF NOT EXISTS idx_wiki_spaces_aud_id ON wiki_spaces_aud(id);

CREATE TABLE IF NOT EXISTS wiki_pages_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    space_id BIGINT,
    parent_id BIGINT,
    title VARCHAR(255),
    slug VARCHAR(255),
    content TEXT,
    content_text TEXT,
    position INT,
    created_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id, rev)
);

CREATE INDEX IF NOT EXISTS idx_wiki_pages_aud_rev ON wiki_pages_aud(rev);
CREATE INDEX IF NOT EXISTS idx_wiki_pages_aud_id ON wiki_pages_aud(id);
