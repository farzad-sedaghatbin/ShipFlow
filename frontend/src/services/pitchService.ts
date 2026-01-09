import api from './api';
import { Pitch, CreatePitchRequest, PitchStatus } from '../types';

export const pitchService = {
  getAll: () => api.get<Pitch[]>('/pitches'),
  getByCycleId: (cycleId: number) => api.get<Pitch[]>(`/pitches/cycle/${cycleId}`),
  getByTeamId: (teamId: number) => api.get<Pitch[]>(`/pitches/team/${teamId}`),
  getById: (id: number) => api.get<Pitch>(`/pitches/${id}`),
  create: (data: CreatePitchRequest) => api.post<Pitch>('/pitches', data),
  update: (id: number, data: CreatePitchRequest) => api.put<Pitch>(`/pitches/${id}`, data),
  updateStatus: (id: number, status: PitchStatus) => api.patch<Pitch>(`/pitches/${id}/status?status=${status}`),
  assignTeam: (id: number, teamId: number) => api.patch<Pitch>(`/pitches/${id}/assign-team/${teamId}`),
  delete: (id: number) => api.delete(`/pitches/${id}`),
};
