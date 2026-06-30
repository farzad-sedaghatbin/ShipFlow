-- Add dislike_count to retro_items
ALTER TABLE retro_items ADD COLUMN IF NOT EXISTS dislike_count INTEGER NOT NULL DEFAULT 0;

-- Create dislike votes table (mirrors retro_item_votes)
CREATE TABLE IF NOT EXISTS retro_item_dislike_votes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    retro_item_id BIGINT NOT NULL REFERENCES retro_items(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_retro_item_dislike_votes UNIQUE (retro_item_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_retro_item_dislike_votes_item ON retro_item_dislike_votes(retro_item_id);
CREATE INDEX IF NOT EXISTS idx_retro_item_dislike_votes_user ON retro_item_dislike_votes(user_id);
