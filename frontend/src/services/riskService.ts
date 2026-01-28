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

export interface PitchRiskDTO {
  pitchId: number;
  pitchTitle: string;
  riskScore: number;
  riskLevel: RiskLevel;
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
   */
  getPitchRisk: (pitchId: number) => api.get<PitchRiskDTO>(`/risk/pitch/${pitchId}`),

  /**
   * Get risk analysis for a specific pitch with AI (slower)
   */
  getPitchRiskWithAI: (pitchId: number) => api.get<PitchRiskDTO>(`/risk/pitch/${pitchId}/ai`),

  /**
   * Get risk overview for an entire cycle (fast - rule-based)
   */
  getCycleRiskOverview: (cycleId: number) => api.get<CycleRiskOverviewDTO>(`/risk/cycle/${cycleId}`),

  /**
   * Get risk overview for an entire cycle with AI (slower)
   */
  getCycleRiskOverviewWithAI: (cycleId: number) => api.get<CycleRiskOverviewDTO>(`/risk/cycle/${cycleId}/ai`),

  /**
   * Force refresh risk analysis for a pitch (always uses AI)
   */
  refreshPitchRisk: (pitchId: number) => api.post<PitchRiskDTO>(`/risk/pitch/${pitchId}/refresh`),

  /**
   * Force refresh risk overview for a cycle (always uses AI)
   */
  refreshCycleRisk: (cycleId: number) => api.post<CycleRiskOverviewDTO>(`/risk/cycle/${cycleId}/refresh`),

  /**
   * Ask a question to the AI Risk Advisor
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
};

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
