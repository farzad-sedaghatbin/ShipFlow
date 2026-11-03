import api from './api';
import type {
  AgentLoopPreview,
  CreateAutomationRequest,
  WorkflowAutomation,
  WorkflowAutomationExecution,
  WorkflowAutomationTemplate,
} from '../types/automation';

export const automationService = {
  // Templates
  getTemplates: () =>
    api.get<WorkflowAutomationTemplate[]>('/automations/templates'),

  getTemplatesByCategory: (category: string) =>
    api.get<WorkflowAutomationTemplate[]>(`/automations/templates/category/${category}`),

  // Automation rules
  getByProject: (projectId: number) =>
    api.get<WorkflowAutomation[]>(`/automations/project/${projectId}`),

  getById: (id: number) =>
    api.get<WorkflowAutomation>(`/automations/${id}`),

  create: (data: CreateAutomationRequest) =>
    api.post<WorkflowAutomation>('/automations', data),

  createFromTemplate: (templateId: number, projectId: number, name?: string) =>
    api.post<WorkflowAutomation>(
      `/automations/from-template?templateId=${templateId}&projectId=${projectId}${name ? `&name=${encodeURIComponent(name)}` : ''}`
    ),

  update: (id: number, data: CreateAutomationRequest) =>
    api.put<WorkflowAutomation>(`/automations/${id}`, data),

  toggle: (id: number) =>
    api.patch<WorkflowAutomation>(`/automations/${id}/toggle`),

  delete: (id: number) =>
    api.delete(`/automations/${id}`),

  // Agent loops
  /** Generates what the loop would send right now, without sending anything. */
  preview: (id: number) =>
    api.post<AgentLoopPreview>(`/automations/${id}/preview`),

  /** Runs the loop now, as if its slot had arrived. Resolves to the execution record. */
  runNow: (id: number) =>
    api.post<WorkflowAutomationExecution>(`/automations/${id}/run-now`),

  // Execution logs
  getExecutionLog: (id: number, limit = 50) =>
    api.get<WorkflowAutomationExecution[]>(`/automations/${id}/executions?limit=${limit}`),

  getProjectExecutionLog: (projectId: number, limit = 100) =>
    api.get<WorkflowAutomationExecution[]>(
      `/automations/project/${projectId}/executions?limit=${limit}`
    ),
};
