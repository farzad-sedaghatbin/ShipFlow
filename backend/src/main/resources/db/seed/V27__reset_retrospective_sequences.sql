-- Reset retrospectives and retro_items sequences after V25 seed data
-- This ensures auto-generated IDs don't conflict with manually inserted seed data from V25

-- Reset retrospectives sequence to max(id) + 1
SELECT setval(pg_get_serial_sequence('retrospectives', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM retrospectives), false);

-- Reset retro_items sequence to max(id) + 1
SELECT setval(pg_get_serial_sequence('retro_items', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM retro_items), false);

-- Reset retro_item_votes doesn't have auto-increment (composite key)
