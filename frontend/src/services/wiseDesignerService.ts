import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

const api = axios.create({ baseURL: `${API_BASE_URL}/api/wise-designer` });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('shipflow_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// ────────────────────────────────────────────────────────────────────────────
// Types
// ────────────────────────────────────────────────────────────────────────────

export interface WiseDesignerRequest {
  pitchId: number;
  wiseArchSessionId?: string;
  additionalContext?: string;
}

export interface WiseDesignerIterateRequest {
  artifactId: number;
  instruction: string;
}

export interface WiseDesignerResponse {
  artifactId: number;
  pitchId: number;
  pitchName: string;
  htmlContent: string;
  description: string;
  sessionId: string;
  usedArchContext: boolean;
  generatedAt: string;
}

export interface WiseDesignerArtifactSummary {
  artifactId: number;
  pitchId: number;
  description: string;
  sessionId: string;
  usedArchContext: boolean;
  createdAt: string;
}

export interface WiseDesignerStatus {
  enabled: boolean;
  message: string;
}

// ────────────────────────────────────────────────────────────────────────────
// API calls
// ────────────────────────────────────────────────────────────────────────────

export const wiseDesignerService = {
  async generate(request: WiseDesignerRequest): Promise<WiseDesignerResponse> {
    const { data } = await api.post<WiseDesignerResponse>('/generate', request);
    return data;
  },

  async iterate(request: WiseDesignerIterateRequest): Promise<WiseDesignerResponse> {
    const { data } = await api.post<WiseDesignerResponse>('/iterate', request);
    return data;
  },

  async getArtifactsForPitch(pitchId: number): Promise<WiseDesignerArtifactSummary[]> {
    const { data } = await api.get<WiseDesignerArtifactSummary[]>(`/pitch/${pitchId}`);
    return data;
  },

  async getArtifact(artifactId: number): Promise<WiseDesignerResponse> {
    const { data } = await api.get<WiseDesignerResponse>(`/artifacts/${artifactId}`);
    return data;
  },

  async getStatus(): Promise<WiseDesignerStatus> {
    const { data } = await api.get<WiseDesignerStatus>('/status');
    return data;
  },
};
