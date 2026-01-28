import api from './api';
import { WorkLog, CreateWorkLogRequest, CreateWorkLogForSelfRequest } from '../types';

export const workLogService = {
  // Admin/Manager endpoints
  getAll: () => api.get<WorkLog[]>('/worklogs'),
  getByPitchId: (pitchId: number) => api.get<WorkLog[]>(`/worklogs/pitch/${pitchId}`),
  getByTaskId: (taskId: number) => api.get<WorkLog[]>(`/worklogs/task/${taskId}`),
  getByPersonId: (personId: number) => api.get<WorkLog[]>(`/worklogs/person/${personId}`),
  getByPersonAndDate: (personId: number, date: string) => api.get<WorkLog[]>(`/worklogs/person/${personId}/date/${date}`),
  getByCycleId: (cycleId: number) => api.get<WorkLog[]>(`/worklogs/cycle/${cycleId}`),
  getById: (id: number) => api.get<WorkLog>(`/worklogs/${id}`),
  create: (data: CreateWorkLogRequest) => api.post<WorkLog>('/worklogs', data),
  update: (id: number, data: CreateWorkLogRequest) => api.put<WorkLog>(`/worklogs/${id}`, data),
  delete: (id: number) => api.delete(`/worklogs/${id}`),
  
  // Current user's own work logs
  getMy: () => api.get<WorkLog[]>('/worklogs/my'),
  getMyByCycle: (cycleId: number) => api.get<WorkLog[]>(`/worklogs/my/cycle/${cycleId}`),
  getMyByDate: (date: string) => api.get<WorkLog[]>(`/worklogs/my/date/${date}`),
  createMy: (data: CreateWorkLogForSelfRequest) => api.post<WorkLog>('/worklogs/my', data),
  updateMy: (id: number, data: CreateWorkLogForSelfRequest) => api.put<WorkLog>(`/worklogs/my/${id}`, data),
  deleteMy: (id: number) => api.delete(`/worklogs/my/${id}`),
};
