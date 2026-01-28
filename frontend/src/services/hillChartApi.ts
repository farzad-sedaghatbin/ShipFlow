import api from './api';
import { HillChartPoint, CreateHillChartPointRequest, UpdateHillChartPointRequest } from '../types';

// Types for hill chart history
export interface UpdateHillChartPositionRequest {
  newPosition: number;
  confidenceLevel?: number;
  note?: string;
}

export interface HillChartHistoryDTO {
  id: number;
  hillChartPointId: number;
  scope: string;
  pitchId: number;
  pitchTitle: string;
  userId?: number;
  userName?: string;
  previousPosition: number;
  newPosition: number;
  confidenceLevel?: number;
  note?: string;
  actualHoursAtMove?: number;
  progressAtMove?: number;
  createdAt: string;
}

export interface ConfidenceAnalysisDTO {
  pitchId: number;
  pitchTitle: string;
  averageConfidence: number;
  actualProgress: number;
  confidenceDelta: number;
  accuracyAssessment: string;
  history: HillChartHistoryDTO[];
  insights: string[];
}

export const hillChartApi = {
  getAllHillChartPoints: async (): Promise<HillChartPoint[]> => {
    const response = await api.get<HillChartPoint[]>('/hill-chart');
    return response.data;
  },

  getHillChartPointsByPitch: async (pitchId: number): Promise<HillChartPoint[]> => {
    const response = await api.get<HillChartPoint[]>(`/hill-chart/pitch/${pitchId}`);
    return response.data;
  },

  searchHillChartPoints: async (query: string): Promise<HillChartPoint[]> => {
    const response = await api.get<HillChartPoint[]>('/hill-chart/search', {
      params: { q: query },
    });
    return response.data;
  },

  getHillChartPointById: async (id: number): Promise<HillChartPoint> => {
    const response = await api.get<HillChartPoint>(`/hill-chart/${id}`);
    return response.data;
  },

  createHillChartPoint: async (data: CreateHillChartPointRequest): Promise<HillChartPoint> => {
    const response = await api.post<HillChartPoint>('/hill-chart', data);
    return response.data;
  },

  updateHillChartPoint: async (id: number, data: UpdateHillChartPointRequest): Promise<HillChartPoint> => {
    const response = await api.put<HillChartPoint>(`/hill-chart/${id}`, data);
    return response.data;
  },

  deleteHillChartPoint: async (id: number): Promise<void> => {
    await api.delete(`/hill-chart/${id}`);
  },

  // New interactive hill chart methods with confidence tracking
  updatePositionWithHistory: async (id: number, data: UpdateHillChartPositionRequest): Promise<HillChartPoint> => {
    const response = await api.put<HillChartPoint>(`/hill-chart/${id}/position`, data);
    return response.data;
  },

  getPointHistory: async (pointId: number): Promise<HillChartHistoryDTO[]> => {
    const response = await api.get<HillChartHistoryDTO[]>(`/hill-chart/${pointId}/history`);
    return response.data;
  },

  getPitchHistory: async (pitchId: number): Promise<HillChartHistoryDTO[]> => {
    const response = await api.get<HillChartHistoryDTO[]>(`/hill-chart/pitch/${pitchId}/history`);
    return response.data;
  },

  getConfidenceAnalysis: async (pitchId: number): Promise<ConfidenceAnalysisDTO> => {
    const response = await api.get<ConfidenceAnalysisDTO>(`/hill-chart/pitch/${pitchId}/confidence`);
    return response.data;
  },
};
