-- V59__add_pitch_risk_history.sql
-- Add pitch risk history tracking for trend analysis

CREATE TABLE pitch_risk_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pitch_id BIGINT NOT NULL,
    risk_score INT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    risk_factors_json CLOB,
    recorded_at TIMESTAMP NOT NULL,
    trigger_type VARCHAR(20) NOT NULL,
    
    CONSTRAINT fk_pitch_risk_history_pitch 
        FOREIGN KEY (pitch_id) REFERENCES pitches(id) ON DELETE CASCADE
);

CREATE INDEX idx_pitch_risk_history_pitch ON pitch_risk_history (pitch_id);
CREATE INDEX idx_pitch_risk_history_date ON pitch_risk_history (recorded_at);
CREATE INDEX idx_pitch_risk_history_pitch_date ON pitch_risk_history (pitch_id, recorded_at);

COMMENT ON TABLE pitch_risk_history IS 'Historical snapshots of pitch risk scores for trend analysis';
COMMENT ON COLUMN pitch_risk_history.risk_factors_json IS 'Serialized JSON array of risk factors contributing to the score';
COMMENT ON COLUMN pitch_risk_history.trigger_type IS 'What triggered this snapshot: MANUAL, SCHEDULED, STATUS_CHANGE, WORK_LOG_ADDED, CIRCUIT_BREAKER';
