import api from './api';
import { Cycle, CreateCycleRequest, CyclePhase, CycleRetroStatus, BurndownPoint, VelocityPoint } from '../types';

export const cycleService = {
  getAll: () => api.get<Cycle[]>('/cycles'),
  getMyCycles: () => api.get<Cycle[]>('/cycles/my-cycles'),
  getMyActiveCycles: () => api.get<Cycle[]>('/cycles/my-cycles/active'),
  getActive: () => api.get<Cycle[]>('/cycles/active'),
  getById: (id: number) => api.get<Cycle>(`/cycles/${id}`),
  getByProject: (projectId: number) => api.get<Cycle[]>(`/cycles/project/${projectId}`),
  getActiveByProject: (projectId: number) => api.get<Cycle[]>(`/cycles/project/${projectId}/active`),
  create: (data: CreateCycleRequest) => api.post<Cycle>('/cycles', data),
  update: (id: number, data: CreateCycleRequest) => api.put<Cycle>(`/cycles/${id}`, data),
  updatePhase: (id: number, phase: CyclePhase) => api.patch<Cycle>(`/cycles/${id}/phase?phase=${phase}`),
  toggleActive: (id: number) => api.patch<Cycle>(`/cycles/${id}/toggle-active`),
  closeCycle: (id: number) => api.post<Cycle>(`/cycles/${id}/close`),
  getRetroStatus: (id: number) => api.get<CycleRetroStatus>(`/cycles/${id}/retro-status`),
  delete: (id: number) => api.delete(`/cycles/${id}`),
  /** Scrum: fetch burndown chart data for a sprint (cycle) */
  getBurndown: (cycleId: number) => api.get<BurndownPoint[]>(`/cycles/${cycleId}/burndown`),
  /** Scrum: fetch velocity chart data for a project */
  getVelocity: (projectId: number) => api.get<VelocityPoint[]>(`/projects/${projectId}/velocity`),
};
