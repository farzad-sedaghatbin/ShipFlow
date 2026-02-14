import api from './api';
import { Epic, CreateEpicRequest, EpicStatus } from '../types';

export const epicService = {
  getByProject: (projectId: number) => 
    api.get<Epic[]>(`/epics/project/${projectId}`),
  
  getOrphansByProject: (projectId: number) =>
    api.get<Epic[]>(`/epics/project/${projectId}/orphans`),
  
  getByInitiative: (initiativeId: number) =>
    api.get<Epic[]>(`/epics/initiative/${initiativeId}`),
  
  getByProjectAndStatus: (projectId: number, status: EpicStatus) =>
    api.get<Epic[]>(`/epics/project/${projectId}/status/${status}`),
  
  getActiveByProject: (projectId: number) =>
    api.get<Epic[]>(`/epics/project/${projectId}/active`),
  
  getById: (id: number) => 
    api.get<Epic>(`/epics/${id}`),
  
  create: (data: CreateEpicRequest) => 
    api.post<Epic>('/epics', data),
  
  update: (id: number, data: CreateEpicRequest) => 
    api.put<Epic>(`/epics/${id}`, data),
  
  updateStatus: (id: number, status: EpicStatus) => 
    api.patch<Epic>(`/epics/${id}/status?status=${status}`),
  
  linkToInitiative: (id: number, initiativeId: number) =>
    api.patch<Epic>(`/epics/${id}/link-initiative?initiativeId=${initiativeId}`),
  
  unlinkFromInitiative: (id: number) =>
    api.patch<Epic>(`/epics/${id}/unlink-initiative`),
  
  delete: (id: number) => 
    api.delete(`/epics/${id}`),
};
