import api from './api';
import { Release, CreateReleaseRequest, ReleaseStatus, ReleaseProgress } from '../types';

export const releaseService = {
  getByProject: (projectId: number) => 
    api.get<Release[]>(`/releases/project/${projectId}`),
  
  getUpcomingByProject: (projectId: number) =>
    api.get<Release[]>(`/releases/project/${projectId}/upcoming`),
  
  getReleasedByProject: (projectId: number) =>
    api.get<Release[]>(`/releases/project/${projectId}/released`),
  
  getByProjectAndStatus: (projectId: number, status: ReleaseStatus) =>
    api.get<Release[]>(`/releases/project/${projectId}/status/${status}`),
  
  getById: (id: number) => 
    api.get<Release>(`/releases/${id}`),
  
  getProgress: (id: number) =>
    api.get<ReleaseProgress>(`/releases/${id}/progress`),
  
  create: (data: CreateReleaseRequest) => 
    api.post<Release>('/releases', data),
  
  update: (id: number, data: CreateReleaseRequest) => 
    api.put<Release>(`/releases/${id}`, data),
  
  updateStatus: (id: number, status: ReleaseStatus) => 
    api.patch<Release>(`/releases/${id}/status?status=${status}`),
  
  linkCycle: (id: number, cycleId: number) =>
    api.patch<Release>(`/releases/${id}/link-cycle?cycleId=${cycleId}`),
  
  unlinkCycle: (id: number, cycleId: number) =>
    api.patch<Release>(`/releases/${id}/unlink-cycle?cycleId=${cycleId}`),
  
  delete: (id: number) => 
    api.delete(`/releases/${id}`),
};
