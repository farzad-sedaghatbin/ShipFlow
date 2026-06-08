-- Widen hill_chart_points.scope (VARCHAR 100) and .description (VARCHAR 500) to TEXT.
-- Task descriptions are copied into scope when a task is linked to a pitch;
-- the hard length cap silently truncates long content.
-- TEXT removes the limit entirely.
-- Syntax compatible with both H2 (tests) and PostgreSQL (prod).
ALTER TABLE hill_chart_points ALTER COLUMN scope TYPE TEXT;
ALTER TABLE hill_chart_points ALTER COLUMN description TYPE TEXT;
