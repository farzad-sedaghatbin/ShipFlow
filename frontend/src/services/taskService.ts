import api from './api';
import { Task, CreateTaskRequest, TaskStatistics, TaskStatus, TaskPriority, TaskCategory, Page, TaskDependencies, TaskDependency, CreateTaskDependencyRequest, EntityHistory, BulkTaskUpdateRequest, BulkUpdateResult } from '../types';

export const taskService = {
  // Current user's tasks
  getMy: (page?: number, size?: number, sortBy?: string, sortOrder?: string) => {
    return api.get<Page<Task>>('/tasks/my', {
      params: {
        page: page ?? 0,
        size: size ?? 10,
        sortBy: sortBy ?? 'createdAt',
        sortOrder: sortOrder ?? 'desc',
      },
    });
  },
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
  getAll: (page?: number, size?: number, sortBy?: string, sortOrder?: string) => {
    return api.get<Page<Task>>('/tasks', {
      params: {
        page: page ?? 0,
        size: size ?? 10,
        sortBy: sortBy ?? 'createdAt',
        sortOrder: sortOrder ?? 'desc',
      },
    });
  },
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
  getByPitchId: (pitchId: number) => api.get<Task[]>(`/tasks/pitch/${pitchId}`),
  getByProjectId: (projectId: number) => api.get<Task[]>(`/tasks/project/${projectId}`),
  getByProjectIdPaged: (projectId: number, page?: number, size?: number, sortBy?: string, sortOrder?: string) => {
    return api.get<Page<Task>>(`/tasks/project/${projectId}/paged`, {
      params: {
        page: page ?? 0,
        size: size ?? 10,
        sortBy: sortBy ?? 'createdAt',
        sortOrder: sortOrder ?? 'desc',
      },
    });
  },
  getByProjectIdAndCategory: (projectId: number, category: TaskCategory, page?: number, size?: number, sortBy?: string, sortOrder?: string) => {
    return api.get<Page<Task>>(`/tasks/project/${projectId}/category/${category}`, {
      params: {
        page: page ?? 0,
        size: size ?? 10,
        sortBy: sortBy ?? 'createdAt',
        sortOrder: sortOrder ?? 'desc',
      },
    });
  },
  getByProjectIdWithFilters: (
    projectId: number,
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
    return api.get<Page<Task>>(`/tasks/project/${projectId}/filter`, {
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
  getStatisticsByProjectId: (projectId: number) => api.get<TaskStatistics>(`/tasks/project/${projectId}/statistics`),
  search: (query: string, page?: number, size?: number, sortBy?: string, sortOrder?: string) => {
    return api.get<Page<Task>>('/tasks/search', {
      params: {
        q: query,
        page: page ?? 0,
        size: size ?? 50,
        sortBy: sortBy ?? 'createdAt',
        sortOrder: sortOrder ?? 'desc',
      },
    });
  },
  
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
  updatePriority: (id: number, priority: TaskPriority) => 
    api.patch<Task>(`/tasks/${id}/priority`, { priority }),
  delete: (id: number) => api.delete(`/tasks/${id}`),
  
  // Sub-task hierarchy
  getSubTasks: (parentTaskId: number) => api.get<Task[]>(`/tasks/${parentTaskId}/subtasks`),
  getRootTasks: (cycleId: number) => api.get<Task[]>(`/tasks/cycle/${cycleId}/roots`),
  getTaskTree: (cycleId: number) => api.get<Task[]>(`/tasks/cycle/${cycleId}/tree`),
  
  // Task Dependencies
  addDependency: (taskId: number, data: CreateTaskDependencyRequest) => 
    api.post<TaskDependency>(`/tasks/${taskId}/dependencies`, data),
  getDependencies: (taskId: number) => 
    api.get<TaskDependencies>(`/tasks/${taskId}/dependencies`),
  getBlockingDependencies: (taskId: number) => 
    api.get<TaskDependency[]>(`/tasks/${taskId}/dependencies/blocking`),
  getBlockedByDependencies: (taskId: number) => 
    api.get<TaskDependency[]>(`/tasks/${taskId}/dependencies/blocked-by`),
  removeDependency: (taskId: number, dependencyId: number) => 
    api.delete(`/tasks/${taskId}/dependencies/${dependencyId}`),
  
  // Task History (Audit Trail)
  getHistory: (taskId: number, page?: number, size?: number) =>
    api.get<Page<EntityHistory>>(`/tasks/${taskId}/history`, {
      params: {
        page: page ?? 0,
        size: size ?? 20,
      },
    }),

  // Assign task to a cycle (sprint) or remove from cycle (null = product backlog)
  assignCycle: (taskId: number, cycleId: number | null) =>
    api.patch<Task>(`/tasks/${taskId}/cycle`, { cycleId }),

  // Bulk operations
  bulkUpdate: (request: BulkTaskUpdateRequest) =>
    api.post<BulkUpdateResult>('/tasks/bulk-update', request),

  // Drag-to-reorder: send new sort positions, server validates dependency constraints
  reorderTasks: (items: { id: number; sortOrder: number }[]) =>
    api.patch<void>('/tasks/reorder', { items }),

  // CSV Export — pass exactly one of projectId or cycleId
  exportTasks: (params: {
    projectId?: number;
    cycleId?: number;
    statuses?: TaskStatus[];
    priorities?: TaskPriority[];
    assigneeIds?: number[];
    category?: TaskCategory;
  }) =>
    api.get<Blob>('/tasks/export', {
      params: {
        projectId: params.projectId,
        cycleId: params.cycleId,
        statuses: params.statuses?.join(','),
        priorities: params.priorities?.join(','),
        assigneeIds: params.assigneeIds?.join(','),
        category: params.category,
      },
      responseType: 'blob',
    }),
};
