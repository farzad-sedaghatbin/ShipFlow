-- Add Circuit Breaker support to pitches (Shape Up safety valve)
ALTER TABLE pitches ADD COLUMN is_circuit_breaker_triggered BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pitches ADD COLUMN circuit_breaker_reason TEXT;
ALTER TABLE pitches ADD COLUMN circuit_breaker_date TIMESTAMP;

-- Add index for finding pitches with circuit breakers
CREATE INDEX idx_pitches_circuit_breaker ON pitches(is_circuit_breaker_triggered);
