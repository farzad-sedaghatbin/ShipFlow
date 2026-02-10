import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useToast } from '../contexts';
import { taskService } from '../services/taskService';
import { cycleService } from '../services/cycleService';
import { personService } from '../services/personService';
import timerService from '../services/timerService';
import { 
  Task, 
  TaskStatus, 
  TaskPriority, 
  TaskCategory,
  CreateTaskRequest 
} from '../types';
import { getUserFriendlyError } from '../utils/errorMessages';

// Query keys
export const backlogKeys = {
  all: ['backlog'] as const,
  cycles: () => [...backlogKeys.all, 'cycles'] as const,
  persons: () => [...backlogKeys.all, 'persons'] as const,
  activeTimer: () => [...backlogKeys.all, 'activeTimer'] as const,
  tasks: (params: TaskQueryParams) => [...backlogKeys.all, 'tasks', params] as const,
  statistics: (params: StatisticsQueryParams) => [...backlogKeys.all, 'statistics', params] as const,
  subtasks: (taskId: number) => [...backlogKeys.all, 'subtasks', taskId] as const,
};

export interface TaskQueryParams {
  cycleId?: number | 'all';
  projectId?: number;
  isKanban?: boolean;
  category: TaskCategory;
  tabValue: 'all' | 'my';
  statusFilter?: TaskStatus[];
  priorityFilter?: TaskPriority[];
  assigneeFilter?: number[];
  dependencyFilter?: 'all' | 'blocked' | 'blocking';
  excludeMode?: boolean;
  page: number;
  pageSize: number;
  sortBy: string;
  sortOrder: 'asc' | 'desc';
  userId?: number;
}

export interface StatisticsQueryParams {
  cycleId?: number | 'all';
  projectId?: number;
  isKanban?: boolean;
}

/**
 * Hook to fetch active cycles for the current user (backlog-specific)
 */
export function useBacklogCycles() {
  return useQuery({
    queryKey: backlogKeys.cycles(),
    queryFn: async () => {
      const response = await cycleService.getMyActiveCycles();
      return response.data;
    },
    staleTime: 60000, // 1 minute
  });
}

/**
 * Hook to fetch all persons (backlog-specific)
 */
export function useBacklogPersons() {
  return useQuery({
    queryKey: backlogKeys.persons(),
    queryFn: async () => {
      return await personService.getAll();
    },
    staleTime: 60000,
  });
}

/**
 * Hook to fetch active timer
 */
export function useActiveTimer() {
  return useQuery({
    queryKey: backlogKeys.activeTimer(),
    queryFn: async () => {
      return await timerService.getActiveTimer();
    },
    staleTime: 10000, // 10 seconds
  });
}

/**
 * Hook to fetch tasks with all filters
 */
