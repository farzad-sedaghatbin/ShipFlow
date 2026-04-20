-- Wise Designer: stores AI-generated HTML prototype artifacts per pitch.
-- Each artifact is a self-contained HTML file (with inline Tailwind CDN and JS)
-- that can be rendered in a sandboxed iframe as a clickable UI mockup.
-- Multiple iterations per pitch are supported (each generate/iterate creates a new row).

CREATE TABLE wise_designer_artifacts (
    id                    BIGSERIAL PRIMARY KEY,
    pitch_id              BIGINT       NOT NULL REFERENCES pitches(id),
    html_content          TEXT         NOT NULL,
    description           VARCHAR(500),
    session_id            VARCHAR(100),
    wise_arch_session_id  VARCHAR(100),           -- optional: arch session used as context
    created_by_user_id    BIGINT       REFERENCES users(id),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wise_designer_artifacts_pitch_id ON wise_designer_artifacts (pitch_id);
CREATE INDEX idx_wise_designer_artifacts_session_id ON wise_designer_artifacts (session_id);

COMMENT ON TABLE wise_designer_artifacts IS
    'AI-generated HTML UI prototype artifacts produced by the Wise Designer feature. '
    'Each row is one version of the mockup for a pitch. The html_content is a single '
    'self-contained file rendered inside a sandboxed <iframe>.';

-- Feature flag on org settings
ALTER TABLE organization_settings
    ADD COLUMN enable_wise_designer BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN organization_settings.enable_wise_designer IS
    'Enable Wise Designer experimental feature — AI-powered UI prototype generator for pitches.';
