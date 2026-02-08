import api from './api';

export enum CooldownActivityType {
  BUG_FIX = 'BUG_FIX',
  TECH_DEBT = 'TECH_DEBT',
  RESEARCH = 'RESEARCH',
  EXPERIMENT = 'EXPERIMENT',
  TRAINING = 'TRAINING',
  DOCUMENTATION = 'DOCUMENTATION',
}

export enum CooldownActivityStatus {
  PLANNED = 'PLANNED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

export interface CooldownActivityDTO {
  id: number;
  cycleId: number;
  title: string;
  description?: string;
  activityType: CooldownActivityType;
  status: CooldownActivityStatus;
  assigneeId?: number;
  assigneeName?: string;
  estimatedHours?: number;
  actualHours?: number;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
}

export interface CreateCooldownActivityRequest {
  cycleId: number;
  title: string;
  description?: string;
  activityType: CooldownActivityType;
  assigneeId?: number;
  estimatedHours?: number;
}

export interface UpdateCooldownActivityRequest {
  title?: string;
  description?: string;
  activityType?: CooldownActivityType;
  status?: CooldownActivityStatus;
  assigneeId?: number;
  estimatedHours?: number;
  actualHours?: number;
}

export interface CooldownSummaryDTO {
  cycleId: number;
  totalActivities: number;
  plannedCount: number;
  inProgressCount: number;
  completedCount: number;
  cancelledCount: number;
  completionRate: number;
  totalEstimatedHours: number;
  totalActualHours: number;
  byType: {
    [key in CooldownActivityType]: number;
  };
}

class CooldownActivityService {
  async createActivity(request: CreateCooldownActivityRequest): Promise<{ data: CooldownActivityDTO }> {
    return api.post('/cooldown-activities', request);
  }

  async getActivity(id: number): Promise<{ data: CooldownActivityDTO }> {
    return api.get(`/cooldown-activities/${id}`);
  }

  async getActivitiesByCycle(cycleId: number): Promise<{ data: CooldownActivityDTO[] }> {
    return api.get(`/cooldown-activities/cycle/${cycleId}`);
  }

  async updateActivity(id: number, request: UpdateCooldownActivityRequest): Promise<{ data: CooldownActivityDTO }> {
    return api.put(`/cooldown-activities/${id}`, request);
  }

  async deleteActivity(id: number): Promise<void> {
    return api.delete(`/cooldown-activities/${id}`);
  }

  async getCycleSummary(cycleId: number): Promise<{ data: CooldownSummaryDTO }> {
    return api.get(`/cooldown-activities/cycle/${cycleId}/summary`);
  }

  async getActivityTypes(): Promise<{ data: CooldownActivityType[] }> {
    return api.get('/cooldown-activities/types');
  }
}

export const cooldownActivityService = new CooldownActivityService();
