import api from './api';
import {
  Retrospective,
  RetroItem,
  CreateRetroRequest,
  CreateRetroItemRequest,
  UpdateRetroRequest,
  CycleRetroStatus,
  Pitch,
} from '../types';

// v0.5 - Action tracking types
export interface MarkActedOnRequest {
  actedOn: boolean;
  notes?: string;
}

export interface RetroActionStats {
  retrospectiveId: number;
  totalActionItems: number;
  actedOnCount: number;
  pendingCount: number;
  followThroughRate: number;
}

export interface ConvertToPitchRequest {
  retroItemIds?: number[];
  targetCycleId?: number;
  customTitle?: string;
  additionalNotes?: string;
  appetiteDays?: number;
}

export const retroService = {
  // Retro CRUD
  getByProject: (projectId: number) => api.get<Retrospective[]>(`/retros/project/${projectId}`),
  getByCycle: (cycleId: number) => api.get<Retrospective[]>(`/retros/cycle/${cycleId}`),
  getById: (id: number) => api.get<Retrospective>(`/retros/${id}`),
  getWithItems: (id: number) => api.get<Retrospective>(`/retros/${id}/with-items`),
  create: (data: CreateRetroRequest) => api.post<Retrospective>('/retros', data),
  update: (id: number, data: UpdateRetroRequest) => api.put<Retrospective>(`/retros/${id}`, data),
  open: (id: number) => api.post<Retrospective>(`/retros/${id}/open`),
  close: (id: number) => api.post<Retrospective>(`/retros/${id}/close`),
  delete: (id: number) => api.delete(`/retros/${id}`),

  // Retro Items
  getItems: (retroId: number) => api.get<RetroItem[]>(`/retros/${retroId}/items`),
  createItem: (data: CreateRetroItemRequest) => api.post<RetroItem>('/retros/items', data),
  updateItem: (itemId: number, content: string) => api.put<RetroItem>(`/retros/items/${itemId}`, { content }),
  deleteItem: (itemId: number) => api.delete(`/retros/items/${itemId}`),

  // Voting
  toggleVote: (itemId: number) => api.post<RetroItem>(`/retros/items/${itemId}/vote`),

  // Merging
  mergeItems: (targetItemId: number, sourceItemId: number) =>
    api.post<RetroItem>(`/retros/items/${targetItemId}/merge/${sourceItemId}`),
  unmergeItem: (itemId: number) => api.post<RetroItem>(`/retros/items/${itemId}/unmerge`),

  // Cycle status
  getCycleStatus: (cycleId: number) => api.get<CycleRetroStatus>(`/retros/cycle/${cycleId}/status`),
  canCloseCycle: (cycleId: number) => api.get<{ canClose: boolean }>(`/retros/cycle/${cycleId}/can-close`),

  // Project settings
  isEnabled: (projectId: number) => api.get<{ enabled: boolean }>(`/retros/project/${projectId}/enabled`),
  setEnabled: (projectId: number, enabled: boolean) =>
    api.put<{ enabled: boolean }>(`/retros/project/${projectId}/enabled`, { enabled }),

  // v0.5 - Action tracking
  markActedOn: (itemId: number, request: MarkActedOnRequest) =>
    api.post<RetroItem>(`/retros/items/${itemId}/acted-on`, request),
  getActionStats: (retroId: number) => 
    api.get<RetroActionStats>(`/retros/${retroId}/action-stats`),
  getPendingActions: (projectId: number) =>
    api.get<RetroItem[]>(`/retros/project/${projectId}/pending-actions`),

  // v0.5 - Convert to pitch draft
  convertToPitchDraft: (retroId: number, request: ConvertToPitchRequest) =>
    api.post<Pitch>(`/retros/${retroId}/convert-to-pitch`, { 
      retrospectiveId: retroId,
      ...request 
    }),
};
