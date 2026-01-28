-- V35__add_meeting_enhancements.sql
-- Add new fields to meetings table for enhanced functionality

-- Add retrospective linking
ALTER TABLE meetings ADD COLUMN retrospective_id BIGINT;
ALTER TABLE meetings ADD CONSTRAINT fk_meetings_retrospective FOREIGN KEY (retrospective_id) REFERENCES retrospectives(id);

-- Add decisions field
ALTER TABLE meetings ADD COLUMN decisions TEXT;

-- Add attendees field
ALTER TABLE meetings ADD COLUMN attendees TEXT;

-- Create meeting_actions table
CREATE TABLE meeting_actions (
    id BIGSERIAL PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    description TEXT NOT NULL,
    assigned_to_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    due_date DATE,
    notes TEXT,
    CONSTRAINT fk_meeting_actions_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    CONSTRAINT fk_meeting_actions_person FOREIGN KEY (assigned_to_id) REFERENCES persons(id)
);

-- Create index for faster queries
CREATE INDEX idx_meeting_actions_meeting_id ON meeting_actions(meeting_id);
CREATE INDEX idx_meeting_actions_assigned_to_id ON meeting_actions(assigned_to_id);
CREATE INDEX idx_meeting_actions_status ON meeting_actions(status);
CREATE INDEX idx_meetings_retrospective_id ON meetings(retrospective_id);
CREATE INDEX idx_meetings_date_held ON meetings(date_held DESC);
