import api from './api';

export interface ShippedCycle {
  cycleName: string;
  endDate: string;
  pitchCount: number;
  summary: string;
}

export interface ProjectSnapshot {
  projectId: number;
  projectName: string;
  projectType: string;
  computedAt: string;
  cyclesCompleted: number;
  avgPitchesPerCycle: number;
  avgCompletionRate: number;
  typicalAppetiteDays: number;
  recentShipped: ShippedCycle[];
  activeCycleName: string | null;
  activeCycleDaysRemaining: number | null;
  activeCyclePitchCount: number | null;
  activeCycleCompletionPercent: number | null;
  roadmapThemes: string[];
  knownPatterns: string[];
  riskPosture: string;
}

export const projectSnapshotService = {
  getSnapshot: (projectId: number) =>
    api.get<ProjectSnapshot>(`/ai/projects/${projectId}/snapshot`).then(r => r.data),
  refreshSnapshot: (projectId: number) =>
    api.post<ProjectSnapshot>(`/ai/projects/${projectId}/snapshot/refresh`).then(r => r.data),
};
