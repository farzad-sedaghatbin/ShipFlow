-- ShipFlow Agent (S70): changes the agent proposed in a comment reply, awaiting the requester's
-- confirmation. Nothing here is applied until a person confirms it.
CREATE TABLE IF NOT EXISTS agent_action_proposals (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comment_id        BIGINT       NOT NULL,
    entity_type       VARCHAR(20)  NOT NULL,
    entity_id         BIGINT       NOT NULL,
    requester_user_id BIGINT       NOT NULL,
    project_id        BIGINT       NOT NULL,
    tool              VARCHAR(64)  NOT NULL,
    arguments_json    TEXT         NOT NULL,
    summary           VARCHAR(500),
    status            VARCHAR(20)  NOT NULL,
    result_message    VARCHAR(500),
    created_at        TIMESTAMP    NOT NULL,
    resolved_at       TIMESTAMP,
    CONSTRAINT fk_agent_action_proposals_comments FOREIGN KEY (comment_id) REFERENCES comments (id),
    CONSTRAINT fk_agent_action_proposals_users FOREIGN KEY (requester_user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_agent_action_comment ON agent_action_proposals (comment_id);
CREATE INDEX IF NOT EXISTS idx_agent_action_requester ON agent_action_proposals (requester_user_id);
