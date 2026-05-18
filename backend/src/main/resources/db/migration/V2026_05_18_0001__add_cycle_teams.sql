-- Explicit team assignment to cycles, needed for betting slot generation
-- before any pitches have been assigned to the cycle.
CREATE TABLE cycle_teams (
    cycle_id BIGINT NOT NULL,
    team_id  BIGINT NOT NULL,
    CONSTRAINT pk_cycle_teams PRIMARY KEY (cycle_id, team_id),
    CONSTRAINT fk_cycle_teams_cycle FOREIGN KEY (cycle_id) REFERENCES cycles(id),
    CONSTRAINT fk_cycle_teams_team  FOREIGN KEY (team_id)  REFERENCES teams(id)
);

CREATE INDEX IF NOT EXISTS idx_cycle_teams_cycle_id ON cycle_teams(cycle_id);
CREATE INDEX IF NOT EXISTS idx_cycle_teams_team_id  ON cycle_teams(team_id);
