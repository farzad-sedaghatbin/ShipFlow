-- Backfill priority and sort_order for seeded pitches (V1001 data)
-- Columns were added without defaults in V2026_02_25_0002; this migration sets the
-- intended values for the demo/seed data so they render correctly in the UI.

-- Cycle 4, Team 1 pitches (IN_PROGRESS)
UPDATE pitches SET sort_order = 1, priority = 'HIGH'   WHERE id = 1;
UPDATE pitches SET sort_order = 2, priority = 'MEDIUM' WHERE id = 2;

-- Cycle 4, Team 2 pitches (IN_PROGRESS / DONE)
UPDATE pitches SET sort_order = 1, priority = 'HIGH'   WHERE id = 3;
UPDATE pitches SET sort_order = 2, priority = 'LOW'    WHERE id = 4;

-- Cycle 5 SHAPED pitches
UPDATE pitches SET sort_order = 1, priority = 'HIGH'   WHERE id = 5;
UPDATE pitches SET sort_order = 2, priority = 'HIGH'   WHERE id = 6;
UPDATE pitches SET sort_order = 3, priority = 'MEDIUM' WHERE id = 7;
UPDATE pitches SET sort_order = 4, priority = 'MEDIUM' WHERE id = 8;
UPDATE pitches SET sort_order = 5, priority = 'HIGH'   WHERE id = 9;
UPDATE pitches SET sort_order = 6, priority = 'LOW'    WHERE id = 10;

-- Cycle 5 IDEA pitches
UPDATE pitches SET sort_order = 1, priority = 'HIGH'   WHERE id = 11;
UPDATE pitches SET sort_order = 2, priority = 'MEDIUM' WHERE id = 12;
UPDATE pitches SET sort_order = 3, priority = 'LOW'    WHERE id = 13;

-- Historical DONE pitches (cycles 1-3)
UPDATE pitches SET sort_order = 1, priority = 'HIGH'   WHERE id = 14;
UPDATE pitches SET sort_order = 2, priority = 'HIGH'   WHERE id = 15;
UPDATE pitches SET sort_order = 1, priority = 'MEDIUM' WHERE id = 16;
UPDATE pitches SET sort_order = 2, priority = 'MEDIUM' WHERE id = 17;
UPDATE pitches SET sort_order = 1, priority = 'LOW'    WHERE id = 18;
UPDATE pitches SET sort_order = 2, priority = 'MEDIUM' WHERE id = 19;

-- CANCELLED pitch (no priority)
UPDATE pitches SET sort_order = 1, priority = NULL     WHERE id = 20;

-- Backfill priority for seeded initiatives (V1006 data)
UPDATE initiatives SET priority = 'HIGH'   WHERE id = 1;
UPDATE initiatives SET priority = 'HIGH'   WHERE id = 2;
UPDATE initiatives SET priority = 'MEDIUM' WHERE id = 3;

-- Backfill priority for seeded epics (V1006 data)
UPDATE epics SET priority = 'HIGH'   WHERE id = 1;
UPDATE epics SET priority = 'HIGH'   WHERE id = 2;
UPDATE epics SET priority = 'MEDIUM' WHERE id = 3;
UPDATE epics SET priority = 'HIGH'   WHERE id = 4;
UPDATE epics SET priority = 'MEDIUM' WHERE id = 5;
