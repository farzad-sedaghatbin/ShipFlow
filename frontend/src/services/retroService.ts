import api from './api';
import {
  Retrospective,
  RetroItem,
  CreateRetroRequest,
  CreateRetroItemRequest,
  UpdateRetroRequest,
  CycleRetroStatus,
} from '../types';

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
};
