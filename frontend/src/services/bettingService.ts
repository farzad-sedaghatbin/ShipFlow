import api from './api';
import { 
  BettingTable, 
  BettingSlot, 
  CreateBettingSlotRequest, 
  Pitch,
  TeamPerformanceHistory,
  PitchComparison,
  CapacityAnalysis
} from '../types';

export const bettingService = {
  // Get the full betting table for a cycle
  getBettingTable: (cycleId: number) => 
    api.get<BettingTable>(`/betting/cycle/${cycleId}`),
  
  // Get shaped pitches available for betting
  getShapedPitches: (projectId?: number) => {
    const params = projectId ? `?projectId=${projectId}` : '';
    return api.get<Pitch[]>(`/betting/shaped-pitches${params}`);
  },
  
  // Create a new betting slot
  createSlot: (data: CreateBettingSlotRequest) => 
    api.post<BettingSlot>('/betting/slots', data),
  
  // Auto-generate slots for all teams in a cycle
  generateSlots: (cycleId: number) => 
    api.post<BettingSlot[]>(`/betting/cycle/${cycleId}/generate-slots`),
  
  // Assign a pitch to a slot (the main betting action)
  assignPitchToSlot: (slotId: number, pitchId: number) => 
    api.patch<BettingSlot>(`/betting/slots/${slotId}/assign/${pitchId}`),
  
  // Remove pitch from a slot
  removePitchFromSlot: (slotId: number) => 
    api.patch<BettingSlot>(`/betting/slots/${slotId}/unassign`),
  
  // Update slot dates
  updateSlotDates: (slotId: number, startDate: string, endDate: string) => 
    api.patch<BettingSlot>(`/betting/slots/${slotId}/dates?startDate=${startDate}&endDate=${endDate}`),
  
  // Delete a betting slot
  deleteSlot: (slotId: number) => 
    api.delete(`/betting/slots/${slotId}`),
  
  // Check if a pitch can fit in a slot
  canPitchFitInSlot: (slotId: number, pitchId: number) => 
    api.get<boolean>(`/betting/slots/${slotId}/can-fit/${pitchId}`),

  // === Betting Analytics ===

  // Get historical performance data for a team
  getTeamPerformanceHistory: (teamId: number) =>
    api.get<TeamPerformanceHistory>(`/betting/team/${teamId}/performance-history`),

  // Get pitch comparisons for a cycle with team fit analysis
  getPitchComparisons: (cycleId: number) =>
    api.get<PitchComparison[]>(`/betting/cycle/${cycleId}/pitch-comparisons`),

  // Get capacity analysis for a cycle
  getCapacityAnalysis: (cycleId: number) =>
    api.get<CapacityAnalysis>(`/betting/cycle/${cycleId}/capacity-analysis`),
};
