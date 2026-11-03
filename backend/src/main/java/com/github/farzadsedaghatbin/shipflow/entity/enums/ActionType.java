package com.github.farzadsedaghatbin.shipflow.entity.enums;

public enum ActionType {
  NOTIFY_ASSIGNEE,
  NOTIFY_PROJECT_MEMBERS,
  SEND_WEBHOOK,
  SEND_EMAIL,
  ADD_COMMENT,
  CHANGE_TASK_STATUS,
  CREATE_TASK,
  // ShipFlow Agent loops (S71): the agent gathers facts, writes a short summary and posts it
  AGENT_STANDUP_DIGEST,
  AGENT_CYCLE_HEALTH_CHECK,
  AGENT_BETTING_PREP
}
