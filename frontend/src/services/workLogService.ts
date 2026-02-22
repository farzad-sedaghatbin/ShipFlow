import api from './api';
import { WorkLog, Page, CreateWorkLogRequest, CreateWorkLogForSelfRequest, WorkLogSummary } from '../types';

const DEFAULT_PAGE = 0;
const DEFAULT_SIZE = 20;

export const workLogService = {
  // Admin/Manager endpoints
  getAll: (page = DEFAULT_PAGE, size = DEFAULT_SIZE, projectId?: number) =>
    api.get<Page<WorkLog>>('/worklogs', { params: { page, size, ...(projectId !== undefined ? { projectId } : {}) } }),
  getByPitchId: (pitchId: number, page = DEFAULT_PAGE, size = DEFAULT_SIZE) =>
    api.get<Page<WorkLog>>(`/worklogs/pitch/${pitchId}`, { params: { page, size } }),
  getByTaskId: (taskId: number, page = DEFAULT_PAGE, size = DEFAULT_SIZE) =>
    api.get<Page<WorkLog>>(`/worklogs/task/${taskId}`, { params: { page, size } }),
  getByPersonId: (personId: number, page = DEFAULT_PAGE, size = DEFAULT_SIZE) =>
    api.get<Page<WorkLog>>(`/worklogs/person/${personId}`, { params: { page, size } }),
  getByPersonAndDate: (personId: number, date: string, page = DEFAULT_PAGE, size = DEFAULT_SIZE) =>
    api.get<Page<WorkLog>>(`/worklogs/person/${personId}/date/${date}`, { params: { page, size } }),
  getByCycleId: (cycleId: number, page = DEFAULT_PAGE, size = DEFAULT_SIZE) =>
    api.get<Page<WorkLog>>(`/worklogs/cycle/${cycleId}`, { params: { page, size } }),
  getById: (id: number) => api.get<WorkLog>(`/worklogs/${id}`),
  create: (data: CreateWorkLogRequest) => api.post<WorkLog>('/worklogs', data),
  update: (id: number, data: CreateWorkLogRequest) => api.put<WorkLog>(`/worklogs/${id}`, data),
  delete: (id: number) => api.delete(`/worklogs/${id}`),

  // Current user's own work logs
  getMy: (page = DEFAULT_PAGE, size = DEFAULT_SIZE, projectId?: number) =>
    api.get<Page<WorkLog>>('/worklogs/my', { params: { page, size, ...(projectId !== undefined ? { projectId } : {}) } }),
  getMyByCycle: (cycleId: number, page = DEFAULT_PAGE, size = DEFAULT_SIZE) =>
    api.get<Page<WorkLog>>(`/worklogs/my/cycle/${cycleId}`, { params: { page, size } }),
  getMyByDate: (date: string, page = DEFAULT_PAGE, size = DEFAULT_SIZE) =>
    api.get<Page<WorkLog>>(`/worklogs/my/date/${date}`, { params: { page, size } }),
  createMy: (data: CreateWorkLogForSelfRequest) => api.post<WorkLog>('/worklogs/my', data),
  updateMy: (id: number, data: CreateWorkLogForSelfRequest) => api.put<WorkLog>(`/worklogs/my/${id}`, data),
  deleteMy: (id: number) => api.delete(`/worklogs/my/${id}`),
  getMySummary: (cycleId?: number, projectId?: number) =>
    api.get<WorkLogSummary>('/worklogs/my/summary', { params: { ...(cycleId !== undefined ? { cycleId } : {}), ...(projectId !== undefined ? { projectId } : {}) } }),
};
