import api from './api';
import { RoadmapTimeline } from '../types';

export const roadmapService = {
  getTimeline: (projectId: number, startDate: string, endDate: string) =>
    api.get<RoadmapTimeline>(`/roadmap/project/${projectId}/timeline`, {
      params: { startDate, endDate },
    }),
  
  getQuarterly: (projectId: number, year: number, quarter: number) =>
    api.get<RoadmapTimeline>(`/roadmap/project/${projectId}/quarterly`, {
      params: { year, quarter },
    }),
  
  getYearly: (projectId: number, year: number) =>
    api.get<RoadmapTimeline>(`/roadmap/project/${projectId}/yearly`, {
      params: { year },
    }),
};
