/**
 * Custom React Query hooks for common data fetching patterns.
 * These hooks provide type-safe, cached data fetching with automatic
 * error handling and loading states.
 */

import { useQuery, useMutation, useQueryClient, UseQueryOptions, UseMutationOptions } from '@tanstack/react-query';
import { queryKeys, STALE_TIMES } from '@/lib/queryClient';
import api from '@/services/api';
import { AxiosError } from 'axios';
import { 
  Project, 
  CreateProjectRequest,
  Cycle, 
  CreateCycleRequest, 
  CyclePhase,
  Pitch,
  CreatePitchRequest,
  PitchStatus,
  Team,
  CreateTeamRequest,
} from '@/types';

// ============================================
// PROJECT HOOKS
// ============================================

/**
 * Hook to fetch all projects
 */
export function useProjects(options?: Omit<UseQueryOptions<Project[], AxiosError>, 'queryKey' | 'queryFn'>) {
  return useQuery({
    queryKey: queryKeys.projects.lists(),
    queryFn: async () => {
      const response = await api.get<Project[]>('/projects');
      return response.data;
    },
    staleTime: STALE_TIMES.entities,
    ...options,
  });
}

/**
 * Hook to fetch active projects
 */
export function useActiveProjects(options?: Omit<UseQueryOptions<Project[], AxiosError>, 'queryKey' | 'queryFn'>) {
  return useQuery({
    queryKey: queryKeys.projects.active(),
    queryFn: async () => {
      const response = await api.get<Project[]>('/projects/active');
      return response.data;
    },
    staleTime: STALE_TIMES.entities,
    ...options,
  });
}

/**
 * Hook to fetch a single project by ID
 */
export function useProject(
  id: number | string | undefined,
  options?: Omit<UseQueryOptions<Project, AxiosError>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: queryKeys.projects.detail(id!),
    queryFn: async () => {
      const response = await api.get<Project>(`/projects/${id}`);
      return response.data;
    },
    enabled: !!id,
    staleTime: STALE_TIMES.entities,
    ...options,
  });
}

/**
 * Hook to create a new project
 */
export function useCreateProject(
  options?: UseMutationOptions<Project, AxiosError, CreateProjectRequest>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateProjectRequest) => {
      const response = await api.post<Project>('/projects', data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.projects.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.projects.active() });
    },
    ...options,
  });
}

/**
 * Hook to update an existing project
 */
export function useUpdateProject(
  options?: UseMutationOptions<Project, AxiosError, { id: number; data: CreateProjectRequest }>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }: { id: number; data: CreateProjectRequest }) => {
      const response = await api.put<Project>(`/projects/${id}`, data);
      return response.data;
    },
    onSuccess: (updatedProject) => {
      queryClient.setQueryData(queryKeys.projects.detail(updatedProject.id), updatedProject);
      queryClient.invalidateQueries({ queryKey: queryKeys.projects.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.projects.active() });
    },
    ...options,
  });
}

/**
 * Hook to delete a project
 */
