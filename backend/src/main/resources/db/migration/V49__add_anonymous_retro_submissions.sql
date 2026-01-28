-- Add support for anonymous retrospective item submissions
ALTER TABLE retro_items ADD COLUMN is_anonymous BOOLEAN NOT NULL DEFAULT FALSE;

-- Add index for filtering anonymous items
CREATE INDEX idx_retro_items_is_anonymous ON retro_items(is_anonymous);
