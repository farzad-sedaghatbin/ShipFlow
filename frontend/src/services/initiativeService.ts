import api from './api';
import { Initiative, CreateInitiativeRequest, InitiativeStatus } from '../types';

export const initiativeService = {
  getByProject: (projectId: number) => 
    api.get<Initiative[]>(`/initiatives/project/${projectId}`),
  
  getByProjectAndStatus: (projectId: number, status: InitiativeStatus) =>
    api.get<Initiative[]>(`/initiatives/project/${projectId}/status/${status}`),
  
  getActiveByProject: (projectId: number) =>
    api.get<Initiative[]>(`/initiatives/project/${projectId}/active`),
  
  getById: (id: number) => 
    api.get<Initiative>(`/initiatives/${id}`),
  
  create: (data: CreateInitiativeRequest) => 
    api.post<Initiative>('/initiatives', data),
  
  update: (id: number, data: CreateInitiativeRequest) => 
    api.put<Initiative>(`/initiatives/${id}`, data),
  
  updateStatus: (id: number, status: InitiativeStatus) => 
    api.patch<Initiative>(`/initiatives/${id}/status?status=${status}`),
  
  delete: (id: number) => 
    api.delete(`/initiatives/${id}`),
};
