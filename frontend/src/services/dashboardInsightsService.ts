import api from './api';

export interface DashboardInsight {
  type: string;
  severity: 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';
  title: string;
  detail: string;
  count?: number;
  targetId?: string;
  targetRoute?: string;
}

export interface DashboardInsightsResponse {
  insights: DashboardInsight[];
  aiSummary: string | null;
  computedAt: string;
  aiEnabled: boolean;
}

export const dashboardInsightsService = {
  getInsights: (projectId: number) =>
    api
      .get<DashboardInsightsResponse>(`/ai/dashboard/${projectId}/insights`)
      .then((r) => r.data),

  refreshInsights: (projectId: number) =>
    api
      .post<DashboardInsightsResponse>(`/ai/dashboard/${projectId}/insights/refresh`)
      .then((r) => r.data),
};