export function useDeleteProject(
  options?: UseMutationOptions<void, AxiosError, number>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/projects/${id}`);
    },
    onSuccess: (_, deletedId) => {
      queryClient.removeQueries({ queryKey: queryKeys.projects.detail(deletedId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.projects.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.projects.active() });
    },
    ...options,
  });
}

// ============================================
// CYCLE HOOKS
// ============================================

/**
 * Hook to fetch all cycles
 */
export function useCycles(options?: Omit<UseQueryOptions<Cycle[], AxiosError>, 'queryKey' | 'queryFn'>) {
  return useQuery({
    queryKey: queryKeys.cycles.lists(),
    queryFn: async () => {
      const response = await api.get<Cycle[]>('/cycles');
      return response.data;
    },
    staleTime: STALE_TIMES.entities,
    ...options,
  });
}

/**
 * Hook to fetch active cycles
 */
export function useActiveCycles(options?: Omit<UseQueryOptions<Cycle[], AxiosError>, 'queryKey' | 'queryFn'>) {
  return useQuery({
    queryKey: queryKeys.cycles.active(),
    queryFn: async () => {
      const response = await api.get<Cycle[]>('/cycles/active');
      return response.data;
    },
    staleTime: STALE_TIMES.entities,
    ...options,
  });
}

/**
 * Hook to fetch cycles for a project
 */
export function useCyclesByProject(
  projectId: number | string | undefined,
  options?: Omit<UseQueryOptions<Cycle[], AxiosError>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: queryKeys.cycles.byProject(projectId!),
    queryFn: async () => {
      const response = await api.get<Cycle[]>(`/cycles/project/${projectId}`);
      return response.data;
    },
    enabled: !!projectId,
    staleTime: STALE_TIMES.entities,
    ...options,
  });
}

/**
 * Hook to fetch active cycles for a project
 */
export function useActiveCyclesByProject(
  projectId: number | string | undefined,
  options?: Omit<UseQueryOptions<Cycle[], AxiosError>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: [...queryKeys.cycles.byProject(projectId!), 'active'] as const,
    queryFn: async () => {
      const response = await api.get<Cycle[]>(`/cycles/project/${projectId}/active`);
      return response.data;
    },
    enabled: !!projectId,
    staleTime: STALE_TIMES.entities,
    ...options,
  });
}

/**
 * Hook to fetch a single cycle
 */
export function useCycle(
  id: number | string | undefined,
  options?: Omit<UseQueryOptions<Cycle, AxiosError>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: queryKeys.cycles.detail(id!),
    queryFn: async () => {
      const response = await api.get<Cycle>(`/cycles/${id}`);
      return response.data;
    },
    enabled: !!id,
    staleTime: STALE_TIMES.entities,
    ...options,
  });
}

/**
 * Hook to create a new cycle
 */
export function useCreateCycle(
  options?: UseMutationOptions<Cycle, AxiosError, CreateCycleRequest>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateCycleRequest) => {
      const response = await api.post<Cycle>('/cycles', data);
      return response.data;
    },
    onSuccess: (newCycle) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.cycles.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.cycles.active() });
      if (newCycle.projectId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.cycles.byProject(newCycle.projectId) });
      }
    },
    ...options,
  });
}

/**
 * Hook to update an existing cycle
 */
export function useUpdateCycle(
  options?: UseMutationOptions<Cycle, AxiosError, { id: number; data: CreateCycleRequest }>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }: { id: number; data: CreateCycleRequest }) => {
      const response = await api.put<Cycle>(`/cycles/${id}`, data);
      return response.data;
    },
    onSuccess: (updatedCycle) => {
      queryClient.setQueryData(queryKeys.cycles.detail(updatedCycle.id), updatedCycle);
      queryClient.invalidateQueries({ queryKey: queryKeys.cycles.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.cycles.active() });
      if (updatedCycle.projectId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.cycles.byProject(updatedCycle.projectId) });
      }
    },
    ...options,
  });
}

/**
 * Hook to update cycle phase
 */
export function useUpdateCyclePhase(
  options?: UseMutationOptions<Cycle, AxiosError, { id: number; phase: CyclePhase }>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, phase }: { id: number; phase: CyclePhase }) => {
      const response = await api.patch<Cycle>(`/cycles/${id}/phase?phase=${phase}`);
      return response.data;
    },
    onSuccess: (updatedCycle) => {
      queryClient.setQueryData(queryKeys.cycles.detail(updatedCycle.id), updatedCycle);
      queryClient.invalidateQueries({ queryKey: queryKeys.cycles.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.cycles.active() });
    },
    ...options,
  });
}

/**
 * Hook to delete a cycle
 */
export function useDeleteCycle(
  options?: UseMutationOptions<void, AxiosError, number>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/cycles/${id}`);
    },
    onSuccess: (_, deletedId) => {
      queryClient.removeQueries({ queryKey: queryKeys.cycles.detail(deletedId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.cycles.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.cycles.active() });
    },
    ...options,
  });
}

// ============================================
// PITCH HOOKS
// ============================================

/**
 * Hook to fetch all pitches
 */
