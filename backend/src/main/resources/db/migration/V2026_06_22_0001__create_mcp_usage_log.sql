-- MCP tool-call usage log: one row per tools/call invocation
CREATE TABLE IF NOT EXISTS mcp_usage_log (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    api_key_id      BIGINT       REFERENCES api_keys(id),
    tool_name       VARCHAR(120) NOT NULL,
    success         BOOLEAN      NOT NULL DEFAULT TRUE,
    error_message   TEXT,
    duration_ms     BIGINT,
    called_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mcp_usage_log_user_id   ON mcp_usage_log(user_id);
CREATE INDEX IF NOT EXISTS idx_mcp_usage_log_tool_name  ON mcp_usage_log(tool_name);
CREATE INDEX IF NOT EXISTS idx_mcp_usage_log_called_at  ON mcp_usage_log(called_at);
CREATE INDEX IF NOT EXISTS idx_mcp_usage_log_api_key_id ON mcp_usage_log(api_key_id);
