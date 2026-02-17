import axios, { AxiosError } from 'axios';
import {
  AdviceFeedbackRequest,
  AdviceHistoryItem,
  ConversationHistoryPage,
  ConversationSummary,
  DetectStacksRequest,
  DetectStacksResponse,
  FollowUpQuestion,
  FollowUpResponse,
  JobStartResponse,
  JobStatusResponse,
  ProgressCallback,
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
 * Default polling interval in milliseconds
 */
const POLL_INTERVAL = 2000;

/**
 * Maximum polling duration in milliseconds (5 minutes)
 */
const MAX_POLL_DURATION = 5 * 60 * 1000;

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

  // =====================================================================
  // ASYNC METHODS - For large repositories that may timeout
  // =====================================================================

  /**
   * Start async stack detection job.
   * Use this for large repositories that may timeout with sync endpoint.
   */
  startDetectStacksAsync: async (request: DetectStacksRequest): Promise<JobStartResponse> => {
    const response = await wiseArchApi.post<JobStartResponse>('/async/detect-stacks', request);
    return response.data;
  },

  /**
   * Start async solution generation job.
   * Use this for complex analyses that may timeout with sync endpoint.
   */
  startAnalyzeAsync: async (request: WiseArchitectureRequest): Promise<JobStartResponse> => {
    const response = await wiseArchApi.post<JobStartResponse>('/async/analyze', request);
    return response.data;
  },

  /**
   * Get current status of a job.
   */
  getJobStatus: async (jobId: string): Promise<JobStatusResponse> => {
    const response = await wiseArchApi.get<JobStatusResponse>(`/jobs/${jobId}/status`);
    return response.data;
  },

  /**
   * Get result of a completed job.
   * Returns the actual result type (DetectStacksResponse or WiseArchitectureResponse)
   */
  getJobResult: async <T>(jobId: string): Promise<T> => {
    const response = await wiseArchApi.get<T>(`/jobs/${jobId}/result`);
    return response.data;
  },

  /**
   * Cancel a running job.
   */
  cancelJob: async (jobId: string): Promise<void> => {
    await wiseArchApi.delete(`/jobs/${jobId}`);
  },

  /**
   * Detect stacks with automatic fallback to async for large repos.
   * Starts with sync, falls back to async with polling on timeout.
   */
  detectStacksWithFallback: async (
    request: DetectStacksRequest,
    onProgress?: ProgressCallback,
    signal?: AbortSignal
  ): Promise<DetectStacksResponse> => {
    try {
      // Try sync first (faster for small repos)
      onProgress?.(10, 'Scanning repositories...');
      const result = await wiseArchitectureService.detectStacks(request);
      onProgress?.(100, 'Complete');
      return result;
    } catch (error) {
      const axiosError = error as AxiosError;
      
      // On timeout (503/504) or gateway error, fall back to async
      if (axiosError.response?.status === 503 || 
          axiosError.response?.status === 504 ||
          axiosError.code === 'ECONNABORTED') {
        onProgress?.(5, 'Large repo detected, switching to async mode...');
        return wiseArchitectureService.pollDetectStacks(request, onProgress, signal);
      }
      throw error;
    }
  },

  /**
   * Analyze with automatic fallback to async for complex analyses.
   * Starts with sync, falls back to async with polling on timeout.
   */
  analyzeWithFallback: async (
    request: WiseArchitectureRequest,
    onProgress?: ProgressCallback,
    signal?: AbortSignal
  ): Promise<WiseArchitectureResponse> => {
    try {
      // Try sync first (faster for simple analyses)
      onProgress?.(10, 'Generating solution...');
      const result = await wiseArchitectureService.analyze(request);
      onProgress?.(100, 'Complete');
      return result;
    } catch (error) {
      const axiosError = error as AxiosError;
      
      // On timeout (503/504) or gateway error, fall back to async
      if (axiosError.response?.status === 503 || 
          axiosError.response?.status === 504 ||
          axiosError.code === 'ECONNABORTED') {
        onProgress?.(5, 'Complex analysis, switching to async mode...');
        return wiseArchitectureService.pollAnalyze(request, onProgress, signal);
      }
      throw error;
    }
  },

  /**
   * Start detect stacks job and poll until complete.
   */
  pollDetectStacks: async (
    request: DetectStacksRequest,
    onProgress?: ProgressCallback,
    signal?: AbortSignal
  ): Promise<DetectStacksResponse> => {
    const job = await wiseArchitectureService.startDetectStacksAsync(request);
    return wiseArchitectureService.pollUntilComplete<DetectStacksResponse>(
      job.jobId,
      onProgress,
      signal
    );
  },

  /**
   * Start analyze job and poll until complete.
   */
  pollAnalyze: async (
    request: WiseArchitectureRequest,
    onProgress?: ProgressCallback,
    signal?: AbortSignal
  ): Promise<WiseArchitectureResponse> => {
    const job = await wiseArchitectureService.startAnalyzeAsync(request);
    return wiseArchitectureService.pollUntilComplete<WiseArchitectureResponse>(
      job.jobId,
      onProgress,
      signal
    );
  },

  /**
   * Poll a job until it completes or fails.
   * Supports AbortSignal for cancellation.
   */
  pollUntilComplete: async <T>(
    jobId: string,
    onProgress?: ProgressCallback,
    signal?: AbortSignal
  ): Promise<T> => {
    const startTime = Date.now();
    
    while (true) {
      // Check if aborted
      if (signal?.aborted) {
        throw new Error('Operation cancelled');
      }
      
      // Check timeout
      if (Date.now() - startTime > MAX_POLL_DURATION) {
        throw new Error('Job timed out - please try again or select fewer repositories');
      }
      
      const status = await wiseArchitectureService.getJobStatus(jobId);
      
      // Report progress
      onProgress?.(status.progress, status.progressMessage);
      
      switch (status.status) {
        case 'COMPLETED':
          return wiseArchitectureService.getJobResult<T>(jobId);
        
        case 'FAILED':
          throw new Error(status.errorMessage || 'Job failed');
        
        case 'CANCELLED':
          throw new Error('Job was cancelled');
        
        case 'PENDING':
        case 'PROCESSING':
        default:
          // Wait before next poll with abort support
          await new Promise<void>((resolve, reject) => {
            if (signal?.aborted) {
              reject(new Error('Operation cancelled'));
              return;
            }
            
            const timeoutId = setTimeout(resolve, POLL_INTERVAL);
            
            // Listen for abort during the wait
            const abortHandler = () => {
              clearTimeout(timeoutId);
              reject(new Error('Operation cancelled'));
            };
            
            signal?.addEventListener('abort', abortHandler, { once: true });
          });
      }
    }
  },

  // =====================================================================
  // ADVICE HISTORY METHODS - For reviewing and following up on past advice
  // =====================================================================

  /**
   * Get paginated conversation history for the current user.
   */
  getMyHistory: async (page = 0, size = 10): Promise<ConversationHistoryPage> => {
    const response = await wiseArchApi.get<ConversationHistoryPage>('/history', {
      params: { page, size },
    });
    return response.data;
  },

  /**
   * Get conversation history for a specific pitch.
   */
  getPitchHistory: async (pitchId: number): Promise<ConversationSummary[]> => {
    const response = await wiseArchApi.get<ConversationSummary[]>(`/history/pitch/${pitchId}`);
    return response.data;
  },

  /**
   * Get full conversation thread by conversation ID.
   */
  getConversation: async (conversationId: string): Promise<AdviceHistoryItem[]> => {
    const response = await wiseArchApi.get<AdviceHistoryItem[]>(
      `/history/conversation/${conversationId}`
    );
    return response.data;
  },

  /**
   * Get a single advice entry by ID.
   */
  getAdvice: async (adviceId: number): Promise<AdviceHistoryItem> => {
    const response = await wiseArchApi.get<AdviceHistoryItem>(`/history/${adviceId}`);
    return response.data;
  },

  /**
   * Submit feedback for an advice entry.
   */
  submitFeedback: async (
    adviceId: number,
    feedback: AdviceFeedbackRequest
  ): Promise<AdviceHistoryItem> => {
    const response = await wiseArchApi.post<AdviceHistoryItem>(
      `/history/${adviceId}/feedback`,
      feedback
    );
    return response.data;
  },
};

export default wiseArchitectureService;
