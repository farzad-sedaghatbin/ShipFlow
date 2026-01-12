-- GitHub Integration Tables

-- GitHub repositories table
CREATE TABLE github_repositories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    full_name VARCHAR(511) NOT NULL,
    url VARCHAR(1000),
    default_branch VARCHAR(255) DEFAULT 'main',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_github_repo UNIQUE (owner, name)
);

-- GitHub commits table
CREATE TABLE github_commits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    sha VARCHAR(40) NOT NULL UNIQUE,
    message CLOB,
    author_name VARCHAR(255),
    author_email VARCHAR(255),
    author_username VARCHAR(255),
    commit_date TIMESTAMP NOT NULL,
    branch VARCHAR(255),
    url VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_github_commits_repository FOREIGN KEY (repository_id) REFERENCES github_repositories(id) ON DELETE CASCADE
);

-- GitHub pull requests table
CREATE TABLE github_pull_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    pr_number INT NOT NULL,
    title VARCHAR(500) NOT NULL,
    description CLOB,
    state VARCHAR(50) NOT NULL,
    head_branch VARCHAR(255) NOT NULL,
    base_branch VARCHAR(255) NOT NULL,
    author_username VARCHAR(255),
    url VARCHAR(1000),
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    merged_at TIMESTAMP,
    merged_by_username VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_github_prs_repository FOREIGN KEY (repository_id) REFERENCES github_repositories(id) ON DELETE CASCADE,
    CONSTRAINT uk_github_pr UNIQUE (repository_id, pr_number)
);

-- GitHub branches table
CREATE TABLE github_branches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    head_sha VARCHAR(40),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_github_branches_repository FOREIGN KEY (repository_id) REFERENCES github_repositories(id) ON DELETE CASCADE,
    CONSTRAINT uk_github_branch UNIQUE (repository_id, name)
);

-- Pitch-GitHub links table (many-to-many relationship)
CREATE TABLE pitch_github_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pitch_id BIGINT NOT NULL,
    link_type VARCHAR(50) NOT NULL, -- COMMIT, PULL_REQUEST, BRANCH
    commit_id BIGINT,
    pull_request_id BIGINT,
    branch_id BIGINT,
    linked_at TIMESTAMP NOT NULL,
    linked_by_user_id BIGINT,
    CONSTRAINT fk_pitch_github_pitch FOREIGN KEY (pitch_id) REFERENCES pitches(id) ON DELETE CASCADE,
    CONSTRAINT fk_pitch_github_commit FOREIGN KEY (commit_id) REFERENCES github_commits(id) ON DELETE CASCADE,
    CONSTRAINT fk_pitch_github_pr FOREIGN KEY (pull_request_id) REFERENCES github_pull_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_pitch_github_branch FOREIGN KEY (branch_id) REFERENCES github_branches(id) ON DELETE CASCADE,
    CONSTRAINT fk_pitch_github_user FOREIGN KEY (linked_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CHECK (
        (link_type = 'COMMIT' AND commit_id IS NOT NULL AND pull_request_id IS NULL AND branch_id IS NULL) OR
        (link_type = 'PULL_REQUEST' AND commit_id IS NULL AND pull_request_id IS NOT NULL AND branch_id IS NULL) OR
        (link_type = 'BRANCH' AND commit_id IS NULL AND pull_request_id IS NULL AND branch_id IS NOT NULL)
    )
);

-- Task-GitHub links table (many-to-many relationship)
CREATE TABLE task_github_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    link_type VARCHAR(50) NOT NULL, -- COMMIT, PULL_REQUEST, BRANCH
    commit_id BIGINT,
    pull_request_id BIGINT,
    branch_id BIGINT,
    linked_at TIMESTAMP NOT NULL,
    linked_by_user_id BIGINT,
    auto_linked BOOLEAN NOT NULL DEFAULT FALSE, -- TRUE if linked via keyword in commit/PR, FALSE if manually linked
    CONSTRAINT fk_task_github_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_github_commit FOREIGN KEY (commit_id) REFERENCES github_commits(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_github_pr FOREIGN KEY (pull_request_id) REFERENCES github_pull_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_github_branch FOREIGN KEY (branch_id) REFERENCES github_branches(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_github_user FOREIGN KEY (linked_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CHECK (
        (link_type = 'COMMIT' AND commit_id IS NOT NULL AND pull_request_id IS NULL AND branch_id IS NULL) OR
        (link_type = 'PULL_REQUEST' AND commit_id IS NULL AND pull_request_id IS NOT NULL AND branch_id IS NULL) OR
        (link_type = 'BRANCH' AND commit_id IS NULL AND pull_request_id IS NULL AND branch_id IS NOT NULL)
    )
);

-- GitHub webhook events log (for audit and debugging)
CREATE TABLE github_webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    payload CLOB NOT NULL,
    repository_full_name VARCHAR(511),
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP,
    error_message CLOB,
    created_at TIMESTAMP NOT NULL
);

-- GitHub configuration table (stores webhook secrets, tokens, etc.)
CREATE TABLE github_configuration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repository_id BIGINT NOT NULL,
    webhook_secret VARCHAR(500),
    access_token VARCHAR(500),
    auto_link_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    auto_close_tasks_on_merge BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_github_config_repository FOREIGN KEY (repository_id) REFERENCES github_repositories(id) ON DELETE CASCADE,
    CONSTRAINT uk_github_config_repo UNIQUE (repository_id)
);

-- Create indices for performance
CREATE INDEX idx_commits_sha ON github_commits(sha);
CREATE INDEX idx_commits_repo ON github_commits(repository_id);
CREATE INDEX idx_commits_branch ON github_commits(branch);
CREATE INDEX idx_prs_repo ON github_pull_requests(repository_id);
CREATE INDEX idx_prs_state ON github_pull_requests(state);
CREATE INDEX idx_branches_repo ON github_branches(repository_id);
CREATE INDEX idx_pitch_links_pitch ON pitch_github_links(pitch_id);
CREATE INDEX idx_pitch_links_type ON pitch_github_links(link_type);
CREATE INDEX idx_task_links_task ON task_github_links(task_id);
CREATE INDEX idx_task_links_type ON task_github_links(link_type);
CREATE INDEX idx_task_links_auto ON task_github_links(auto_linked);
