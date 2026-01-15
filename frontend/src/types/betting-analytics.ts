// === Betting Analytics Types ===

export type PerformanceTrend = 'IMPROVING' | 'STABLE' | 'DECLINING';
export type PerformanceRating = 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR';
export type ComplexityLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type BusinessValue = 'HIGH' | 'MEDIUM' | 'LOW';
export type Urgency = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type CapacityWarningSeverity = 'CRITICAL' | 'WARNING' | 'INFO';

export interface TeamPerformanceHistory {
  teamId: number;
  teamName: string;
  totalCycles: number;
  completedBets: number;
  totalBets: number;
  lastCycleCompletionRate: number;
  overallCompletionRate: number;
  trend: PerformanceTrend;
  performanceRating: PerformanceRating;
}

export interface TeamFitAnalysis {
  teamId: number;
  teamName: string;
  availableWeeks: number;
  requiredWeeks: number;
  utilizationPercent: number;
  canFit: boolean;
  fitScore: number;
  capacityWarning?: string;
}

export interface PitchComparison {
  pitchId: number;
  pitchTitle: string;
  appetiteDays: number;
  appetiteWeeks: number;
  complexityLevel: ComplexityLevel | null;
  risks: string[];
  rabbitHoles: string[];
  teamFitScores: TeamFitAnalysis[];
  estimatedBusinessValue: BusinessValue | null;
  urgency: Urgency | null;
}

export interface CapacityWarning {
  severity: CapacityWarningSeverity;
  teamId: number;
  teamName: string;
  message: string;
  utilizationRate: number;
}

export interface CapacityAnalysis {
  cycleId: number;
  cycleName: string;
  totalCapacityWeeks: number;
  allocatedWeeks: number;
  remainingWeeks: number;
  utilizationRate: number;
  warnings: CapacityWarning[];
  recommendations: string[];
}

export interface PitchAssignment {
  pitchId: number;
  teamId: number;
  pitchTitle: string;
  teamName: string;
}

export interface BettingScenario {
  name: string;
  assignments: PitchAssignment[];
  totalUtilization: number;
  viabilityScore: number;
  pros: string[];
  cons: string[];
}
