import api from './api';
import { Project, CreateProjectRequest } from '../types';

export const projectService = {
  getAll: async (): Promise<Project[]> => {
    const response = await api.get<Project[]>('/projects');
    return response.data;
  },

  getActive: async (): Promise<Project[]> => {
    const response = await api.get<Project[]>('/projects/active');
    return response.data;
  },

  getById: async (id: number): Promise<Project> => {
    const response = await api.get<Project>(`/projects/${id}`);
    return response.data;
  },

  getByKey: async (key: string): Promise<Project> => {
    const response = await api.get<Project>(`/projects/key/${key}`);
    return response.data;
  },

  create: async (request: CreateProjectRequest): Promise<Project> => {
    const response = await api.post<Project>('/projects', request);
    return response.data;
  },

  update: async (id: number, request: CreateProjectRequest): Promise<Project> => {
    const response = await api.put<Project>(`/projects/${id}`, request);
    return response.data;
  },

  deactivate: async (id: number): Promise<Project> => {
    const response = await api.post<Project>(`/projects/${id}/deactivate`);
    return response.data;
  },

  activate: async (id: number): Promise<Project> => {
    const response = await api.post<Project>(`/projects/${id}/activate`);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/projects/${id}`);
  },
};

export default projectService;
