import api from './api';
import { Project, CreateProjectRequest, ProjectRole, ProjectMember } from '../types';

export const projectService = {
  /**
   * Get all projects (Admin only)
   */
  getAll: async (): Promise<Project[]> => {
    const response = await api.get<Project[]>('/projects');
    return response.data;
  },

  /**
   * Get projects accessible to the current user.
   * This is the primary method for listing projects.
   */
  getMyProjects: async (): Promise<Project[]> => {
    const response = await api.get<Project[]>('/projects/my-projects');
    return response.data;
  },

  /**
   * Get active projects accessible to the current user.
   */
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

  /**
   * Check if current user has access to a project
   */
  hasAccess: async (id: number): Promise<boolean> => {
    const response = await api.get<boolean>(`/projects/${id}/has-access`);
    return response.data;
  },

  /**
   * Get users with access to a project
   */
  getMembers: async (projectId: number): Promise<ProjectMember[]> => {
    const response = await api.get<ProjectMember[]>(`/projects/${projectId}/members`);
    return response.data;
  },

  /**
   * Get the calling user's role for a specific project
   */
  getMyRole: async (projectId: number): Promise<ProjectRole | null> => {
    const response = await api.get<ProjectRole | null>(`/projects/${projectId}/my-role`);
    return response.data;
  },

  /**
   * Grant project access to a user
   */
  grantAccess: async (projectId: number, userId: number, role: ProjectRole = 'VIEWER'): Promise<void> => {
    await api.post(`/projects/${projectId}/members/${userId}`, null, {
      params: { role }
    });
  },

  /**
   * Revoke project access from a user
   */
  revokeAccess: async (projectId: number, userId: number): Promise<void> => {
    await api.delete(`/projects/${projectId}/members/${userId}`);
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
