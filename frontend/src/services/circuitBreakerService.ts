import api from './api';
import { CircuitBreaker } from '../types/circuit-breaker';

const API_BASE_URL = '/circuit-breaker';

export const circuitBreakerService = {
  /**
   * Detect pitches overflowing their time budget
   */
  detectOverflow: (cycleId: number, threshold: number = 100) => {
    return api.get<CircuitBreaker[]>(`${API_BASE_URL}/cycle/${cycleId}/overflow`, {
      params: { threshold }
    });
  },

  /**
   * Get all triggered circuit breakers in a cycle
   */
  getTriggered: (cycleId: number) => {
    return api.get<CircuitBreaker[]>(`${API_BASE_URL}/cycle/${cycleId}/triggered`);
  },

  /**
   * Manually trigger circuit breaker on a pitch
   */
  trigger: (pitchId: number, reason: string) => {
    return api.post<CircuitBreaker>(`${API_BASE_URL}/pitch/${pitchId}/trigger`, null, {
      params: { reason }
    });
  },

  /**
   * Kill a pitch due to overflow
   */
  kill: (pitchId: number, reason: string) => {
    return api.post<CircuitBreaker>(`${API_BASE_URL}/pitch/${pitchId}/kill`, null, {
      params: { reason }
    });
  },

  /**
   * Resolve/clear circuit breaker
   */
  resolve: (pitchId: number, newStatus: string) => {
    return api.post<CircuitBreaker>(`${API_BASE_URL}/pitch/${pitchId}/resolve`, null, {
      params: { newStatus }
    });
  }
};
