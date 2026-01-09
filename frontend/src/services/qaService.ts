import api from './api';

// Types for Q&A feature
export interface AskQuestionRequest {
  question: string;
  contextType?: string;
  contextId?: number;
  cycleId?: number;
  teamId?: number;
  includeSources?: boolean;
}

export interface SourceCitation {
  knowledgeItemId?: number;
  entityType?: string;
  entityId?: number;
  title?: string;
  snippet?: string;
  relevanceScore?: number;
}

export interface QAResponse {
  interactionId?: number;
  question: string;
  answer?: string;
  sources?: SourceCitation[];
  confidenceScore?: number;
  answeredAt?: string;
  processingTimeMs?: number;
  aiEnabled?: boolean;
  errorMessage?: string;
  suggestedFollowUps?: string[];
  /** Whether this response was served from cache */
  cached?: boolean;
}

export type QAFeedbackType = 'ACCURATE' | 'INACCURATE' | 'CORRECTED';

export interface QAFeedbackRequest {
  interactionId: number;
  feedbackType: QAFeedbackType;
  correction?: string;
}

export interface QAStatusDTO {
  qaEnabled?: boolean;
  aiAvailable?: boolean;
  vectorStoreType?: string;
  totalKnowledgeItems?: number;
  embeddedKnowledgeItems?: number;
  totalInteractions?: number;
  validatedInteractions?: number;
  embeddingModel?: string;
  llmModel?: string;
}

export interface QAInteraction {
  id: number;
  question: string;
  answer?: string;
  contextType?: string;
  contextId?: number;
  cycleId?: number;
  teamId?: number;
  confidenceScore?: number;
  feedbackType?: QAFeedbackType;
  feedbackCorrection?: string;
  createdAt: string;
  feedbackAt?: string;
}

// Note types
export interface CreateNoteRequest {
  title: string;
  content: string;
  contextType: string;
  contextId: number;
  includeInKnowledge?: boolean;
}

export interface NoteDTO {
  id: number;
  title: string;
  content: string;
  contextType: string;
  contextId: number;
  cycleId?: number;
  teamId?: number;
  pitchId?: number;
  authorId?: number;
  authorName?: string;
  includeInKnowledge?: boolean;
  createdAt: string;
  updatedAt: string;
}

export const qaService = {
  // Q&A Status
  getStatus: () => api.get<QAStatusDTO>('/qa/status'),

  // Ask a question
  askQuestion: (request: AskQuestionRequest) => 
    api.post<QAResponse>('/qa/ask', request),

  // Submit feedback
  submitFeedback: (request: QAFeedbackRequest) => 
    api.post<{ message: string }>('/qa/feedback', request),

  // Get Q&A history
  getHistory: () => api.get<QAInteraction[]>('/qa/history'),

  // Notes
  createNote: (request: CreateNoteRequest) => 
    api.post<NoteDTO>('/qa/notes', request),

  updateNote: (id: number, request: CreateNoteRequest) => 
    api.put<NoteDTO>(`/qa/notes/${id}`, request),

  deleteNote: (id: number) => 
    api.delete<{ message: string }>(`/qa/notes/${id}`),

  getNoteById: (id: number) => 
    api.get<NoteDTO>(`/qa/notes/${id}`),

  getNotesByContext: (contextType: string, contextId: number) => 
    api.get<NoteDTO[]>(`/qa/notes?contextType=${contextType}&contextId=${contextId}`),

  getNotesByPitch: (pitchId: number) => 
    api.get<NoteDTO[]>(`/qa/notes/pitch/${pitchId}`),

  getNotesByCycle: (cycleId: number) => 
    api.get<NoteDTO[]>(`/qa/notes/cycle/${cycleId}`),

  getNotesByTeam: (teamId: number) => 
    api.get<NoteDTO[]>(`/qa/notes/team/${teamId}`),

  getMyNotes: () => 
    api.get<NoteDTO[]>('/qa/notes/my'),

  // Admin functions
  reindexKnowledge: () => 
    api.post<{ message: string }>('/qa/admin/reindex'),

  processPending: () => 
    api.post<{ message: string; processedCount: number }>('/qa/admin/process-pending'),
};
