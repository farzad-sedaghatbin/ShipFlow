export type TriggerType =
  | 'TASK_CREATED'
  | 'TASK_STATUS_CHANGED'
  | 'TASK_ASSIGNED'
  | 'TASK_COMPLETED'
  | 'PITCH_CREATED'
  | 'PITCH_STATUS_CHANGED'
  | 'CYCLE_STARTED'
  | 'CYCLE_ENDED'
  | 'CYCLE_STATUS_CHANGED'
  | 'COMMENT_ADDED'
  | 'BETTING_TABLE_LOCKED'
  | 'HILL_CHART_MOVED'
  | 'APPETITE_EXCEEDED'
  | 'SCOPE_CREEP_DETECTED';

export type ActionType =
  | 'NOTIFY_ASSIGNEE'
  | 'NOTIFY_PROJECT_MEMBERS'
  | 'SEND_WEBHOOK'
  | 'SEND_EMAIL'
  | 'ADD_COMMENT'
  | 'CHANGE_TASK_STATUS'
  | 'CREATE_TASK';

export type AutomationExecutionStatus = 'SUCCESS' | 'FAILURE' | 'SKIPPED';

export interface WorkflowAutomation {
  id: number;
  name: string;
  description?: string;
  projectId: number;
  triggerType: TriggerType;
  triggerConfig?: string;
  actionType: ActionType;
  actionConfig?: string;
  enabled: boolean;
  templateId?: number;
  templateName?: string;
  executionCount: number;
  lastTriggeredAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkflowAutomationTemplate {
  id: number;
  name: string;
  description?: string;
  category: string;
  triggerType: TriggerType;
  defaultTriggerConfig?: string;
  actionType: ActionType;
  defaultActionConfig?: string;
  iconName?: string;
  builtIn: boolean;
  sortOrder: number;
}

export interface WorkflowAutomationExecution {
  id: number;
  automationId: number;
  automationName?: string;
  triggerEventType: string;
  triggerEventData?: string;
  status: AutomationExecutionStatus;
  resultMessage?: string;
  executedAt: string;
}

export interface CreateAutomationRequest {
  name: string;
  description?: string;
  projectId: number;
  triggerType: TriggerType;
  triggerConfig?: string;
  actionType: ActionType;
  actionConfig?: string;
  enabled: boolean;
  templateId?: number;
}
