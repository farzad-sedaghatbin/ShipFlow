import api from './api';
import { Team, CreateTeamRequest } from '../types';

export const teamService = {
  getAll: () => api.get<Team[]>('/teams'),
  getAllIncludingArchived: () => api.get<Team[]>('/teams?includeArchived=true'),
  getByCycleId: (cycleId: number) => api.get<Team[]>(`/teams/cycle/${cycleId}`),
  getById: (id: number) => api.get<Team>(`/teams/${id}`),
  create: (data: CreateTeamRequest) => api.post<Team>('/teams', data),
  update: (id: number, data: CreateTeamRequest) => api.put<Team>(`/teams/${id}`, data),
  archive: (id: number) => api.patch(`/teams/${id}/archive`),
  unarchive: (id: number) => api.patch(`/teams/${id}/unarchive`),
};