export function usePitches(options?: Omit<UseQueryOptions<Pitch[], AxiosError>, 'queryKey' | 'queryFn'>) {
  return useQuery({
    queryKey: queryKeys.pitches.lists(),
    queryFn: async () => {
      const response = await api.get<Pitch[]>('/pitches');
      return response.data;
    },
    staleTime: STALE_TIMES.entities,
    ...options,
  });
}

/**
 * Hook to fetch pitches for a cycle
 */
export function usePitchesByCycle(
  cycleId: number | string | undefined,
  options?: Omit<UseQueryOptions<Pitch[], AxiosError>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: queryKeys.pitches.byCycle(cycleId!),
    queryFn: async () => {
      const response = await api.get<Pitch[]>(`/pitches/cycle/${cycleId}`);
      return response.data;
    },
    enabled: !!cycleId,
    staleTime: STALE_TIMES.entities,
    ...options,
  });
}

/**
 * Hook to fetch pitches for a team
 */
export function usePitchesByTeam(
  teamId: number | string | undefined,
  options?: Omit<UseQueryOptions<Pitch[], AxiosError>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: queryKeys.pitches.byTeam(teamId!),
    queryFn: async () => {
      const response = await api.get<Pitch[]>(`/pitches/team/${teamId}`);
      return response.data;
    },
    enabled: !!teamId,
    staleTime: STALE_TIMES.entities,
    ...options,
  });
}

/**
 * Hook to fetch a single pitch
 */
export function usePitch(
  id: number | string | undefined,
  options?: Omit<UseQueryOptions<Pitch, AxiosError>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: queryKeys.pitches.detail(id!),
    queryFn: async () => {
      const response = await api.get<Pitch>(`/pitches/${id}`);
      return response.data;
    },
    enabled: !!id,
    staleTime: STALE_TIMES.entities,
    ...options,
  });
}

/**
 * Hook to create a new pitch
 */
export function useCreatePitch(
  options?: UseMutationOptions<Pitch, AxiosError, CreatePitchRequest>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreatePitchRequest) => {
      const response = await api.post<Pitch>('/pitches', data);
      return response.data;
    },
    onSuccess: (newPitch) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.pitches.lists() });
      if (newPitch.cycleId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.pitches.byCycle(newPitch.cycleId) });
      }
      if (newPitch.teamId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.pitches.byTeam(newPitch.teamId) });
      }
    },
    ...options,
  });
}

/**
 * Hook to update an existing pitch
 */
export function useUpdatePitch(
  options?: UseMutationOptions<Pitch, AxiosError, { id: number; data: CreatePitchRequest }>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, data }: { id: number; data: CreatePitchRequest }) => {
      const response = await api.put<Pitch>(`/pitches/${id}`, data);
      return response.data;
    },
    onSuccess: (updatedPitch) => {
      queryClient.setQueryData(queryKeys.pitches.detail(updatedPitch.id), updatedPitch);
      queryClient.invalidateQueries({ queryKey: queryKeys.pitches.lists() });
      if (updatedPitch.cycleId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.pitches.byCycle(updatedPitch.cycleId) });
      }
    },
    ...options,
  });
}

/**
 * Hook to update pitch status
 */
export function useUpdatePitchStatus(
  options?: UseMutationOptions<Pitch, AxiosError, { id: number; status: PitchStatus }>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, status }: { id: number; status: PitchStatus }) => {
      const response = await api.patch<Pitch>(`/pitches/${id}/status?status=${status}`);
      return response.data;
    },
    onSuccess: (updatedPitch) => {
      queryClient.setQueryData(queryKeys.pitches.detail(updatedPitch.id), updatedPitch);
      queryClient.invalidateQueries({ queryKey: queryKeys.pitches.lists() });
    },
    ...options,
  });
}

/**
 * Hook to assign a team to a pitch
 */
export function useAssignTeamToPitch(
  options?: UseMutationOptions<Pitch, AxiosError, { pitchId: number; teamId: number }>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ pitchId, teamId }: { pitchId: number; teamId: number }) => {
      const response = await api.patch<Pitch>(`/pitches/${pitchId}/assign-team/${teamId}`);
      return response.data;
    },
    onSuccess: (updatedPitch) => {
      queryClient.setQueryData(queryKeys.pitches.detail(updatedPitch.id), updatedPitch);
      queryClient.invalidateQueries({ queryKey: queryKeys.pitches.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.teams.all });
    },
    ...options,
  });
}

