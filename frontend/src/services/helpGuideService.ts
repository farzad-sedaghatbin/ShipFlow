import api from './api';

/**
 * Dedicated frontend service for the Help Guide AI search.
 * Calls the separate /api/help/ai/* endpoints,
 * completely decoupled from the business-logic qaService.
 */

export interface HelpAIResponse {
  question: string;
  answer?: string;
  aiEnabled?: boolean;
  processingTimeMs?: number;
  errorMessage?: string;
  suggestedFollowUps?: string[];
}

export const helpGuideService = {
  /** Check if Help AI is available */
  getStatus: () => api.get<{ available: boolean }>('/help/ai/status'),

  /** Ask a ShipFlow how-to question */
  ask: (question: string) =>
    api.post<HelpAIResponse>('/help/ai/ask', { question }),
};
