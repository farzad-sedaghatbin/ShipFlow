import api from './api';
import { ImportJobDTO, LinearConnectionStatus, LinearTeam } from '../types';

export const importService = {
  importCsv: async (file: File, projectName: string, format: string): Promise<ImportJobDTO> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('projectName', projectName);
    formData.append('format', format);
    const res = await api.post('/import/csv', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data;
  },
  getJob: async (id: number): Promise<ImportJobDTO> => {
    const res = await api.get(`/import/${id}`);
    return res.data;
  },
  listJobs: async (): Promise<ImportJobDTO[]> => {
    const res = await api.get('/import');
    return res.data;
  },

  // ── Linear API import ────────────────────────────────────────────────────
  getLinearStatus: async (): Promise<LinearConnectionStatus> => {
    const res = await api.get('/import/linear/status');
    return res.data;
  },
  authorizeLinear: async (baseUrl: string): Promise<{ authorizationUrl: string }> => {
    const res = await api.post('/import/linear/authorize', { baseUrl });
    return res.data;
  },
  getLinearTeams: async (): Promise<LinearTeam[]> => {
    const res = await api.get('/import/linear/teams');
    return res.data;
  },
  saveLinearTeam: async (teamId: string, teamName: string): Promise<void> => {
    await api.post('/import/linear/team', { teamId, teamName });
  },
  disconnectLinear: async (): Promise<void> => {
    await api.delete('/import/linear/disconnect');
  },
  importFromLinear: async (
    teamId: string,
    projectName: string,
    projectType: string
  ): Promise<ImportJobDTO> => {
    const res = await api.post('/import/linear', { teamId, projectName, projectType });
    return res.data;
  },
};
