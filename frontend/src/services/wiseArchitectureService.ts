import axios from 'axios';
import {
  DetectStacksRequest,
  DetectStacksResponse,
  FollowUpQuestion,
  FollowUpResponse,
  WiseArchitectureRequest,
  WiseArchitectureResponse,
  WiseArchitectureStatus,
} from '../types/wiseArchitecture';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

const wiseArchApi = axios.create({
  baseURL: `${API_BASE_URL}/api/wise-architecture`,
});

// Add auth interceptor
wiseArchApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('shipflow_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * Service for the Wise Architecture feature - AI-powered technical solution generator.
 * This is an experimental feature that analyzes repositories and generates
 * technical solutions for implementing pitches.
 */
export const wiseArchitectureService = {
  /**
   * Check if the Wise Architecture feature is enabled.
   */
  getStatus: async (): Promise<WiseArchitectureStatus> => {
    const response = await wiseArchApi.get<WiseArchitectureStatus>('/status');
    return response.data;
  },

  /**
   * Detect technology stacks in the selected repositories.
   * Analyzes file patterns and package manifests to identify tech stacks.
   * 
   * @param request - The pitch and repository IDs to analyze
   * @returns Detected stacks with confidence scores
   */
  detectStacks: async (request: DetectStacksRequest): Promise<DetectStacksResponse> => {
    const response = await wiseArchApi.post<DetectStacksResponse>('/detect-stacks', request);
    return response.data;
  },

  /**
   * Generate a technical solution document for the pitch.
   * Uses AI to analyze the codebase and generate implementation recommendations
   * including best practices, library suggestions, and service reuse opportunities.
   * 
   * @param request - The pitch, repositories, and selected stacks
   * @returns Complete technical solution with appetite check
   */
  analyze: async (request: WiseArchitectureRequest): Promise<WiseArchitectureResponse> => {
    const response = await wiseArchApi.post<WiseArchitectureResponse>('/analyze', request);
    return response.data;
  },

  /**
   * Ask a follow-up question about the generated solution.
   * Can generate Copilot-ready prompts for code requests.
   * 
   * @param question - The session ID and follow-up question
   * @returns Answer with optional Copilot prompt for code requests
   */
  followUp: async (question: FollowUpQuestion): Promise<FollowUpResponse> => {
    const response = await wiseArchApi.post<FollowUpResponse>('/follow-up', question);
    return response.data;
  },
};

export default wiseArchitectureService;
