-- Cycles table
CREATE TABLE cycles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    phase VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Teams table
CREATE TABLE teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    cycle_id BIGINT,
    CONSTRAINT fk_teams_cycle FOREIGN KEY (cycle_id) REFERENCES cycles(id)
);

-- Persons table (independent from teams)
CREATE TABLE persons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    skills TEXT,
    avatar_url VARCHAR(500),
    department VARCHAR(255),
    bio TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Users table (authentication)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    person_id BIGINT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_users_person FOREIGN KEY (person_id) REFERENCES persons(id)
);

-- Team Assignments table (links persons to teams with roles)
CREATE TABLE team_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    CONSTRAINT fk_team_assignments_person FOREIGN KEY (person_id) REFERENCES persons(id),
    CONSTRAINT fk_team_assignments_team FOREIGN KEY (team_id) REFERENCES teams(id)
);

-- Pitches table
CREATE TABLE pitches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    appetite_days INT NOT NULL,
    cycle_id BIGINT NOT NULL,
    team_id BIGINT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_pitches_cycle FOREIGN KEY (cycle_id) REFERENCES cycles(id),
    CONSTRAINT fk_pitches_team FOREIGN KEY (team_id) REFERENCES teams(id)
);

-- Work Logs table
CREATE TABLE work_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_id BIGINT NOT NULL,
    pitch_id BIGINT NOT NULL,
    date DATE NOT NULL,
    hours_spent DECIMAL(4, 2) NOT NULL,
    note TEXT,
    CONSTRAINT fk_work_logs_person FOREIGN KEY (person_id) REFERENCES persons(id),
    CONSTRAINT fk_work_logs_pitch FOREIGN KEY (pitch_id) REFERENCES pitches(id)
);

-- Meetings table
CREATE TABLE meetings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pitch_id BIGINT,
    type VARCHAR(50) NOT NULL,
    date_held DATE NOT NULL,
    dor_ready BOOLEAN NOT NULL DEFAULT FALSE,
    dod_ready BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    CONSTRAINT fk_meetings_pitch FOREIGN KEY (pitch_id) REFERENCES pitches(id)
);

-- Evidences table
CREATE TABLE evidences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pitch_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    date DATE NOT NULL,
    description TEXT NOT NULL,
    file_url VARCHAR(500),
    CONSTRAINT fk_evidences_pitch FOREIGN KEY (pitch_id) REFERENCES pitches(id),
    CONSTRAINT fk_evidences_person FOREIGN KEY (person_id) REFERENCES persons(id)
);
