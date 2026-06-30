ALTER TABLE organization_settings
    ADD COLUMN IF NOT EXISTS default_sprint_length_weeks INTEGER NOT NULL DEFAULT 2;
