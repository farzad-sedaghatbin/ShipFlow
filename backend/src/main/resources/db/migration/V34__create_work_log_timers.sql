-- Create work_log_timers table for tracking active time entries
CREATE TABLE work_log_timers (
    id BIGSERIAL PRIMARY KEY,
    person_id BIGINT NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    pitch_id BIGINT REFERENCES pitches(id) ON DELETE CASCADE,
    task_id BIGINT REFERENCES tasks(id) ON DELETE CASCADE,
    start_time TIMESTAMP NOT NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Ensure exactly one of pitch_id or task_id is set
    CONSTRAINT chk_timer_pitch_or_task CHECK (
        (pitch_id IS NOT NULL AND task_id IS NULL) OR 
        (pitch_id IS NULL AND task_id IS NOT NULL)
    ),
    
    -- Only one active timer per person
    CONSTRAINT uq_active_timer_per_person UNIQUE (person_id)
);

-- Add indexes for common queries
CREATE INDEX idx_work_log_timers_person_id ON work_log_timers(person_id);
CREATE INDEX idx_work_log_timers_pitch_id ON work_log_timers(pitch_id);
CREATE INDEX idx_work_log_timers_task_id ON work_log_timers(task_id);

-- Add comments
COMMENT ON TABLE work_log_timers IS 'Tracks actively running timers for work log entries';
COMMENT ON COLUMN work_log_timers.start_time IS 'When the timer was started';
COMMENT ON CONSTRAINT chk_timer_pitch_or_task ON work_log_timers IS 'Ensures timer is for either a pitch or a task, not both';
COMMENT ON CONSTRAINT uq_active_timer_per_person ON work_log_timers IS 'One person can only have one active timer at a time';
