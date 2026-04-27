import axios from 'axios';
import { GitHubLink, GitHubRepository, CreateGitHubRepositoryRequest } from '../types/github';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

const githubApi = axios.create({
  baseURL: `${API_BASE_URL}/api/github`,
});

// Add auth interceptor
githubApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('shipflow_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Types for GitHub App OAuth
export interface GitHubAppStatus {
  configured: boolean;
  appName?: string;
  appSlug?: string;
  permissions?: {
    contents?: string;
    pullRequests?: string;
    issues?: string;
    metadata?: string;
  };
}

export interface GitHubOAuthUrlResponse {
  authorizationUrl: string;
  state: string;
  callbackUrl: string;
  instructions?: string[];
}

export interface GitHubAppInstallation {
  id: number;
  installationId: number;
  accountLogin: string;
  accountType: string;
  repositorySelection: string;
  repositoriesCount?: number;
  tokenValid: boolean;
  installedAt: string;
}

export interface GitHubBulkSyncResult {
  success: boolean;
  totalInstallations?: number;
  totalRepositoriesSynced?: number;
  installationsProcessed?: number;
  repositoriesDiscovered?: number;
  repositoriesAdded?: number;
  syncedInstallations?: string[];
  errors?: string[];
  message?: string;
}

export const githubService = {
  // Repository management
  registerRepository: async (request: CreateGitHubRepositoryRequest): Promise<void> => {
    await githubApi.post('/repositories', request);
  },

  getAllRepositories: async (): Promise<GitHubRepository[]> => {
    const response = await githubApi.get('/repositories');
    return response.data;
  },

  // Task GitHub links
  getTaskGitHubLinks: async (taskId: number): Promise<GitHubLink[]> => {
    const response = await githubApi.get(`/tasks/${taskId}/links`);
    return response.data;
  },

  // Pitch GitHub links
  getPitchGitHubLinks: async (pitchId: number): Promise<GitHubLink[]> => {
    const response = await githubApi.get(`/pitches/${pitchId}/links`);
    return response.data;
  },

  // GitHub App OAuth methods
  getAppStatus: async (): Promise<GitHubAppStatus> => {
    const response = await githubApi.get('/app/status');
    return response.data;
  },

  initiateOAuth: async (suggestedOrganization?: string): Promise<GitHubOAuthUrlResponse> => {
    const baseUrl = window.location.origin;
    const response = await githubApi.post('/app/authorize', {
      baseUrl,
      suggestedOrganization,
      redirectUrl: `${baseUrl}/settings/integrations/github`,
    });
    return response.data;
  },

  getInstallations: async (): Promise<GitHubAppInstallation[]> => {
    const response = await githubApi.get('/app/installations');
    return response.data;
  },

  syncInstallation: async (installationId: number): Promise<{ repositoriesSynced: number }> => {
    const response = await githubApi.post(`/app/installations/${installationId}/sync`);
    return response.data;
  },

  syncAllRepositories: async (): Promise<GitHubBulkSyncResult> => {
    const response = await githubApi.post('/app/sync-all');
    return response.data;
  },

  discoverInstallations: async (): Promise<GitHubBulkSyncResult> => {
    const response = await githubApi.post('/app/discover');
    return response.data;
  },

  removeInstallation: async (installationId: number): Promise<void> => {
    await githubApi.delete(`/app/installations/${installationId}`);
  },
};
