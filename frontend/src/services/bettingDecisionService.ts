import api from './api';

export interface BettingDecisionDTO {
  id: number;
  cycleId: number;
  cycleName: string;
  pitchId: number;
  pitchTitle: string;
  decision: 'COMMITTED' | 'REJECTED' | 'DEFERRED' | 'NEEDS_SHAPING';
  rationale?: string;
  decidedBy: {
    id: number;
    username: string;
  };
  decidedAt: string;
  commitmentLevel?: number;
  deferralReason?: string;
  deferredUntilCycleId?: number;
  deferredUntilCycleName?: string;
}

export interface RecordBettingDecisionRequest {
  cycleId: number;
  pitchId: number;
  decision: 'COMMITTED' | 'REJECTED' | 'DEFERRED' | 'NEEDS_SHAPING';
  rationale?: string;
  commitmentLevel?: number;
  deferralReason?: string;
  deferredUntilCycleId?: number;
}

export interface CycleBettingSummaryDTO {
  cycleId: number;
  cycleName: string;
  totalPitchesConsidered: number;
  committedCount: number;
  rejectedCount: number;
  deferredCount: number;
  needsShapingCount: number;
  decisions: BettingDecisionDTO[];
}

/**
 * Compute summary from a list of decisions
 */
export function computeCycleSummary(cycleId: number, cycleName: string, decisions: BettingDecisionDTO[]): CycleBettingSummaryDTO {
  return {
    cycleId,
    cycleName,
    totalPitchesConsidered: decisions.length,
    committedCount: decisions.filter(d => d.decision === 'COMMITTED').length,
    rejectedCount: decisions.filter(d => d.decision === 'REJECTED').length,
    deferredCount: decisions.filter(d => d.decision === 'DEFERRED').length,
    needsShapingCount: decisions.filter(d => d.decision === 'NEEDS_SHAPING').length,
    decisions,
  };
}

export const bettingDecisionService = {
  /**
   * Record a betting decision for a pitch in a cycle
   */
  recordDecision: (request: RecordBettingDecisionRequest) =>
    api.post<BettingDecisionDTO>('/betting/decisions', request),

  /**
   * Get a specific betting decision by ID
   */
  getDecision: (id: number) =>
    api.get<BettingDecisionDTO>(`/betting/decisions/${id}`),

  /**
   * Get all betting decisions for a cycle
   */
  getDecisionsByCycle: (cycleId: number) =>
    api.get<BettingDecisionDTO[]>(`/betting/decisions/cycle/${cycleId}`),

  /**
   * Get comprehensive betting history for a cycle (includes statistics)
   */
  getCycleHistory: (cycleId: number) =>
    api.get<any>(`/betting/decisions/cycle/${cycleId}/history`),

  /**
   * Get the decision history for a specific pitch
   */
  getDecisionsByPitch: (pitchId: number) =>
    api.get<BettingDecisionDTO[]>(`/betting/decisions/pitch/${pitchId}/history`),

  /**
   * Get all committed pitches for a cycle
   */
  getCommittedPitches: (cycleId: number) =>
    api.get<BettingDecisionDTO[]>(`/betting/decisions/cycle/${cycleId}/committed`),

  /**
   * Get rejected pitches for a cycle
   */
  getRejectedPitches: (cycleId: number) =>
    api.get<BettingDecisionDTO[]>(`/betting/decisions/cycle/${cycleId}/rejected`),
};
