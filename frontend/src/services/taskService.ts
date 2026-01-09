import api from './api';
import { Task, CreateTaskRequest, TaskStatistics, TaskStatus, TaskPriority, TaskCategory, Page } from '../types';

export const taskService = {
  // Current user's tasks
  getMy: () => api.get<Task[]>('/tasks/my'),
  getMyByCycle: (cycleId: number, page?: number, size?: number, sortBy?: string, sortOrder?: string) => {
    if (page !== undefined) {
      return api.get<Page<Task>>(`/tasks/my/cycle/${cycleId}`, {
        params: {
          page: page ?? 0,
          size: size ?? 10,
          sortBy: sortBy ?? 'createdAt',
          sortOrder: sortOrder ?? 'desc',
        },
      });
    }
    return api.get<Task[]>(`/tasks/my/cycle/${cycleId}`);
  },
  
  // General task management
  getAll: () => api.get<Task[]>('/tasks'),
  getById: (id: number) => api.get<Task>(`/tasks/${id}`),
  getByCycleId: (cycleId: number, page?: number, size?: number, sortBy?: string, sortOrder?: string) => {
    if (page !== undefined) {
      return api.get<Page<Task>>(`/tasks/cycle/${cycleId}`, {
        params: {
          page: page ?? 0,
          size: size ?? 10,
          sortBy: sortBy ?? 'createdAt',
          sortOrder: sortOrder ?? 'desc',
        },
      });
    }
    return api.get<Task[]>(`/tasks/cycle/${cycleId}`);
  },
  getByCycleIdAndCategory: (cycleId: number, category: TaskCategory, page?: number, size?: number, sortBy?: string, sortOrder?: string) => {
    return api.get<Page<Task>>(`/tasks/cycle/${cycleId}/category/${category}`, {
      params: {
        page: page ?? 0,
        size: size ?? 10,
        sortBy: sortBy ?? 'createdAt',
        sortOrder: sortOrder ?? 'desc',
      },
    });
  },
  getByCycleIdAndStatus: (cycleId: number, status: TaskStatus, page?: number, size?: number, sortBy?: string, sortOrder?: string) => {
    if (page !== undefined) {
      return api.get<Page<Task>>(`/tasks/cycle/${cycleId}/status/${status}`, {
        params: {
          page: page ?? 0,
          size: size ?? 10,
          sortBy: sortBy ?? 'createdAt',
          sortOrder: sortOrder ?? 'desc',
        },
      });
    }
    return api.get<Task[]>(`/tasks/cycle/${cycleId}/status/${status}`);
  },
  getByAssigneeId: (assigneeId: number) => api.get<Task[]>(`/tasks/assignee/${assigneeId}`),
  getByPersonId: (personId: number) => api.get<Task[]>(`/tasks/person/${personId}`),
  getByProjectId: (projectId: number) => api.get<Task[]>(`/tasks/project/${projectId}`),
  
  // Multi-filter endpoint
  getWithFilters: (
    cycleId: number,
    statuses?: TaskStatus[],
    priorities?: TaskPriority[],
    assigneeIds?: number[],
    category?: TaskCategory,
    exclude?: boolean,
    page?: number,
    size?: number,
    sortBy?: string,
    sortOrder?: string
  ) => {
    return api.get<Page<Task>>(`/tasks/cycle/${cycleId}/filter`, {
      params: {
        statuses: statuses?.join(','),
        priorities: priorities?.join(','),
        assigneeIds: assigneeIds?.join(','),
        category: category,
        exclude: exclude ?? false,
        page: page ?? 0,
        size: size ?? 10,
        sortBy: sortBy ?? 'createdAt',
        sortOrder: sortOrder ?? 'desc',
      },
    });
  },
  
  // Statistics
  getStatisticsByCycleId: (cycleId: number) => api.get<TaskStatistics>(`/tasks/cycle/${cycleId}/statistics`),
  
  // CRUD operations
  create: (data: CreateTaskRequest) => api.post<Task>('/tasks', data),
  update: (id: number, data: CreateTaskRequest) => api.put<Task>(`/tasks/${id}`, data),
  updateStatus: (id: number, status: TaskStatus) => 
    api.patch<Task>(`/tasks/${id}/status`, { status }),
  delete: (id: number) => api.delete(`/tasks/${id}`),
};