export function useBacklogTasks(params: TaskQueryParams) {
  const { 
    cycleId, 
    projectId, 
    isKanban,
    category, 
    tabValue, 
    statusFilter,
    priorityFilter,
    assigneeFilter,
    dependencyFilter,
    excludeMode,
    page, 
    pageSize, 
    sortBy, 
    sortOrder,
    userId 
  } = params;

  const enabled = isKanban 
    ? !!projectId 
    : cycleId !== undefined && cycleId !== null;

  return useQuery({
    queryKey: backlogKeys.tasks(params),
    queryFn: async () => {
      // For Kanban projects, load tasks by project
      if (isKanban && projectId) {
        const response = await taskService.getByProjectIdAndCategory(
          projectId,
          category,
          page,
          pageSize,
          sortBy,
          sortOrder
        );
        return applyClientFilters(
          response?.data?.content || [],
          response?.data?.totalElements || 0,
          { statusFilter, priorityFilter, assigneeFilter, dependencyFilter, excludeMode, tabValue, userId }
        );
      }

      // For Shape Up projects
      if (!cycleId) {
        return { tasks: [], totalElements: 0 };
      }

      let response: any;
      
      if (tabValue === 'my') {
        if (cycleId === 'all') {
          response = await taskService.getMy(page, pageSize, sortBy, sortOrder);
        } else {
          response = await taskService.getMyByCycle(cycleId, page, pageSize, sortBy, sortOrder);
        }
        const allTasks = response?.data?.content || [];
        const filteredTasks = allTasks.filter((task: Task) => {
          const taskCategory = task.category || 'PITCH_SCOPE';
          return taskCategory === category;
        });
        return { tasks: filteredTasks, totalElements: filteredTasks.length };
      }
      
      if (cycleId === 'all') {
        response = await taskService.getAll(page, pageSize, sortBy, sortOrder);
        const allTasks = response?.data?.content || [];
        const filteredByCategory = allTasks.filter((task: Task) => {
          const taskCategory = task.category || 'PITCH_SCOPE';
          return taskCategory === category;
        });
        return applyClientFilters(
          filteredByCategory,
          response?.data?.totalElements || 0,
          { statusFilter, priorityFilter, assigneeFilter, dependencyFilter, excludeMode, tabValue, userId }
        );
      }
      
      // Use filter endpoint or category endpoint
      const hasFilters = (statusFilter && statusFilter.length > 0) || 
                        (priorityFilter && priorityFilter.length > 0) || 
                        (assigneeFilter && assigneeFilter.length > 0);
      
      if (hasFilters) {
        response = await taskService.getWithFilters(
          cycleId,
          statusFilter?.length ? statusFilter : undefined,
          priorityFilter?.length ? priorityFilter : undefined,
          assigneeFilter?.length ? assigneeFilter : undefined,
          category,
          excludeMode,
          page,
          pageSize,
          sortBy,
          sortOrder
        );
      } else {
        response = await taskService.getByCycleIdAndCategory(cycleId, category, page, pageSize, sortBy, sortOrder);
      }
      
      const tasks = response?.data?.content || [];
      return applyDependencyFilter(tasks, response?.data?.totalElements || 0, dependencyFilter);
    },
    enabled,
    staleTime: 30000,
  });
}

/**
 * Apply client-side filters (used when server doesn't support all filters)
 */
function applyClientFilters(
  tasks: Task[],
  totalElements: number,
  filters: {
    statusFilter?: TaskStatus[];
    priorityFilter?: TaskPriority[];
    assigneeFilter?: number[];
    dependencyFilter?: 'all' | 'blocked' | 'blocking';
    excludeMode?: boolean;
    tabValue: 'all' | 'my';
    userId?: number;
  }
) {
  let filteredTasks = [...tasks];
  const { statusFilter, priorityFilter, assigneeFilter, dependencyFilter, excludeMode, tabValue, userId } = filters;

  if (statusFilter && statusFilter.length > 0) {
    filteredTasks = filteredTasks.filter((t: Task) =>
      excludeMode ? !statusFilter.includes(t.status) : statusFilter.includes(t.status)
    );
  }
  
  if (priorityFilter && priorityFilter.length > 0) {
    filteredTasks = filteredTasks.filter((t: Task) =>
      excludeMode ? !priorityFilter.includes(t.priority) : priorityFilter.includes(t.priority)
    );
  }
  
  if (assigneeFilter && assigneeFilter.length > 0) {
    filteredTasks = filteredTasks.filter((t: Task) =>
      excludeMode ? !assigneeFilter.includes(t.assigneeId || 0) : assigneeFilter.includes(t.assigneeId || 0)
    );
  }

  if (tabValue === 'my' && userId) {
    filteredTasks = filteredTasks.filter((t: Task) => t.assigneeId === userId);
  }

  return applyDependencyFilter(filteredTasks, totalElements, dependencyFilter);
}

/**
 * Apply dependency filter
 */
