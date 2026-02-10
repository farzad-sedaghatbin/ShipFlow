-- Seed Data: Merged/Grouped Retrospective Items
-- This demonstrates the grouping/merging feature in retrospectives
-- Merged items have their merged_into_id set to point to the target item

-- Add merged items for "Great team collaboration" (item 1)
-- These similar items were merged together during the retro facilitation
INSERT INTO retro_items (id, content, column_type, retrospective_id, author_id, vote_count, merged_into_id, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(18, 'Pair programming sessions were very productive', 'WENT_WELL', 1, 4, 2, 1, '2025-11-13 10:16:00', '2025-11-14 15:00:00'),
(19, 'Senior devs mentored juniors effectively', 'WENT_WELL', 1, 6, 2, 1, '2025-11-13 10:17:00', '2025-11-14 15:00:00')
ON CONFLICT (id) DO NOTHING;

-- Add merged items for "Too many meetings interrupted deep work" (item 9)
-- Multiple team members raised similar concerns about meetings
INSERT INTO retro_items (id, content, column_type, retrospective_id, author_id, vote_count, merged_into_id, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(20, 'Context switching between meetings killed productivity', 'DID_NOT_GO_WELL', 1, 2, 2, 9, '2025-11-13 10:52:00', '2025-11-14 15:00:00'),
(21, 'Back-to-back meetings left no time for coding', 'DID_NOT_GO_WELL', 1, 3, 3, 9, '2025-11-13 10:55:00', '2025-11-14 15:00:00')
ON CONFLICT (id) DO NOTHING;

-- Add merged items for "Schedule focus blocks" (item 11)
-- Related suggestions about improving focus time
INSERT INTO retro_items (id, content, column_type, retrospective_id, author_id, vote_count, merged_into_id, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(22, 'Block calendar for uninterrupted work time', 'TRY_NEXT', 1, 2, 2, 11, '2025-11-13 11:06:00', '2025-11-14 15:00:00'),
(23, 'Implement async standups instead of daily meetings', 'TRY_NEXT', 1, 5, 3, 11, '2025-11-13 11:08:00', '2025-11-14 15:00:00')
ON CONFLICT (id) DO NOTHING;

-- Add merged items for "Add performance testing earlier" (item 12)
INSERT INTO retro_items (id, content, column_type, retrospective_id, author_id, vote_count, merged_into_id, created_at, updated_at) OVERRIDING SYSTEM VALUE VALUES
(24, 'Run load tests in CI from week 3', 'TRY_NEXT', 1, 3, 1, 12, '2025-11-13 11:12:00', '2025-11-14 15:00:00')
ON CONFLICT (id) DO NOTHING;

-- Votes for merged items (before they were merged)
-- Item 18: "Pair programming sessions"
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(18, 3, '2025-11-14 14:03:00'),
(18, 5, '2025-11-14 14:06:00')
ON CONFLICT (retro_item_id, user_id) DO NOTHING;

-- Item 19: "Senior devs mentored juniors"
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(19, 2, '2025-11-14 14:04:00'),
(19, 4, '2025-11-14 14:07:00')
ON CONFLICT (retro_item_id, user_id) DO NOTHING;

-- Item 20: "Context switching"
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(20, 4, '2025-11-14 14:11:00'),
(20, 5, '2025-11-14 14:13:00')
ON CONFLICT (retro_item_id, user_id) DO NOTHING;

-- Item 21: "Back-to-back meetings"
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(21, 2, '2025-11-14 14:12:00'),
(21, 4, '2025-11-14 14:14:00'),
(21, 6, '2025-11-14 14:16:00')
ON CONFLICT (retro_item_id, user_id) DO NOTHING;

-- Item 22: "Block calendar"
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(22, 3, '2025-11-14 14:10:00'),
(22, 6, '2025-11-14 14:15:00')
ON CONFLICT (retro_item_id, user_id) DO NOTHING;

-- Item 23: "Async standups"
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(23, 2, '2025-11-14 14:11:00'),
(23, 3, '2025-11-14 14:13:00'),
(23, 6, '2025-11-14 14:17:00')
ON CONFLICT (retro_item_id, user_id) DO NOTHING;

-- Item 24: "Load tests in CI"
INSERT INTO retro_item_votes (retro_item_id, user_id, created_at) VALUES
(24, 5, '2025-11-14 14:14:00')
ON CONFLICT (retro_item_id, user_id) DO NOTHING;

-- Reset retro_items sequence to account for new items
SELECT setval(pg_get_serial_sequence('retro_items', 'id'), (SELECT COALESCE(MAX(id), 0) + 1 FROM retro_items), false);
