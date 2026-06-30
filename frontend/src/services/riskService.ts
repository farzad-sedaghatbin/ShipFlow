import api from './api';

// Types for risk analysis
export interface RiskFactor {
  category: 
    | 'SCOPE_CREEP'
    | 'TIME_OVERRUN'
    | 'RESOURCE_CONSTRAINT'
    | 'TECHNICAL_COMPLEXITY'
    | 'DEPENDENCY_RISK'
    | 'TEAM_CAPACITY'
    | 'UNCLEAR_REQUIREMENTS'
    | 'PROGRESS_STAGNATION'
    | 'APPETITE_MISMATCH'
    | 'OTHER';
  description: string;
  impactLevel: number;
  probability: number;
}

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface RiskBand {
  level: RiskLevel;
  minScore: number;
  maxScore: number;
  active: boolean;
}

export interface FactorContribution {
  category: RiskFactor['category'];
  description: string;
  impactLevel: number;
  probability: number;
  weightedPoints: number;
}

export interface RiskScoreExplanation {
  score: number;
  activeBand: RiskLevel;
  bands: RiskBand[];
  factorContributions: FactorContribution[];
}

export interface PitchRiskDTO {
  pitchId: number;
  pitchTitle: string;
  riskScore: number;
  riskLevel: RiskLevel;
  explanation?: RiskScoreExplanation;
  riskFactors: RiskFactor[];
  insights: string[];
  recommendations: string[];
  confidenceScore: number;
  analyzedAt: string;
  aiEnabled: boolean;
  errorMessage?: string;
}

export interface CycleRiskStats {
  averageRiskScore: number;
  maxRiskScore: number;
  minRiskScore: number;
  averageProgress: number;
  appetiteUtilization: number;
}

export interface PitchRiskSummary {
  pitchId: number;
  pitchTitle: string;
  riskScore: number;
  riskLevel: RiskLevel;
  topRisk: string;
  progressPercentage?: number;
}

export interface CommonRiskFactor {
  category: RiskFactor['category'];
  occurrenceCount: number;
  averageImpact: number;
}

export interface CycleRiskOverviewDTO {
  cycleId: number;
  cycleName: string;
  projectName?: string;
  overallRiskScore: number;
  overallRiskLevel: RiskLevel;
  totalPitches: number;
  highRiskPitches: number;
  mediumRiskPitches: number;
  lowRiskPitches: number;
  stats: CycleRiskStats;
  pitchRisks: PitchRiskSummary[];
  cycleInsights: string[];
  cycleRecommendations: string[];
  topRiskFactors: CommonRiskFactor[];
  analyzedAt: string;
  aiEnabled: boolean;
}

export interface AIStatus {
  enabled: boolean;
  message: string;
}

export interface RiskQuestionRequest {
  question: string;
}

export interface RiskQuestionResponse {
  pitchId: number;
  pitchTitle: string;
  question: string;
  answer: string | null;
  confidenceScore: number;
  aiEnabled: boolean;
  answeredAt: string;
  errorMessage?: string;
}

// Risk Analysis Service
// Default endpoints use fast rule-based analysis
// Use *WithAI methods for AI-enhanced analysis (slower)
export const riskService = {
  /**
   * Check if AI risk analysis is enabled and available
   */
  getAIStatus: () => api.get<AIStatus>('/risk/status'),

  /**
   * Get risk analysis for a specific pitch (fast - rule-based)
   * For AI-powered analysis, use startAsyncPitchRiskAnalysis() instead
   */
  getPitchRisk: (pitchId: number) => api.get<PitchRiskDTO>(`/risk/pitch/${pitchId}`),

  /**
   * Get risk overview for an entire cycle (fast - rule-based)
   * For AI-powered analysis, use startAsyncCycleRiskAnalysis() instead
   */
  getCycleRiskOverview: (cycleId: number) => api.get<CycleRiskOverviewDTO>(`/risk/cycle/${cycleId}`),

  /**
   * Ask a question to the AI Risk Advisor (sync - for backward compatibility)
   * Consider using startAsyncQuestion() for non-blocking calls
   */
  askQuestion: (pitchId: number, question: string) => 
    api.post<RiskQuestionResponse>(`/risk/pitch/${pitchId}/ask`, { question }),

  /**
   * Get risk history for a pitch
   */
  getRiskHistory: async (pitchId: number, days: number = 30): Promise<PitchRiskHistory[]> => {
    const response = await api.get<PitchRiskHistory[]>(`/risk/pitch/${pitchId}/history?days=${days}`);
    return response.data;
  },

  /**
   * Analyze pitch risk (returns full PitchRiskDTO with factors)
   */
  analyzePitchRisk: async (pitchId: number): Promise<PitchRiskDTO> => {
    const response = await api.get<PitchRiskDTO>(`/risk/pitch/${pitchId}`);
    return response.data;
  },

  // ===========================================
  // ASYNC AI ADVISOR (Non-blocking)
  // ===========================================

  /**
   * Start async pitch risk analysis with AI
   * Returns immediately with a jobId for polling
   */
  startAsyncPitchRiskAnalysis: (pitchId: number) =>
    api.post<AsyncJobResponse>(`/risk/async/pitch/${pitchId}/analyze`),

  /**
   * Start async cycle risk analysis with AI
   * Returns immediately with a jobId for polling
   */
  startAsyncCycleRiskAnalysis: (cycleId: number) =>
    api.post<AsyncJobResponse>(`/risk/async/cycle/${cycleId}/analyze`),

  /**
   * Start async Q&A question
   * Returns immediately with a jobId for polling
   */
  startAsyncQuestion: (pitchId: number, question: string) =>
    api.post<AsyncJobResponse>(`/risk/async/pitch/${pitchId}/ask`, { question }),

  /**
   * Get job status (for polling)
   */
  getJobStatus: (jobId: string) =>
    api.get<JobStatusResponse>(`/risk/async/jobs/${jobId}/status`),

  /**
   * Get pitch risk result from completed job
   */
  getAsyncPitchRiskResult: (jobId: string) =>
    api.get<PitchRiskDTO>(`/risk/async/jobs/${jobId}/pitch-risk`),

  /**
   * Get cycle risk result from completed job
   */
  getAsyncCycleRiskResult: (jobId: string) =>
    api.get<CycleRiskOverviewDTO>(`/risk/async/jobs/${jobId}/cycle-risk`),

  /**
   * Get Q&A result from completed job
   */
  getAsyncQAResult: (jobId: string) =>
    api.get<RiskQuestionResponse>(`/risk/async/jobs/${jobId}/qa`),

  /**
   * Cancel an async job
   */
  cancelAsyncJob: (jobId: string) =>
    api.delete<{ jobId: string; cancelled: boolean; message: string }>(`/risk/async/jobs/${jobId}`),
};

