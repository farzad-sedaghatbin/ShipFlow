-- S71 ShipFlow Agent loops: time-based automations.
-- next_run_at is the scheduler's bookkeeping: an enabled SCHEDULE automation is due once it is in
-- the past, and the single replica that moves it to the next slot is the one that runs it.
ALTER TABLE workflow_automations ADD COLUMN IF NOT EXISTS next_run_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_automation_next_run ON workflow_automations (next_run_at);

-- Three ready-made agent loops (cron: second minute hour day-of-month month day-of-week).
INSERT INTO workflow_automation_templates (name, description, category, trigger_type, default_trigger_config, action_type, default_action_config, icon_name, sort_order)
VALUES
  ('Daily standup digest', 'Every weekday morning the ShipFlow Agent posts what moved since yesterday: finished, in progress, blocked and overdue work, plus new bugs.', 'Agent', 'SCHEDULE', '{"cron":"0 0 9 * * MON-FRI","timezone":"UTC"}', 'AGENT_STANDUP_DIGEST', '{"lookbackHours":24,"channels":["SLACK","TEAMS","IN_APP"],"useAi":true}', 'Sparkles', 21),
  ('Cycle health check', 'Three times a week the agent checks the active cycle or sprint against the calendar and flags slipping scope, blocked work and overdue tasks. Stays quiet when everything is on track.', 'Agent', 'SCHEDULE', '{"cron":"0 0 10 * * MON,WED,FRI","timezone":"UTC"}', 'AGENT_CYCLE_HEALTH_CHECK', '{"channels":["SLACK","TEAMS","IN_APP"],"onlyWhenAtRisk":true,"useAi":true}', 'Activity', 22),
  ('Betting table prep', 'Before the betting meeting the agent lists the shaped pitches, their appetite against the capacity of the next cycle, and anything that looks risky. Shape Up projects only.', 'Agent', 'SCHEDULE', '{"cron":"0 0 9 * * MON","timezone":"UTC"}', 'AGENT_BETTING_PREP', '{"channels":["SLACK","TEAMS","IN_APP"],"useAi":true}', 'Target', 23);
