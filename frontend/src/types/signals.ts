// Frontend signal types / view models (may differ from backend DTOs)

export type TrendDirection = 'IMPROVING' | 'STABLE' | 'DECLINING' | 'INSUFFICIENT_DATA';

export type ShapingQuality = 'EXCELLENT' | 'GOOD' | 'NEEDS_ATTENTION' | 'POOR' | 'INSUFFICIENT_DATA';

export interface CycleAccuracyData {
  cycleId: number;
  cycleName: string;
  startDate: string;
  endDate: string;
  totalAppetiteHours: number;
  totalActualHours: number;
  varianceHours: number;
  variancePercent: number;
  pitchCount: number;
  overBudgetCount: number;
  underBudgetCount: number;
}

export interface AppetiteAccuracySignal {
  trend: TrendDirection;
  averageVariancePercent: number;
  varianceStdDev: number;
  cyclesAnalyzed: number;
  cycleData: CycleAccuracyData[];
  interpretation: string;
  recommendations: string[];
}

export interface ShapingPatternPitch {
  pitchId: number;
  pitchTitle: string;
  appetiteHours: number;
  actualHours: number;
  variancePercent: number;
  uncertaintyScore: number | null;
  rabbitHoleCount: number | null;
}

export interface ShapingPatternSignal {
  overallQuality: ShapingQuality;
  overShapedCount: number;
  underShapedCount: number;
  accuratelyShapedCount: number;
  totalPitchesAnalyzed: number;
  tolerancePercent: number;
  overShapedPitches: ShapingPatternPitch[];
  underShapedPitches: ShapingPatternPitch[];
  interpretation: string;
  recommendations: string[];
}

export interface RiskDetailItem {
  pitchId: number;
  pitchTitle: string;
  predictedRiskLevel: string;
  actualOutcome: string;
  explanation: string;
}

export interface RiskCorrelationSignal {
  predictiveAccuracyPercent: number;
  pitchesAnalyzed: number;
  truePositives: number;
  falseAlarms: number;
  missedRisks: number;
  trueNegatives: number;
  missedRiskDetails: RiskDetailItem[];
  falseAlarmDetails: RiskDetailItem[];
  mostPredictiveFactors: string[];
  leastPredictiveFactors: string[];
  interpretation: string;
  recommendations: string[];
}

export interface RetroActionItem {
  actionItemId: number;
  description: string;
  priority: string;
  actedOn: boolean;
  linkedPitchIds: number[];
}

export interface RetroData {
  retrospectiveId: number;
  cycleName: string;
  totalActionItems: number;
  actedOnCount: number;
}

export interface RetroFollowThroughSignal {
  followThroughRatePercent: number;
  totalActionItems: number;
  actedOnCount: number;
  pendingCount: number;
  retrospectivesAnalyzed: number;
  trend: TrendDirection;
  highPriorityUnacted: RetroActionItem[];
  recurringThemes: string[];
  retroData: RetroData[];
  interpretation: string;
  recommendations: string[];
}

export interface CycleSignals {
  cycleId: number | null;
  cycleName: string | null;
  projectId: number;
  projectName: string;
  analyzedAt: string;
  overallHealthScore: number;
  appetiteAccuracy: AppetiteAccuracySignal | null;
  shapingPattern: ShapingPatternSignal | null;
  riskCorrelation: RiskCorrelationSignal | null;
  retroFollowThrough: RetroFollowThroughSignal | null;
}
