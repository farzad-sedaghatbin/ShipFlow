import api from './api';

// Types for Pitch Health
export interface PitchHealthDTO {
  pitchId: number;
  pitchName: string;
  projectName?: string;
  projectKey?: string;
  cycleName: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  riskColor: string;
  appetiteUsedPercent: number;
  daysLeft: number;
  statusSummary: string;
  status: string;
  teamName: string;
  appetiteHours: number;
  actualHours: number;
  qaStatus: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  cycleEndDate: string;
}

export interface CycleHealthSummaryDTO {
  cycleId: number;
  cycleName: string;
  projectName?: string;
  projectKey?: string;
  overallHealth: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  healthColor: string;
  startDate: string;
  endDate: string;
  daysLeft: number;
  cycleProgressPercent: number;
  totalPitches: number;
  healthyPitches: number;
  atRiskPitches: number;
  criticalPitches: number;
  totalAppetiteHours: number;
  totalActualHours: number;
  budgetUsedPercent: number;
  pendingCount: number;
  inProgressCount: number;
  testingCount: number;
  doneCount: number;
  pitchHealthList: PitchHealthDTO[];
}

// Pitch Health Service
// Default endpoints use fast rule-based analysis
// Use *WithAI methods for AI-enhanced analysis (slower)
export const pitchHealthService = {
  /**
   * Get health summary for a single pitch (fast - rule-based)
   */
  getPitchHealth: async (pitchId: number): Promise<PitchHealthDTO> => {
    const response = await api.get<PitchHealthDTO>(`/health/pitch/${pitchId}`);
    return response.data;
  },

  /**
   * Get health summary for a single pitch with AI analysis (slower)
   */
  getPitchHealthWithAI: async (pitchId: number): Promise<PitchHealthDTO> => {
    const response = await api.get<PitchHealthDTO>(`/health/pitch/${pitchId}/ai`);
    return response.data;
  },

  /**
   * Get health summary for a cycle (fast - rule-based)
   */
  getCycleHealth: async (cycleId: number): Promise<CycleHealthSummaryDTO> => {
    const response = await api.get<CycleHealthSummaryDTO>(`/health/cycle/${cycleId}`);
    return response.data;
  },

  /**
   * Get health summary for a cycle with AI analysis (slower)
   */
  getCycleHealthWithAI: async (cycleId: number): Promise<CycleHealthSummaryDTO> => {
    const response = await api.get<CycleHealthSummaryDTO>(`/health/cycle/${cycleId}/ai`);
    return response.data;
  },

  /**
   * Get health summaries for all active cycles (fast - rule-based)
   */
  getAllActiveCycleHealth: async (): Promise<CycleHealthSummaryDTO[]> => {
    const response = await api.get<CycleHealthSummaryDTO[]>('/health/active-cycles');
    return response.data;
  },

  /**
   * Get health summaries for all active cycles with AI analysis (slower)
   */
  getAllActiveCycleHealthWithAI: async (): Promise<CycleHealthSummaryDTO[]> => {
    const response = await api.get<CycleHealthSummaryDTO[]>('/health/active-cycles/ai');
    return response.data;
  },
};

// Helper functions
export const getHealthColor = (level: string): string => {
  switch (level) {
    case 'LOW':
      return '#4caf50';
    case 'MEDIUM':
      return '#ff9800';
    case 'HIGH':
      return '#f44336';
    case 'CRITICAL':
      return '#d32f2f';
    default:
      return '#9e9e9e';
  }
};

export const getHealthLabel = (level: string): string => {
  switch (level) {
    case 'LOW':
      return 'Healthy';
    case 'MEDIUM':
      return 'At Risk';
    case 'HIGH':
      return 'High Risk';
    case 'CRITICAL':
      return 'Critical';
    default:
      return 'Unknown';
  }
};

export const getQAStatusLabel = (status: string): string => {
  switch (status) {
    case 'NOT_STARTED':
      return 'QA Not Started';
    case 'IN_PROGRESS':
      return 'QA In Progress';
    case 'COMPLETED':
      return 'QA Complete';
    default:
      return status;
  }
};
