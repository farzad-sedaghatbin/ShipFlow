-- Migration: Add new features support
-- 1. Risk Feedback table for AI feedback loop
-- 2. Hill Chart History table for tracking position changes
-- 3. User Preferences table for theme/dark mode settings

-- Risk Feedback Table
CREATE TABLE risk_feedback (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pitch_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    original_risk_score INT NOT NULL,
    rating VARCHAR(50) NOT NULL,
    suggested_risk_score INT,
    notes TEXT,
    missed_factors TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pitch_id) REFERENCES pitches(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_risk_feedback_pitch ON risk_feedback(pitch_id);
CREATE INDEX idx_risk_feedback_user ON risk_feedback(user_id);
CREATE INDEX idx_risk_feedback_rating ON risk_feedback(rating);
CREATE INDEX idx_risk_feedback_created ON risk_feedback(created_at);

-- Hill Chart History Table
CREATE TABLE hill_chart_history (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hill_chart_point_id BIGINT NOT NULL,
    pitch_id BIGINT NOT NULL,
    user_id BIGINT,
    previous_position INT NOT NULL,
    new_position INT NOT NULL,
    confidence_level INT,
    note TEXT,
    actual_hours_at_move DOUBLE PRECISION,
    progress_at_move DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (hill_chart_point_id) REFERENCES hill_chart_points(id) ON DELETE CASCADE,
    FOREIGN KEY (pitch_id) REFERENCES pitches(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_hill_history_point ON hill_chart_history(hill_chart_point_id);
CREATE INDEX idx_hill_history_pitch ON hill_chart_history(pitch_id);
CREATE INDEX idx_hill_history_user ON hill_chart_history(user_id);
CREATE INDEX idx_hill_history_created ON hill_chart_history(created_at);

-- User Preferences Table
CREATE TABLE user_preferences (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    theme_mode VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    primary_color VARCHAR(20),
    secondary_color VARCHAR(20),
    compact_view BOOLEAN DEFAULT FALSE,
    enable_animations BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_prefs_user ON user_preferences(user_id);

-- Insert default user preferences for existing users
INSERT INTO user_preferences (user_id, theme_mode, compact_view, enable_animations)
SELECT id, 'SYSTEM', FALSE, TRUE FROM users WHERE id NOT IN (SELECT user_id FROM user_preferences);
