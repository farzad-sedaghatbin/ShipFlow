import api from './api';

export enum CooldownActivityType {
  TECH_DEBT = 'TECH_DEBT',
  BUG_FIX = 'BUG_FIX',
  REFACTORING = 'REFACTORING',
  KICKOFF_PREP = 'KICKOFF_PREP',
  LEARNING = 'LEARNING',
  QA = 'QA',
  DOCUMENTATION = 'DOCUMENTATION',
  INFRASTRUCTURE = 'INFRASTRUCTURE',
  RESEARCH = 'RESEARCH',
  PLANNING = 'PLANNING',
  OTHER = 'OTHER',
}

export enum CooldownActivityStatus {
  PLANNED = 'PLANNED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  SKIPPED = 'SKIPPED',
  BLOCKED = 'BLOCKED',
}

export interface CooldownActivityDTO {
  id: number;
  cycleId: number;
  cycleName?: string;
  title: string;
  description?: string;
  activityType: CooldownActivityType;
  status: CooldownActivityStatus;
  assigneeId?: number;
  assigneeUsername?: string;
  createdById?: number;
  createdByUsername?: string;
  estimatedHours?: number;
  actualHours?: number;
  priority?: number;
  relatedPitchId?: number;
  relatedPitchTitle?: string;
  notes?: string;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
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
  clearAssignee?: boolean;
  estimatedHours?: number;
  actualHours?: number;
  priority?: number;
  relatedPitchId?: number;
  clearRelatedPitch?: boolean;
  notes?: string;
}

export interface CooldownSummaryDTO {
  cycleId: number;
  cycleName?: string;
  activities?: CooldownActivityDTO[];
  countByType?: { [key: string]: number };
  countByStatus?: { [key: string]: number };
  totalActivities: number;
  completedCount: number;
  inProgressCount: number;
  plannedCount: number;
  blockedCount: number;
  skippedCount: number;
  totalEstimatedHours: number;
  totalActualHours: number;
  completionPercentage: number;
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
