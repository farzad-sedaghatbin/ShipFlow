import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

const teamsApi = axios.create({
  baseURL: `${API_BASE_URL}/api/teams`,
});

// Add auth interceptor
teamsApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('shipflow_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export interface TeamsConfiguration {
  id: number;
  tenantName: string;
  webhookUrl: string;
  defaultChannel?: string;
  isEnabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeamsConfigurationRequest {
  tenantName: string;
  webhookUrl: string;
  defaultChannel?: string;
  isEnabled?: boolean;
}

export interface TeamsChannelConfig {
  id: number;
  teamsConfigId: number;
  channelName: string;
  channelWebhookUrl?: string;
  notifyTaskAssigned: boolean;
  notifyTaskCompleted: boolean;
  notifyTaskBlocked: boolean;
  notifyPitchShaped: boolean;
  notifyCycleStarted: boolean;
  notifyCycleCooldown: boolean;
  notifyBettingCompleted: boolean;
  notifySprintStarted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeamsChannelConfigRequest {
  channelName: string;
  channelWebhookUrl?: string;
  notifyTaskAssigned?: boolean;
  notifyTaskCompleted?: boolean;
  notifyTaskBlocked?: boolean;
  notifyPitchShaped?: boolean;
  notifyCycleStarted?: boolean;
  notifyCycleCooldown?: boolean;
  notifyBettingCompleted?: boolean;
  notifySprintStarted?: boolean;
}

export interface TestTeamsNotificationRequest {
  message?: string;
  channel?: string;
}

export interface TeamsNotificationHistory {
  id: number;
  teamsConfigId: number;
  channelName?: string;
  notificationType: string;
  messageText?: string;
  entityType?: string;
  entityId?: number;
  sentAt: string;
  success: boolean;
  errorMessage?: string;
}

export const teamsService = {
  // Configuration management
  createConfiguration: async (request: CreateTeamsConfigurationRequest): Promise<TeamsConfiguration> => {
    const response = await teamsApi.post('/configurations', request);
    return response.data;
  },

  getAllConfigurations: async (): Promise<TeamsConfiguration[]> => {
    const response = await teamsApi.get('/configurations');
    return response.data;
  },

  getActiveConfiguration: async (): Promise<TeamsConfiguration | null> => {
    try {
      const response = await teamsApi.get('/configurations/active');
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },

  deleteConfiguration: async (configId: number): Promise<void> => {
    await teamsApi.delete(`/configurations/${configId}`);
  },

  // Channel configuration
  createChannelConfig: async (configId: number, request: CreateTeamsChannelConfigRequest): Promise<TeamsChannelConfig> => {
    const response = await teamsApi.post(`/configurations/${configId}/channels`, request);
    return response.data;
  },

  getChannelConfigs: async (configId: number): Promise<TeamsChannelConfig[]> => {
    const response = await teamsApi.get(`/configurations/${configId}/channels`);
    return response.data;
  },

  deleteChannelConfig: async (channelConfigId: number): Promise<void> => {
    await teamsApi.delete(`/channels/${channelConfigId}`);
  },

  // Testing
  sendTestNotification: async (configId: number, request: TestTeamsNotificationRequest): Promise<string> => {
    const response = await teamsApi.post(`/configurations/${configId}/test`, request);
    return response.data;
  },

  // History
  getNotificationHistory: async (configId: number): Promise<TeamsNotificationHistory[]> => {
    const response = await teamsApi.get(`/configurations/${configId}/history`);
    return response.data;
  },
};
