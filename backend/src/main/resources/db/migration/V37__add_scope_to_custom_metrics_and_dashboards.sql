-- Add scope columns to custom_metrics table
-- Allows metrics to be scoped to a specific cycle, pitch, or team

ALTER TABLE custom_metrics ADD COLUMN cycle_id BIGINT;
ALTER TABLE custom_metrics ADD COLUMN pitch_id BIGINT;
ALTER TABLE custom_metrics ADD COLUMN team_id BIGINT;

-- Add foreign key constraints
ALTER TABLE custom_metrics
    ADD CONSTRAINT fk_custom_metrics_cycle
        FOREIGN KEY (cycle_id) REFERENCES cycles(id)
        ON DELETE CASCADE;
        
ALTER TABLE custom_metrics
    ADD CONSTRAINT fk_custom_metrics_pitch
        FOREIGN KEY (pitch_id) REFERENCES pitches(id)
        ON DELETE CASCADE;
        
ALTER TABLE custom_metrics
    ADD CONSTRAINT fk_custom_metrics_team
        FOREIGN KEY (team_id) REFERENCES teams(id)
        ON DELETE CASCADE;

-- Add indexes for better query performance
CREATE INDEX idx_custom_metrics_cycle_id ON custom_metrics(cycle_id);
CREATE INDEX idx_custom_metrics_pitch_id ON custom_metrics(pitch_id);
CREATE INDEX idx_custom_metrics_team_id ON custom_metrics(team_id);

-- Add scope columns to custom_dashboards table
-- Allows dashboards to be scoped to a specific cycle, pitch, or team

ALTER TABLE custom_dashboards ADD COLUMN cycle_id BIGINT;
ALTER TABLE custom_dashboards ADD COLUMN pitch_id BIGINT;
ALTER TABLE custom_dashboards ADD COLUMN team_id BIGINT;

-- Add foreign key constraints
ALTER TABLE custom_dashboards
    ADD CONSTRAINT fk_custom_dashboards_cycle
        FOREIGN KEY (cycle_id) REFERENCES cycles(id)
        ON DELETE CASCADE;
        
ALTER TABLE custom_dashboards
    ADD CONSTRAINT fk_custom_dashboards_pitch
        FOREIGN KEY (pitch_id) REFERENCES pitches(id)
        ON DELETE CASCADE;
        
ALTER TABLE custom_dashboards
    ADD CONSTRAINT fk_custom_dashboards_team
        FOREIGN KEY (team_id) REFERENCES teams(id)
        ON DELETE CASCADE;

-- Add indexes for better query performance
CREATE INDEX idx_custom_dashboards_cycle_id ON custom_dashboards(cycle_id);
CREATE INDEX idx_custom_dashboards_pitch_id ON custom_dashboards(pitch_id);
CREATE INDEX idx_custom_dashboards_team_id ON custom_dashboards(team_id);
