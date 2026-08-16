-- Add task cycle history tracking for reporting: "which cycle was this task in, in which
-- cycle did it complete". Task.cycle stays @NotAudited (not Envers-tracked), so this table
-- accumulates one snapshot row per meaningful cycle/status event via direct synchronous
-- service calls (mirrors pitch_risk_history / V59).

CREATE TABLE task_cycle_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id BIGINT NOT NULL,
    cycle_id BIGINT,
    status VARCHAR(20) NOT NULL,
    source VARCHAR(30) NOT NULL,
    recorded_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_task_cycle_history_task
        FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_cycle_history_cycle
        FOREIGN KEY (cycle_id) REFERENCES cycles(id) ON DELETE SET NULL
);

CREATE INDEX idx_task_cycle_history_task ON task_cycle_history (task_id);
CREATE INDEX idx_task_cycle_history_cycle ON task_cycle_history (cycle_id);
CREATE INDEX idx_task_cycle_history_task_date ON task_cycle_history (task_id, recorded_at);

COMMENT ON TABLE task_cycle_history IS 'Historical snapshots of the cycle (and status) a task belonged to, for reporting';
COMMENT ON COLUMN task_cycle_history.cycle_id IS 'NULL means the task had no cycle at this point in time';
COMMENT ON COLUMN task_cycle_history.source IS 'What triggered this snapshot: TASK_CREATED, STATUS_CHANGE, PITCH_CYCLE_CHANGE, MANUAL_CYCLE_CHANGE';