function applyDependencyFilter(
  tasks: Task[],
  totalElements: number,
  dependencyFilter?: 'all' | 'blocked' | 'blocking'
) {
  if (!dependencyFilter || dependencyFilter === 'all') {
    return { tasks, totalElements };
  }

  let filteredTasks = tasks;
  if (dependencyFilter === 'blocked') {
    filteredTasks = tasks.filter((t: Task) => t.isBlocked && t.blockedByCount && t.blockedByCount > 0);
  } else if (dependencyFilter === 'blocking') {
    filteredTasks = tasks.filter((t: Task) => t.blockingTasks && t.blockingTasks.length > 0);
  }

  return { tasks: filteredTasks, totalElements };
}

/**
 * Hook to fetch statistics
 */
export function useBacklogStatistics(params: StatisticsQueryParams) {
  const { cycleId, projectId, isKanban } = params;

  const enabled = isKanban 
    ? !!projectId 
    : cycleId !== undefined && cycleId !== null && cycleId !== 'all';

  return useQuery({
    queryKey: backlogKeys.statistics(params),
    queryFn: async () => {
      if (isKanban && projectId) {
        const response = await taskService.getStatisticsByProjectId(projectId);
        return response.data;
      }
      
      if (typeof cycleId === 'number') {
        const response = await taskService.getStatisticsByCycleId(cycleId);
        return response.data;
      }
      
      return null;
    },
    enabled,
    staleTime: 30000,
  });
}

/**
 * Hook to fetch subtasks
 */
export function useSubtasks(taskId: number | null) {
  return useQuery({
    queryKey: backlogKeys.subtasks(taskId!),
    queryFn: async () => {
      const response = await taskService.getSubTasks(taskId!);
      return response.data;
    },
    enabled: taskId !== null,
    staleTime: 30000,
  });
}

/**
 * Hook to create a task
 */
export function useCreateTask() {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToast();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: (data: CreateTaskRequest) => taskService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: backlogKeys.all });
      showSuccess(t('backlogPage.taskCreated'));
    },
    onError: (error: any) => {
      showError(getUserFriendlyError(error));
    },
  });
}

/**
 * Hook to update a task
 */
export function useUpdateTask() {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToast();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateTaskRequest }) =>
      taskService.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: backlogKeys.all });
      showSuccess(t('backlogPage.taskUpdated'));
    },
    onError: (error: any) => {
      showError(getUserFriendlyError(error));
    },
  });
}

/**
 * Hook to delete a task
 */
export function useDeleteTask() {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToast();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: (taskId: number) => taskService.delete(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: backlogKeys.all });
      showSuccess(t('backlogPage.taskDeleted'));
    },
    onError: (error: any) => {
      showError(getUserFriendlyError(error));
    },
  });
}

/**
 * Hook to update task status
 */
export function useUpdateTaskStatus() {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToast();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: ({ taskId, status }: { taskId: number; status: TaskStatus }) =>
      taskService.updateStatus(taskId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: backlogKeys.all });
      showSuccess(t('backlogPage.statusUpdated'));
    },
    onError: (error: any) => {
      showError(getUserFriendlyError(error));
    },
  });
}

/**
 * Hook to update task priority
 */
export function useUpdateTaskPriority() {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToast();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: ({ taskId, priority }: { taskId: number; priority: TaskPriority }) =>
      taskService.updatePriority(taskId, priority),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: backlogKeys.all });
      showSuccess(t('backlogPage.priorityUpdated'));
    },
    onError: (error: any) => {
      showError(getUserFriendlyError(error));
    },
  });
}

/**
 * Hook to start timer for a task
 */
export function useStartTimer() {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToast();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: ({ taskId, taskTitle }: { taskId: number; taskTitle: string }) =>
      timerService.startTimer({ taskId, note: `Working on: ${taskTitle}` }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: backlogKeys.activeTimer() });
      showSuccess(t('backlogPage.timerStarted'));
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || 'Failed to start timer';
      showError(message);
    },
  });
}
