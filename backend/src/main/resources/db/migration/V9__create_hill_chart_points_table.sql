-- Create hill_chart_points table
CREATE TABLE hill_chart_points (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pitch_id BIGINT NOT NULL,
    scope VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    position INT NOT NULL CHECK (position >= 0 AND position <= 100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pitch_id) REFERENCES pitches(id) ON DELETE CASCADE
);

-- Create index for faster pitch lookups
CREATE INDEX idx_hill_chart_pitch_id ON hill_chart_points(pitch_id);
CREATE INDEX idx_hill_chart_updated_at ON hill_chart_points(updated_at);
