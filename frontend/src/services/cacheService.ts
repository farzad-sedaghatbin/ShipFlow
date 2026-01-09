import api from './api';

// Types for cache management
export interface CacheStats {
  totalEntries: number;
  pitchRiskEntries: number;
  cycleRiskEntries: number;
  qaEntries: number;
  totalHits: number;
  provider: string;  // 'in-memory' or 'redis'
  riskCacheEnabled: boolean;
  qaCacheEnabled: boolean;
}

export interface CacheConfig {
  enabled: boolean;
  provider: string;
  redis?: {
    host: string;
    port: number;
    database: number;
  };
  risk: {
    enabled: boolean;
    pitchTtlMinutes: number;  // 0 = infinite
    cycleTtlMinutes: number;  // 0 = infinite
    invalidateOnDataChange: boolean;
  };
  qa: {
    enabled: boolean;
    ttlHours: number;  // 0 = infinite
    similarityThreshold: number;
    maxEntriesPerContext: number;
  };
}

export const cacheService = {
  /**
   * Get current cache statistics
   */
  getStats: () => api.get<CacheStats>('/cache/stats'),

  /**
   * Get current cache configuration
   */
  getConfig: () => api.get<CacheConfig>('/cache/config'),

  /**
   * Clear all AI caches
   */
  clearAll: () => api.post<{ message: string }>('/cache/clear'),

  /**
   * Clear only risk analysis cache
   */
  clearRiskCache: () => api.post<{ message: string }>('/cache/clear/risk'),

  /**
   * Clear only Q&A cache
   */
  clearQACache: () => api.post<{ message: string }>('/cache/clear/qa'),

  /**
   * Invalidate cache for a specific pitch
   */
  invalidatePitchCache: (pitchId: number) => 
    api.delete<{ message: string }>(`/cache/risk/pitch/${pitchId}`),

  /**
   * Invalidate cache for a specific cycle
   */
  invalidateCycleCache: (cycleId: number) => 
    api.delete<{ message: string }>(`/cache/risk/cycle/${cycleId}`),

  /**
   * Invalidate Q&A cache for a specific context
   */
  invalidateQACache: (contextType: string, contextId: number) => 
    api.delete<{ message: string }>(`/cache/qa/${contextType}/${contextId}`),
};

export default cacheService;
