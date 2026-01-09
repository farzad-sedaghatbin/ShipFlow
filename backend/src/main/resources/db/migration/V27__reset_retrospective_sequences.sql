-- Reset retrospectives and retro_items sequences after V25 seed data
-- This ensures auto-generated IDs don't conflict with manually inserted seed data from V25

-- Reset retrospectives sequence to max(id) + 1
ALTER TABLE retrospectives ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM retrospectives);

-- Reset retro_items sequence to max(id) + 1
ALTER TABLE retro_items ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM retro_items);

-- Reset retro_item_votes doesn't have auto-increment (composite key)
