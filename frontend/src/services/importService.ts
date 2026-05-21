import api from './api';
import { ImportJobDTO } from '../types';

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
};
