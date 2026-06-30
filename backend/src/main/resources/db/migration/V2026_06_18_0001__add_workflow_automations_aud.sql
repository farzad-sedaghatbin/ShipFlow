-- Hibernate Envers audit table for WorkflowAutomation entity
-- Required because WorkflowAutomation is annotated with @Audited

CREATE TABLE IF NOT EXISTS workflow_automations_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    name VARCHAR(255),
    description TEXT,
    project_id BIGINT,
    trigger_type VARCHAR(100),
    trigger_config TEXT,
    action_type VARCHAR(100),
    action_config TEXT,
    enabled BOOLEAN,
    template_id BIGINT,
    execution_count BIGINT,
    last_triggered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by_id BIGINT,
    PRIMARY KEY (id, rev)
);

CREATE INDEX IF NOT EXISTS idx_workflow_automations_aud_rev ON workflow_automations_aud(rev);
CREATE INDEX IF NOT EXISTS idx_workflow_automations_aud_id ON workflow_automations_aud(id);
