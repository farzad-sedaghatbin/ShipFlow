import api from './api';

export interface CycleNarrative {
  id: number;
  cycleId: number;
  narrativeType: 'WHAT_WE_BET' | 'WHAT_SHIPPED' | 'WHAT_WE_CUT' | 'SURPRISES' | 'FULL_SUMMARY';
  content: string;
  isAiGenerated: boolean;
  generatedAt: string;
}

export interface CycleSummary {
  cycleId: number;
  cycleName: string;
  projectName: string;
  startDate: string;
  endDate: string;
  whatWeBet: CycleNarrative | null;
  whatShipped: CycleNarrative | null;
  whatWeCut: CycleNarrative | null;
  surprises: CycleNarrative | null;
  fullSummary: CycleNarrative | null;
  generatedAt: string;
}

export const narrativeService = {
  async getCycleSummary(cycleId: number): Promise<CycleSummary> {
    const response = await api.get<CycleSummary>(`/narratives/cycle/${cycleId}/summary`);
    return response.data;
  },

  async getNarrative(cycleId: number, type: CycleNarrative['narrativeType']): Promise<CycleNarrative> {
    const response = await api.get<CycleNarrative>(`/narratives/cycle/${cycleId}/${type}`);
    return response.data;
  },

  async regenerateNarratives(cycleId: number): Promise<CycleSummary> {
    const response = await api.post<CycleSummary>(`/narratives/cycle/${cycleId}/regenerate`);
    return response.data;
  },

  async regenerateNarrative(cycleId: number, type: CycleNarrative['narrativeType']): Promise<CycleNarrative> {
    const response = await api.post<CycleNarrative>(`/narratives/cycle/${cycleId}/${type}/regenerate`);
    return response.data;
  },

  async exportAsMarkdown(cycleId: number): Promise<string> {
    const response = await api.get<string>(`/narratives/cycle/${cycleId}/export/markdown`, {
      responseType: 'text'
    });
    return response.data;
  }
};
