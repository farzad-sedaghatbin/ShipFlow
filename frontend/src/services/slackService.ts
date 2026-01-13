import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const slackApi = axios.create({
  baseURL: `${API_BASE_URL}/api/slack`,
});

// Add auth interceptor
slackApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('shipflow_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export interface SlackConfiguration {
  id: number;
  workspaceName: string;
  webhookUrl: string;
  defaultChannel?: string;
  isEnabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSlackConfigurationRequest {
  workspaceName: string;
  webhookUrl: string;
  defaultChannel?: string;
  isEnabled?: boolean;
}

export interface SlackChannelConfig {
  id: number;
  slackConfigId: number;
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

export interface CreateSlackChannelConfigRequest {
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

export interface TestSlackNotificationRequest {
  message?: string;
  channel?: string;
}

export interface SlackNotificationHistory {
  id: number;
  channelName?: string;
  notificationType: string;
  messageText?: string;
  entityType?: string;
  entityId?: number;
  sentAt: string;
  success: boolean;
  errorMessage?: string;
}

export const slackService = {
  // Configuration management
  createConfiguration: async (request: CreateSlackConfigurationRequest): Promise<SlackConfiguration> => {
    const response = await slackApi.post('/configurations', request);
    return response.data;
  },

  getAllConfigurations: async (): Promise<SlackConfiguration[]> => {
    const response = await slackApi.get('/configurations');
    return response.data;
  },

  getActiveConfiguration: async (): Promise<SlackConfiguration | null> => {
    try {
      const response = await slackApi.get('/configurations/active');
      return response.data;
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },

  deleteConfiguration: async (configId: number): Promise<void> => {
    await slackApi.delete(`/configurations/${configId}`);
  },

  // Channel configuration
  createChannelConfig: async (configId: number, request: CreateSlackChannelConfigRequest): Promise<SlackChannelConfig> => {
    const response = await slackApi.post(`/configurations/${configId}/channels`, request);
    return response.data;
  },

  getChannelConfigs: async (configId: number): Promise<SlackChannelConfig[]> => {
    const response = await slackApi.get(`/configurations/${configId}/channels`);
    return response.data;
  },

  deleteChannelConfig: async (channelConfigId: number): Promise<void> => {
    await slackApi.delete(`/channels/${channelConfigId}`);
  },

  // Testing
  sendTestNotification: async (configId: number, request: TestSlackNotificationRequest): Promise<string> => {
    const response = await slackApi.post(`/configurations/${configId}/test`, request);
    return response.data;
  },

  // History
  getNotificationHistory: async (configId: number): Promise<SlackNotificationHistory[]> => {
    const response = await slackApi.get(`/configurations/${configId}/history`);
    return response.data;
  },
};
