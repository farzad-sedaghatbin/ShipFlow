-- Workflow Automations: trigger/action engine for v1.7.0

CREATE TABLE workflow_automation_templates (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    trigger_type VARCHAR(100) NOT NULL,
    default_trigger_config TEXT,
    action_type VARCHAR(100) NOT NULL,
    default_action_config TEXT,
    icon_name VARCHAR(100),
    is_built_in BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_automation_template_category ON workflow_automation_templates (category);
CREATE INDEX IF NOT EXISTS idx_automation_template_trigger ON workflow_automation_templates (trigger_type);

CREATE TABLE workflow_automations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    project_id BIGINT NOT NULL REFERENCES projects (id),
    trigger_type VARCHAR(100) NOT NULL,
    trigger_config TEXT,
    action_type VARCHAR(100) NOT NULL,
    action_config TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    template_id BIGINT REFERENCES workflow_automation_templates (id),
    execution_count BIGINT NOT NULL DEFAULT 0,
    last_triggered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by_id BIGINT REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_automation_project ON workflow_automations (project_id);
CREATE INDEX IF NOT EXISTS idx_automation_trigger_type ON workflow_automations (trigger_type);
CREATE INDEX IF NOT EXISTS idx_automation_enabled ON workflow_automations (enabled);
CREATE INDEX IF NOT EXISTS idx_automation_deleted ON workflow_automations (deleted_at);

CREATE TABLE workflow_automation_executions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    automation_id BIGINT NOT NULL REFERENCES workflow_automations (id),
    trigger_event_type VARCHAR(100) NOT NULL,
    trigger_event_data TEXT,
    status VARCHAR(50) NOT NULL,
    result_message TEXT,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_automation_exec_automation ON workflow_automation_executions (automation_id);
CREATE INDEX IF NOT EXISTS idx_automation_exec_status ON workflow_automation_executions (status);
CREATE INDEX IF NOT EXISTS idx_automation_exec_executed_at ON workflow_automation_executions (executed_at);

-- Seed 20 built-in automation templates
INSERT INTO workflow_automation_templates (name, description, category, trigger_type, default_trigger_config, action_type, default_action_config, icon_name, sort_order)
VALUES
  ('Notify on task completed', 'Send an in-app notification when any task is marked as done.', 'Tasks', 'TASK_COMPLETED', '{}', 'NOTIFY_PROJECT_MEMBERS', '{"message":"A task was completed: {{taskTitle}}"}', 'CheckCircle', 1),
  ('Notify on task created', 'Alert project members when a new task is added to the backlog.', 'Tasks', 'TASK_CREATED', '{}', 'NOTIFY_PROJECT_MEMBERS', '{"message":"New task created: {{taskTitle}}"}', 'PlusCircle', 2),
  ('Notify on task assignment', 'Alert the assignee when a task is assigned to them.', 'Tasks', 'TASK_ASSIGNED', '{}', 'NOTIFY_ASSIGNEE', '{"message":"You have been assigned to: {{taskTitle}}"}', 'UserCheck', 3),
  ('Auto-comment on task status change', 'Add a comment when a task status changes.', 'Tasks', 'TASK_STATUS_CHANGED', '{}', 'ADD_COMMENT', '{"comment":"Status changed from {{oldStatus}} to {{newStatus}}."}', 'MessageSquare', 4),
  ('Webhook on task completed', 'POST to an external URL whenever a task is completed.', 'Tasks', 'TASK_COMPLETED', '{}', 'SEND_WEBHOOK', '{"url":"https://hooks.example.com/task-done","method":"POST"}', 'Webhook', 5),
  ('Notify on pitch status change', 'Notify the team when a pitch moves to a new status.', 'Shape Up', 'PITCH_STATUS_CHANGED', '{}', 'NOTIFY_PROJECT_MEMBERS', '{"message":"Pitch {{pitchTitle}} moved to {{newStatus}}."}', 'FileText', 6),
  ('Notify on pitch created', 'Alert project members when a new pitch is submitted.', 'Shape Up', 'PITCH_CREATED', '{}', 'NOTIFY_PROJECT_MEMBERS', '{"message":"New pitch submitted: {{pitchTitle}}"}', 'Lightbulb', 7),
  ('Notify team when cycle starts', 'Send a kick-off notification when a new cycle begins.', 'Shape Up', 'CYCLE_STARTED', '{}', 'NOTIFY_PROJECT_MEMBERS', '{"message":"Cycle {{cycleName}} has started! Let''s ship it."}', 'Play', 8),
  ('Notify team when cycle ends', 'Remind the team when a cycle wraps up.', 'Shape Up', 'CYCLE_ENDED', '{}', 'NOTIFY_PROJECT_MEMBERS', '{"message":"Cycle {{cycleName}} has ended. Time for a retro!"}', 'FlagOff', 9),
  ('Webhook on cycle ended', 'POST to CI/CD or external system when a cycle closes.', 'Shape Up', 'CYCLE_ENDED', '{}', 'SEND_WEBHOOK', '{"url":"https://hooks.example.com/cycle-done","method":"POST"}', 'Webhook', 10),
  ('Alert on appetite exceeded', 'Warn the team when a pitch runs past its appetite.', 'Shape Up', 'APPETITE_EXCEEDED', '{}', 'NOTIFY_PROJECT_MEMBERS', '{"message":"Warning: pitch {{pitchTitle}} has exceeded its appetite of {{appetiteDays}} days."}', 'AlertTriangle', 11),
  ('Webhook on appetite exceeded', 'POST to monitoring system when a pitch blows its budget.', 'Shape Up', 'APPETITE_EXCEEDED', '{}', 'SEND_WEBHOOK', '{"url":"https://hooks.example.com/appetite-exceeded","method":"POST"}', 'Webhook', 12),
  ('Alert on scope creep', 'Notify the team when scope creep is detected in a cycle.', 'Shape Up', 'SCOPE_CREEP_DETECTED', '{}', 'NOTIFY_PROJECT_MEMBERS', '{"message":"Scope creep detected in {{cycleName}}. Review added tasks."}', 'TrendingUp', 13),
  ('Auto-comment on scope creep', 'Post a comment on the pitch when scope creep is detected.', 'Shape Up', 'SCOPE_CREEP_DETECTED', '{}', 'ADD_COMMENT', '{"comment":"Scope creep detected: tasks were added after the betting table was locked."}', 'MessageSquare', 14),
  ('Notify on betting table locked', 'Alert the team when the betting table is locked for a cycle.', 'Shape Up', 'BETTING_TABLE_LOCKED', '{}', 'NOTIFY_PROJECT_MEMBERS', '{"message":"Betting table for {{cycleName}} has been locked. Development begins!"}', 'Lock', 15),
  ('Notify on hill chart move', 'Alert stakeholders when a pitch progresses on the hill chart.', 'Shape Up', 'HILL_CHART_MOVED', '{}', 'NOTIFY_PROJECT_MEMBERS', '{"message":"Hill chart updated for {{pitchTitle}}: now at {{progress}}%."}', 'TrendingUp', 16),
  ('Create retro task on cycle end', 'Automatically create a retrospective task when a cycle closes.', 'Automation', 'CYCLE_ENDED', '{}', 'CREATE_TASK', '{"title":"Run retrospective for {{cycleName}}","status":"IN_PROGRESS"}', 'ClipboardList', 17),
  ('Create wrap-up task on cycle end', 'Create a wrap-up task at the end of every cycle.', 'Automation', 'CYCLE_ENDED', '{}', 'CREATE_TASK', '{"title":"Cycle wrap-up: {{cycleName}}","status":"TODO"}', 'CheckSquare', 18),
  ('Email summary on cycle ended', 'Send an email digest when a cycle finishes.', 'Notifications', 'CYCLE_ENDED', '{}', 'SEND_EMAIL', '{"subject":"Cycle {{cycleName}} wrapped up","body":"Hi team, cycle {{cycleName}} has ended. Please check the retrospective board."}', 'Mail', 19),
  ('Email alert on appetite exceeded', 'Email the project manager when a pitch exceeds its appetite.', 'Notifications', 'APPETITE_EXCEEDED', '{}', 'SEND_EMAIL', '{"subject":"Appetite exceeded: {{pitchTitle}}","body":"The pitch ''{{pitchTitle}}'' has exceeded its appetite of {{appetiteDays}} days. Please review."}', 'Mail', 20);
