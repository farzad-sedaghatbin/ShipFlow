export { useKeyboardShortcuts } from './useKeyboardShortcuts';
export type { KeyboardShortcut } from './useKeyboardShortcuts';

export { useDebounce } from './useDebounce';

export { useReducedMotion } from './useReducedMotion';

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
