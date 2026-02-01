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
};
