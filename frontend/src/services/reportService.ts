import api from './api';
import { CycleReport, EnhancedCycleReport, PitchReport, MemberWorkReport } from '../types';

export const reportService = {
  getCycleReport: (cycleId: number) => api.get<CycleReport>(`/reports/cycle/${cycleId}`),
  getEnhancedCycleReport: (cycleId: number) => api.get<EnhancedCycleReport>(`/reports/cycle/${cycleId}/enhanced`),
  getPitchReports: (cycleId: number) => api.get<PitchReport[]>(`/reports/cycle/${cycleId}/pitches`),
  getMemberReports: (cycleId: number) => api.get<MemberWorkReport[]>(`/reports/cycle/${cycleId}/members`),
  exportCsv: (cycleId: number) => api.get<string>(`/reports/cycle/${cycleId}/export`, {
    responseType: 'blob',
  }),
  exportPdf: (cycleId: number) => api.get<Blob>(`/reports/cycle/${cycleId}/export/pdf`, {
    responseType: 'blob',
  }),
};
