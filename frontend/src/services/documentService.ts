import api from './api';

export interface DocumentUploadResponse {
  id?: number;
  fileName?: string;
  fileType?: string;
  fileSize?: number;
  extractedText?: string;
  storagePath?: string;
  textExtracted?: boolean;
  errorMessage?: string;
}

export interface UploadedDocument {
  id: number;
  fileName: string;
  originalFileName: string;
  fileType: string;
  fileSize?: number;
  storagePath?: string;
  extractedText?: string;
  textExtracted: boolean;
  entityType?: string;
  entityId?: number;
  uploaderId?: number;
  uploaderUsername?: string;
  createdAt: string;
  indexedForQA: boolean;
}

export const documentService = {
  // Upload document for a pitch
  uploadForPitch: (pitchId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    // Don't set Content-Type header - let axios/browser set it automatically with boundary
    return api.post<DocumentUploadResponse>(`/documents/pitch/${pitchId}/upload`, formData);
  },

  // Upload document for a meeting
  uploadForMeeting: (meetingId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    // Don't set Content-Type header - let axios/browser set it automatically with boundary
    return api.post<DocumentUploadResponse>(`/documents/meeting/${meetingId}/upload`, formData);
  },

  // Upload document for a cycle
  uploadForCycle: (cycleId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    // Don't set Content-Type header - let axios/browser set it automatically with boundary
    return api.post<DocumentUploadResponse>(`/documents/cycle/${cycleId}/upload`, formData);
  },

  // Generic upload with entity type
  upload: (entityType: string, entityId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('entityType', entityType);
    formData.append('entityId', entityId.toString());
    // Don't set Content-Type header - let axios/browser set it automatically with boundary
    return api.post<DocumentUploadResponse>('/documents/upload', formData);
  },

  // Get documents for a pitch
  getDocumentsForPitch: (pitchId: number) =>
    api.get<UploadedDocument[]>(`/documents/pitch/${pitchId}`),

  // Get documents for a meeting
  getDocumentsForMeeting: (meetingId: number) =>
    api.get<UploadedDocument[]>(`/documents/meeting/${meetingId}`),

  // Get documents for any entity
  getDocumentsByEntity: (entityType: string, entityId: number) =>
    api.get<UploadedDocument[]>(`/documents/entity/${entityType}/${entityId}`),

  // Get single document
  getDocument: (id: number) =>
    api.get<UploadedDocument>(`/documents/${id}`),

  // Delete document
  deleteDocument: (id: number) =>
    api.delete<{ message: string }>(`/documents/${id}`),

  // Index pending documents
  indexPending: () =>
    api.post<{ message: string; indexedCount: number }>('/documents/index-pending'),
};
