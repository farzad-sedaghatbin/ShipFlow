import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

const inboundApi = axios.create({
  baseURL: `${API_BASE_URL}/api/inbound-webhooks`,
});

inboundApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('shipflow_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ── Types ────────────────────────────────────────────────────────────

export interface InboundWebhookConfig {
  id: number;
  providerName: string;
  displayName?: string;
  webhookSecret?: string;
  signatureHeader?: string;
  hmacAlgorithm?: string;
  description?: string;
  isEnabled: boolean;
  webhookUrl: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateInboundWebhookConfigRequest {
  providerName: string;
  displayName?: string;
  webhookSecret?: string;
  signatureHeader?: string;
  hmacAlgorithm?: string;
  description?: string;
  isEnabled?: boolean;
}

// ── API ──────────────────────────────────────────────────────────────

export const inboundWebhookService = {
  createConfiguration: (request: CreateInboundWebhookConfigRequest) =>
    inboundApi.post<InboundWebhookConfig>('/configurations', request),

  getAllConfigurations: () =>
    inboundApi.get<InboundWebhookConfig[]>('/configurations'),

  getConfiguration: (id: number) =>
    inboundApi.get<InboundWebhookConfig>(`/configurations/${id}`),

  getEnabledConfigurations: () =>
    inboundApi.get<InboundWebhookConfig[]>('/configurations/enabled'),

  toggleConfiguration: (id: number) =>
    inboundApi.patch<InboundWebhookConfig>(`/configurations/${id}/toggle`),

  deleteConfiguration: (id: number) =>
    inboundApi.delete(`/configurations/${id}`),
};
