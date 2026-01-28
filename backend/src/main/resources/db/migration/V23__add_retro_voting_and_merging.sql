-- Add voting and merging support to retro items

-- Add vote_count and merged_into_id columns to retro_items
ALTER TABLE retro_items ADD COLUMN vote_count INT NOT NULL DEFAULT 0;
ALTER TABLE retro_items ADD COLUMN merged_into_id BIGINT;
ALTER TABLE retro_items ADD CONSTRAINT fk_retro_items_merged_into FOREIGN KEY (merged_into_id) REFERENCES retro_items(id) ON DELETE SET NULL;

-- Create retro_item_votes table to track who voted
CREATE TABLE retro_item_votes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    retro_item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_retro_item_votes_item FOREIGN KEY (retro_item_id) REFERENCES retro_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_retro_item_votes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_retro_item_votes_item_user UNIQUE (retro_item_id, user_id)
);

-- Index for faster vote lookups
CREATE INDEX idx_retro_item_votes_item_id ON retro_item_votes(retro_item_id);
CREATE INDEX idx_retro_item_votes_user_id ON retro_item_votes(user_id);
CREATE INDEX idx_retro_items_merged_into ON retro_items(merged_into_id);
