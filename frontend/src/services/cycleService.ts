import api from './api';
import { Cycle, CreateCycleRequest, CyclePhase, CycleRetroStatus, BurndownPoint, VelocityPoint, BurnupPoint, SprintReport, ReleaseReport, Team } from '../types';

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
  /** Scrum: fetch burnup chart data for a sprint (cycle) */
  getBurnup: (cycleId: number) => api.get<BurnupPoint[]>(`/cycles/${cycleId}/burnup`),
  /** Scrum: fetch sprint report data for a sprint (cycle) */
  getSprintReport: (cycleId: number) => api.get<SprintReport>(`/cycles/${cycleId}/sprint-report`),
  /** Scrum: fetch release report data for a project */
  getReleaseReport: (projectId: number) => api.get<ReleaseReport[]>(`/projects/${projectId}/release-report`),

  // Cycle–Team assignment
  getTeamsForCycle: (cycleId: number) => api.get<Team[]>(`/cycles/${cycleId}/teams`),
  assignTeamToCycle: (cycleId: number, teamId: number) => api.post<void>(`/cycles/${cycleId}/teams/${teamId}`, {}),
  removeTeamFromCycle: (cycleId: number, teamId: number) => api.delete(`/cycles/${cycleId}/teams/${teamId}`),
};
