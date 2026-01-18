import { QueryClient, QueryClientProvider, QueryCache, MutationCache } from '@tanstack/react-query';

// Create a custom QueryClient with default options
export const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error, query) => {
      // Global error handling for queries
      if (process.env.NODE_ENV === 'development') {
        console.error(`Query error for ${query.queryKey}:`, error);
      }
    },
  }),
  mutationCache: new MutationCache({
    onError: (error, _variables, _context, mutation) => {
      // Global error handling for mutations
      if (process.env.NODE_ENV === 'development') {
        console.error(`Mutation error for ${mutation.options.mutationKey}:`, error);
      }
    },
  }),
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      gcTime: 1000 * 60 * 30, // 30 minutes (formerly cacheTime)
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        // Don't retry on 4xx errors
        if (error && typeof error === 'object' && 'response' in error) {
          const status = (error as { response?: { status?: number } }).response?.status;
          if (status && status >= 400 && status < 500) {
            return false;
          }
        }
        return failureCount < 3;
      },
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
    },
    mutations: {
      retry: false,
    },
  },
});

// Query key factory for consistent key management
export const queryKeys = {
  // Projects
  projects: {
    all: ['projects'] as const,
    lists: () => [...queryKeys.projects.all, 'list'] as const,
    list: (filters: Record<string, unknown>) => [...queryKeys.projects.lists(), filters] as const,
    details: () => [...queryKeys.projects.all, 'detail'] as const,
    detail: (id: number | string) => [...queryKeys.projects.details(), id] as const,
    active: () => [...queryKeys.projects.all, 'active'] as const,
  },
  
  // Cycles
  cycles: {
    all: ['cycles'] as const,
    lists: () => [...queryKeys.cycles.all, 'list'] as const,
    list: (filters: Record<string, unknown>) => [...queryKeys.cycles.lists(), filters] as const,
    details: () => [...queryKeys.cycles.all, 'detail'] as const,
    detail: (id: number | string) => [...queryKeys.cycles.details(), id] as const,
    byProject: (projectId: number | string) => [...queryKeys.cycles.all, 'project', projectId] as const,
    active: () => [...queryKeys.cycles.all, 'active'] as const,
  },
  
  // Pitches
  pitches: {
    all: ['pitches'] as const,
    lists: () => [...queryKeys.pitches.all, 'list'] as const,
    list: (filters: Record<string, unknown>) => [...queryKeys.pitches.lists(), filters] as const,
    details: () => [...queryKeys.pitches.all, 'detail'] as const,
    detail: (id: number | string) => [...queryKeys.pitches.details(), id] as const,
    byCycle: (cycleId: number | string) => [...queryKeys.pitches.all, 'cycle', cycleId] as const,
    byTeam: (teamId: number | string) => [...queryKeys.pitches.all, 'team', teamId] as const,
  },
  
  // Hill Chart
  hillChart: {
    all: ['hillChart'] as const,
    byCycle: (cycleId: number | string) => [...queryKeys.hillChart.all, 'cycle', cycleId] as const,
  },
  
  // Tasks
  tasks: {
    all: ['tasks'] as const,
    lists: () => [...queryKeys.tasks.all, 'list'] as const,
    list: (filters: Record<string, unknown>) => [...queryKeys.tasks.lists(), filters] as const,
    details: () => [...queryKeys.tasks.all, 'detail'] as const,
    detail: (id: number | string) => [...queryKeys.tasks.details(), id] as const,
    byPitch: (pitchId: number | string) => [...queryKeys.tasks.all, 'pitch', pitchId] as const,
    byCycle: (cycleId: number | string) => [...queryKeys.tasks.all, 'cycle', cycleId] as const,
  },
  
  // Teams
  teams: {
    all: ['teams'] as const,
    lists: () => [...queryKeys.teams.all, 'list'] as const,
    list: (filters: Record<string, unknown>) => [...queryKeys.teams.lists(), filters] as const,
    details: () => [...queryKeys.teams.all, 'detail'] as const,
    detail: (id: number | string) => [...queryKeys.teams.details(), id] as const,
  },
  
  // People
  people: {
    all: ['people'] as const,
    lists: () => [...queryKeys.people.all, 'list'] as const,
    list: (filters: Record<string, unknown>) => [...queryKeys.people.lists(), filters] as const,
    details: () => [...queryKeys.people.all, 'detail'] as const,
    detail: (id: number | string) => [...queryKeys.people.details(), id] as const,
  },
  
  // Meetings
  meetings: {
    all: ['meetings'] as const,
    lists: () => [...queryKeys.meetings.all, 'list'] as const,
    list: (filters: Record<string, unknown>) => [...queryKeys.meetings.lists(), filters] as const,
    details: () => [...queryKeys.meetings.all, 'detail'] as const,
    detail: (id: number | string) => [...queryKeys.meetings.details(), id] as const,
  },
  
  // Reports
  reports: {
    all: ['reports'] as const,
    cycleReport: (cycleId: number | string) => [...queryKeys.reports.all, 'cycle', cycleId] as const,
    velocity: (projectId: number | string) => [...queryKeys.reports.all, 'velocity', projectId] as const,
  },
  
  // Health
  health: {
    all: ['health'] as const,
    cycle: (cycleId: number | string) => [...queryKeys.health.all, 'cycle', cycleId] as const,
    pitch: (pitchId: number | string) => [...queryKeys.health.all, 'pitch', pitchId] as const,
  },
  
  // Dashboard
  dashboard: {
    all: ['dashboard'] as const,
    widgets: () => [...queryKeys.dashboard.all, 'widgets'] as const,
    custom: (dashboardId: number | string) => [...queryKeys.dashboard.all, 'custom', dashboardId] as const,
  },
  
  // QA
  qa: {
    all: ['qa'] as const,
    testCases: () => [...queryKeys.qa.all, 'testCases'] as const,
    testRuns: () => [...queryKeys.qa.all, 'testRuns'] as const,
    bugs: () => [...queryKeys.qa.all, 'bugs'] as const,
  },
  
  // Retrospectives
  retros: {
    all: ['retros'] as const,
    lists: () => [...queryKeys.retros.all, 'list'] as const,
    detail: (id: number | string) => [...queryKeys.retros.all, 'detail', id] as const,
  },
  
  // User & Auth
  user: {
    current: ['user', 'current'] as const,
    preferences: ['user', 'preferences'] as const,
    permissions: ['user', 'permissions'] as const,
  },
  
  // Organization
  organization: {
    all: ['organization'] as const,
    settings: () => [...queryKeys.organization.all, 'settings'] as const,
    members: () => [...queryKeys.organization.all, 'members'] as const,
  },
  
  // Integrations
  integrations: {
    all: ['integrations'] as const,
    github: () => [...queryKeys.integrations.all, 'github'] as const,
    slack: () => [...queryKeys.integrations.all, 'slack'] as const,
    teams: () => [...queryKeys.integrations.all, 'teams'] as const,
  },
};

// Re-export QueryClientProvider for convenience
export { QueryClientProvider };
