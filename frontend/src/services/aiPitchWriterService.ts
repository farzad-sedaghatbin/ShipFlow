import api from './api';

export interface PitchWriterRequest {
  problemDescription: string;
  appetiteHint?: number;
  projectContext?: string;
}

export interface PitchWriterResponse {
  title: string;
  problemStatement: string;
  solution: string;
  appetiteDays: number;
  rabbitHoles: string;
  noGos: string;
  risks: string;
}

export const aiPitchWriterService = {
  generate: (data: PitchWriterRequest) =>
    api.post<PitchWriterResponse>('/ai/pitch-writer/generate', data).then(r => r.data),
  getStatus: () =>
    api.get<{ available: boolean }>('/ai/pitch-writer/status').then(r => r.data),
};