/**
 * Hook to delete a pitch
 */
export function useDeletePitch(
  options?: UseMutationOptions<void, AxiosError, number>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/pitches/${id}`);
    },
    onSuccess: (_, deletedId) => {
      queryClient.removeQueries({ queryKey: queryKeys.pitches.detail(deletedId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.pitches.lists() });
    },
    ...options,
  });
}

// ============================================
// TEAM HOOKS
// ============================================

/**
 * Hook to fetch all teams
 */
export function useTeams(options?: Omit<UseQueryOptions<Team[], AxiosError>, 'queryKey' | 'queryFn'>) {
  return useQuery({
    queryKey: queryKeys.teams.all,
    queryFn: async () => {
      const response = await api.get<Team[]>('/teams');
      return response.data;
    },
    staleTime: STALE_TIMES.reference,
    ...options,
  });
}

/**
 * Hook to fetch a single team
 */
export function useTeam(
  id: number | string | undefined,
  options?: Omit<UseQueryOptions<Team, AxiosError>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: queryKeys.teams.detail(id!),
    queryFn: async () => {
      const response = await api.get<Team>(`/teams/${id}`);
      return response.data;
    },
    enabled: !!id,
    staleTime: STALE_TIMES.reference,
    ...options,
  });
}

/**
 * Hook to create a new team
 */
export function useCreateTeam(
  options?: UseMutationOptions<Team, AxiosError, CreateTeamRequest>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateTeamRequest) => {
      const response = await api.post<Team>('/teams', data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.teams.all });
    },
    ...options,
  });
}

/**
 * Hook to delete a team
 */
export function useDeleteTeam(
  options?: UseMutationOptions<void, AxiosError, number>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/teams/${id}`);
    },
    onSuccess: (_, deletedId) => {
      queryClient.removeQueries({ queryKey: queryKeys.teams.detail(deletedId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.teams.all });
    },
    ...options,
  });
}

// ============================================
// UTILITY HOOKS
// ============================================

/**
 * Utility hook for optimistic updates
 */
export function useOptimisticUpdate<T extends { id: number | string }>() {
  const queryClient = useQueryClient();

  const optimisticUpdate = (queryKey: readonly unknown[], newData: Partial<T> & { id: T['id'] }) => {
    queryClient.cancelQueries({ queryKey });
    const previousData = queryClient.getQueryData<T[]>(queryKey);

    queryClient.setQueryData<T[]>(queryKey, (old) => {
      if (!old) return [newData as T];
      return old.map((item) => (item.id === newData.id ? { ...item, ...newData } : item));
    });

    return { previousData };
  };

  const rollback = (queryKey: readonly unknown[], context: { previousData: T[] | undefined }) => {
    queryClient.setQueryData(queryKey, context.previousData);
  };

  return { optimisticUpdate, rollback };
}

/**
 * Hook to invalidate all queries related to a cycle
 */
export function useInvalidateCycleData() {
  const queryClient = useQueryClient();

  return (cycleId: number) => {
    queryClient.invalidateQueries({ queryKey: queryKeys.cycles.detail(cycleId) });
    queryClient.invalidateQueries({ queryKey: queryKeys.pitches.byCycle(cycleId) });
    queryClient.invalidateQueries({ queryKey: queryKeys.hillChart.byCycle(cycleId) });
  };
}

/**
 * Hook to prefetch data for a cycle detail page
 */
export function usePrefetchCycleData() {
  const queryClient = useQueryClient();

  return async (cycleId: number) => {
    await Promise.all([
      queryClient.prefetchQuery({
        queryKey: queryKeys.cycles.detail(cycleId),
        queryFn: async () => {
          const response = await api.get<Cycle>(`/cycles/${cycleId}`);
          return response.data;
        },
      }),
      queryClient.prefetchQuery({
        queryKey: queryKeys.pitches.byCycle(cycleId),
        queryFn: async () => {
          const response = await api.get<Pitch[]>(`/pitches/cycle/${cycleId}`);
          return response.data;
        },
      }),
    ]);
  };
}
