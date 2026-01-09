import api from './api';

// Types for Risk Feedback
export type FeedbackRating =
  | 'ACCURATE'
  | 'PARTIALLY_ACCURATE'
  | 'INACCURATE'
  | 'OVERLY_PESSIMISTIC'
  | 'OVERLY_OPTIMISTIC';

export interface RiskFeedbackDTO {
  id: number;
  pitchId: number;
  pitchTitle: string;
  userId: number;
  userName: string;
  originalRiskScore: number;
  rating: FeedbackRating;
  suggestedRiskScore?: number;
  notes?: string;
  missedFactors?: string;
  createdAt: string;
}

export interface CreateRiskFeedbackRequest {
  pitchId: number;
  originalRiskScore: number;
  rating: FeedbackRating;
  suggestedRiskScore?: number;
  notes?: string;
  missedFactors?: string;
}

export interface RiskFeedbackStatsDTO {
  totalFeedback: number;
  accurateCount: number;
  partiallyAccurateCount: number;
  inaccurateCount: number;
  overlyPessimisticCount: number;
  overlyOptimisticCount: number;
  accuracyPercent: number;
  averageScoreDelta: number;
  commonMissedFactors: string[];
  feedbackByMonth: Record<string, number>;
}

// Risk Feedback Service
export const riskFeedbackService = {
  /**
   * Submit feedback on an AI risk assessment
   */
  submitFeedback: async (request: CreateRiskFeedbackRequest): Promise<RiskFeedbackDTO> => {
    const response = await api.post<RiskFeedbackDTO>('/risk/feedback', request);
    return response.data;
  },

  /**
   * Get all feedback for a pitch
   */
  getFeedbackByPitch: async (pitchId: number): Promise<RiskFeedbackDTO[]> => {
    const response = await api.get<RiskFeedbackDTO[]>(`/risk/feedback/pitch/${pitchId}`);
    return response.data;
  },

  /**
   * Get all feedback for a cycle
   */
  getFeedbackByCycle: async (cycleId: number): Promise<RiskFeedbackDTO[]> => {
    const response = await api.get<RiskFeedbackDTO[]>(`/risk/feedback/cycle/${cycleId}`);
    return response.data;
  },

  /**
   * Get feedback statistics
   */
  getFeedbackStats: async (): Promise<RiskFeedbackStatsDTO> => {
    const response = await api.get<RiskFeedbackStatsDTO>('/risk/feedback/stats');
    return response.data;
  },

  /**
   * Get recent feedback
   */
  getRecentFeedback: async (): Promise<RiskFeedbackDTO[]> => {
    const response = await api.get<RiskFeedbackDTO[]>('/risk/feedback/recent');
    return response.data;
  },

  /**
   * Check if user has submitted feedback for a pitch
   */
  hasUserSubmittedFeedback: async (pitchId: number): Promise<boolean> => {
    const response = await api.get<{ hasSubmitted: boolean }>(`/risk/feedback/pitch/${pitchId}/check`);
    return response.data.hasSubmitted;
  },
};

// Helper functions
export const getFeedbackRatingLabel = (rating: FeedbackRating): string => {
  const labels: Record<FeedbackRating, string> = {
    ACCURATE: '✅ Accurate',
    PARTIALLY_ACCURATE: '⚠️ Partially Accurate',
    INACCURATE: '❌ Inaccurate',
    OVERLY_PESSIMISTIC: '📉 Overly Pessimistic',
    OVERLY_OPTIMISTIC: '📈 Overly Optimistic',
  };
  return labels[rating] || rating;
};

export const getFeedbackRatingColor = (rating: FeedbackRating): string => {
  const colors: Record<FeedbackRating, string> = {
    ACCURATE: '#4caf50',
    PARTIALLY_ACCURATE: '#ff9800',
    INACCURATE: '#f44336',
    OVERLY_PESSIMISTIC: '#2196f3',
    OVERLY_OPTIMISTIC: '#9c27b0',
  };
  return colors[rating] || '#9e9e9e';
};
