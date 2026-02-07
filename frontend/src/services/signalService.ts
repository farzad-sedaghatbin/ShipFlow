import api from './api';
import { CycleSignals } from '../types/signals';

export const signalService = {
  async getProjectSignals(projectId: number, cycleCount: number = 6): Promise<CycleSignals> {
    const response = await api.get<CycleSignals>(`/signals/project/${projectId}`, {
      params: { cycleCount }
    });
    return response.data;
  },

  async getCycleSignals(cycleId: number): Promise<CycleSignals> {
    const response = await api.get<CycleSignals>(`/signals/cycle/${cycleId}`);
    return response.data;
  }
};

