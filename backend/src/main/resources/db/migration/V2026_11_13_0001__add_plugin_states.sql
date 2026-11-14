-- Persisted enabled/disabled choice per plugin. Before this, the flag lived only in memory and
-- every restart re-enabled every plugin. One row per plugin id; absent row = never seen yet.
CREATE TABLE IF NOT EXISTS plugin_states (
    plugin_id  VARCHAR(100) NOT NULL PRIMARY KEY,
    enabled    BOOLEAN      NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
