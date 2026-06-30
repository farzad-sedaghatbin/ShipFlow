-- pitches_aud was created (V66) before project_id was added to pitches (V2026_06_19_0001).
-- Hibernate Envers schema-validation requires the audited column to exist in the _aud table.
ALTER TABLE pitches_aud ADD COLUMN IF NOT EXISTS project_id BIGINT;
