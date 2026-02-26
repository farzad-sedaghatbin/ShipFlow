export { useKeyboardShortcuts } from './useKeyboardShortcuts';
export type { KeyboardShortcut } from './useKeyboardShortcuts';

export { useDebounce } from './useDebounce';

export { useReducedMotion } from './useReducedMotion';

export { useMediaQuery } from './useMediaQuery';

export { useBreakpoint, useBreakpointHelpers, BREAKPOINTS } from './useBreakpoint';
export type { Breakpoint } from './useBreakpoint';

// Current user live sync
export { useCurrentUser, useInvalidateCurrentUser } from './useCurrentUser';
export type { CurrentUser } from './useCurrentUser';

// React Query hooks
export {
  // Project hooks
  useProjects,
  useActiveProjects,
  useProject,
  useCreateProject,
  useUpdateProject,
  useDeleteProject,
  // Cycle hooks
  useCycles,
  useActiveCycles,
  useCyclesByProject,
  useActiveCyclesByProject,
  useCycle,
  useCreateCycle,
  useUpdateCycle,
  useUpdateCyclePhase,
  useDeleteCycle,
  // Pitch hooks
  usePitches,
  usePitchesByCycle,
  usePitchesByTeam,
  usePitch,
  useCreatePitch,
  useUpdatePitch,
  useUpdatePitchStatus,
  useAssignTeamToPitch,
  useDeletePitch,
  // Team hooks
  useTeams,
  useTeam,
  useCreateTeam,
  useDeleteTeam,
  // Utility hooks
  useInvalidateCycleData,
  usePrefetchCycleData,
  useOptimisticUpdate,
} from './useQueries';

// Backlog-specific hooks
export {
  backlogKeys,
  useBacklogCycles,
  useBacklogPersons,
  useActiveTimer,
  useBacklogTasks,
  useBacklogStatistics,
  useSubtasks,
  useCreateTask,
  useUpdateTask,
  useDeleteTask,
  useUpdateTaskStatus,
  useUpdateTaskPriority,
  useStartTimer,
} from './useBacklogData';
export type { TaskQueryParams, StatisticsQueryParams } from './useBacklogData';

// Backlog filters hook
export { useBacklogFilters } from './useBacklogFilters';
export type { BacklogFilterState, BacklogFilterActions, UseBacklogFiltersReturn } from './useBacklogFilters';

// Backlog form hook
export { useBacklogForm } from './useBacklogForm';
export type { TaskFormData, UseBacklogFormReturn } from './useBacklogForm';