// ===========================================
// ASYNC JOB TYPES
// ===========================================

export type JobStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface AsyncJobResponse {
  cached?: boolean;
  jobId?: string;
  status?: string;
  message?: string;
  result?: PitchRiskDTO | CycleRiskOverviewDTO;
}

export interface JobStatusResponse {
  jobId: string;
  status: JobStatus;
  jobType: 'pitch_risk' | 'cycle_risk' | 'qa';
  contextId: number;
  createdAt: string;
  completedAt?: string;
  errorMessage?: string;
  hasResult: boolean;
}

// ===========================================
// ASYNC POLLING UTILITY
// ===========================================

/**
 * Poll for async AI job completion with exponential backoff
 * 
 * @param jobId - The job ID to poll
 * @param maxAttempts - Maximum polling attempts (default: 30 = ~2 minutes)
 * @param initialDelayMs - Initial delay in ms (default: 1000)
 * @param onStatusChange - Callback when status changes (for UI updates)
 * @returns Final job status when completed or failed
 */
export async function pollJobStatus(
  jobId: string,
  maxAttempts = 30,
  initialDelayMs = 1000,
  onStatusChange?: (status: JobStatusResponse) => void
): Promise<JobStatusResponse> {
  let delay = initialDelayMs;
  let attempts = 0;
  
  while (attempts < maxAttempts) {
    try {
      const response = await riskService.getJobStatus(jobId);
      const status = response.data;
      
      onStatusChange?.(status);
      
      if (status.status === 'COMPLETED' || status.status === 'FAILED') {
        return status;
      }
      
      // Wait before next poll (exponential backoff, max 5 seconds)
      await new Promise(resolve => setTimeout(resolve, delay));
      delay = Math.min(delay * 1.5, 5000);
      attempts++;
    } catch (error) {
      console.error('Error polling job status:', error);
      attempts++;
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
  
  // Timeout - return a failed status
  return {
    jobId,
    status: 'FAILED',
    jobType: 'pitch_risk',
    contextId: 0,
    createdAt: new Date().toISOString(),
    errorMessage: 'Polling timeout - job took too long',
    hasResult: false,
  };
}

/**
 * Convenience function: Start async pitch risk analysis and poll until complete
 * CACHE-FIRST: Returns immediately if result is cached (no polling needed)
 */
export async function fetchPitchRiskAsync(
  pitchId: number,
  onStatusChange?: (status: JobStatusResponse) => void
): Promise<PitchRiskDTO | null> {
  try {
    // Start the async job (or get cached result)
    const startResponse = await riskService.startAsyncPitchRiskAnalysis(pitchId);
    const data = startResponse.data as AsyncJobResponse;
    
    // CACHE HIT: Return immediately
    if (data.cached && data.result) {
      onStatusChange?.({
        jobId: 'cached',
        status: 'COMPLETED',
        jobType: 'pitch_risk',
        contextId: pitchId,
        createdAt: new Date().toISOString(),
        completedAt: new Date().toISOString(),
        hasResult: true,
      });
      return data.result as PitchRiskDTO;
    }
    
    // CACHE MISS: Poll for result
    const jobId = data.jobId!;
    const finalStatus = await pollJobStatus(jobId, 30, 1000, onStatusChange);
    
    if (finalStatus.status === 'COMPLETED' && finalStatus.hasResult) {
      const result = await riskService.getAsyncPitchRiskResult(jobId);
      return result.data;
    }
    
    console.error('Async pitch risk analysis failed:', finalStatus.errorMessage);
    return null;
  } catch (error) {
    console.error('Error in async pitch risk analysis:', error);
    return null;
  }
}

/**
 * Convenience function: Start async cycle risk analysis and poll until complete
 * CACHE-FIRST: Returns immediately if result is cached (no polling needed)
 */
export async function fetchCycleRiskAsync(
  cycleId: number,
  onStatusChange?: (status: JobStatusResponse) => void
): Promise<CycleRiskOverviewDTO | null> {
  try {
    // Start the async job (or get cached result)
    const startResponse = await riskService.startAsyncCycleRiskAnalysis(cycleId);
    const data = startResponse.data as AsyncJobResponse;
    
    // CACHE HIT: Return immediately
    if (data.cached && data.result) {
      onStatusChange?.({
        jobId: 'cached',
        status: 'COMPLETED',
        jobType: 'cycle_risk',
        contextId: cycleId,
        createdAt: new Date().toISOString(),
        completedAt: new Date().toISOString(),
        hasResult: true,
      });
      return data.result as CycleRiskOverviewDTO;
    }
    
    // CACHE MISS: Poll for result
    const jobId = data.jobId!;
    const finalStatus = await pollJobStatus(jobId, 30, 1000, onStatusChange);
    
    if (finalStatus.status === 'COMPLETED' && finalStatus.hasResult) {
      const result = await riskService.getAsyncCycleRiskResult(jobId);
      return result.data;
    }
    
    console.error('Async cycle risk analysis failed:', finalStatus.errorMessage);
    return null;
  } catch (error) {
    console.error('Error in async cycle risk analysis:', error);
    return null;
  }
}

/**
 * Convenience function: Start async Q&A and poll until complete
 */
export async function askQuestionAsync(
  pitchId: number,
  question: string,
  onStatusChange?: (status: JobStatusResponse) => void
): Promise<RiskQuestionResponse | null> {
  try {
    // Start the async job (Q&A is not cached, always goes async)
    const startResponse = await riskService.startAsyncQuestion(pitchId, question);
    const jobId = startResponse.data.jobId!; // Q&A always returns jobId
    
    // Poll until complete
    const finalStatus = await pollJobStatus(jobId, 60, 1000, onStatusChange); // Q&A may take longer
    
    if (finalStatus.status === 'COMPLETED' && finalStatus.hasResult) {
      const result = await riskService.getAsyncQAResult(jobId);
      return result.data;
    }
    
    console.error('Async Q&A failed:', finalStatus.errorMessage);
    return null;
  } catch (error) {
    console.error('Error in async Q&A:', error);
    return null;
  }
}

// Helper functions for risk display
export const getRiskLevelColor = (level: RiskLevel): 'success' | 'warning' | 'error' | 'info' => {
  switch (level) {
    case 'LOW':
      return 'success';
    case 'MEDIUM':
      return 'warning';
    case 'HIGH':
      return 'error';
    case 'CRITICAL':
      return 'error';
    default:
      return 'info';
  }
};

export const getRiskScoreColor = (score: number): string => {
  if (score >= 80) return '#d32f2f'; // Critical - red
  if (score >= 60) return '#f57c00'; // High - orange
  if (score >= 30) return '#fbc02d'; // Medium - yellow
  return '#388e3c'; // Low - green
};

export const formatRiskCategory = (category: RiskFactor['category']): string => {
  const labels: Record<RiskFactor['category'], string> = {
    SCOPE_CREEP: 'Scope Creep',
    TIME_OVERRUN: 'Time Overrun',
    RESOURCE_CONSTRAINT: 'Resource Constraint',
    TECHNICAL_COMPLEXITY: 'Technical Complexity',
    DEPENDENCY_RISK: 'Dependency Risk',
    TEAM_CAPACITY: 'Team Capacity',
    UNCLEAR_REQUIREMENTS: 'Unclear Requirements',
    PROGRESS_STAGNATION: 'Progress Stagnation',
    APPETITE_MISMATCH: 'Appetite Mismatch',
    OTHER: 'Other',
  };
  return labels[category] || category;
};

// Risk History Types
export interface PitchRiskHistory {
  id: number;
  pitchId: number;
  riskScore: number;
  riskLevel: RiskLevel;
  riskFactorsJson: string;
  recordedAt: string;
  triggerType: 'MANUAL' | 'SCHEDULED' | 'STATUS_CHANGE' | 'WORK_LOG_ADDED' | 'CIRCUIT_BREAKER';
}

export default riskService;
