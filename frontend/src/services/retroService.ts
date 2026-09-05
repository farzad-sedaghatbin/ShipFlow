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
  targetProjectId?: number;
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
  /**
   * `expectedVersion` is optional so existing callers (and the exact-body unit
   * tests) keep working unchanged; pass the last-known RetroItem.version to
   * enable the optimistic-lock check (v1.13.0 S64) — a mismatch returns HTTP 409.
   */
  updateItem: (itemId: number, content: string, expectedVersion?: number) =>
    api.put<RetroItem>(
      `/retros/items/${itemId}`,
      expectedVersion !== undefined ? { content, expectedVersion } : { content }
    ),
  deleteItem: (itemId: number) => api.delete(`/retros/items/${itemId}`),

  // Voting / reactions
  toggleVote: (itemId: number) => api.post<RetroItem>(`/retros/items/${itemId}/vote`),
  toggleDislike: (itemId: number) => api.post<RetroItem>(`/retros/items/${itemId}/dislike`),
  markDiscussed: (itemId: number, discussed: boolean) =>
    api.post<RetroItem>(`/retros/items/${itemId}/discussed`, { discussed }),

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
