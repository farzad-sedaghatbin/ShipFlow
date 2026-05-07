import api from './api';
import { ExtractedPitchData } from '../types';

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

export interface ExtractionStatus {
  available: boolean;
  message: string;
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

  // Extract pitch data from a document using AI
  // Optionally adds the document to the knowledge base for Q&A
  extractPitchData: (file: File, pitchId?: number, addToKnowledgeBase: boolean = true) => {
    const formData = new FormData();
    formData.append('file', file);
    if (pitchId !== undefined) {
      formData.append('pitchId', pitchId.toString());
    }
    formData.append('addToKnowledgeBase', addToKnowledgeBase.toString());
    // Don't set Content-Type - let axios/browser set it automatically with boundary
    return api.post<ExtractedPitchData>('/documents/extract-pitch-data', formData);
  },

  // Check if AI extraction is available
  getExtractionStatus: () =>
    api.get<ExtractionStatus>('/documents/extract-pitch-data/status'),

  // Download document
  downloadDocument: async (id: number, fileName: string) => {
    const response = await api.get(`/documents/${id}/download`, {
      responseType: 'blob',
    });
    
    // Create download link
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  // Link existing document to pitch
  linkDocumentToPitch: (documentId: number, pitchId: number) =>
    api.put(`/documents/${documentId}/link-to-pitch/${pitchId}`),

  // ========== Bug Report Attachments ==========

  // Upload attachment for a bug report (images/videos)
  uploadBugAttachment: (bugId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post<DocumentUploadResponse>(`/documents/bug/${bugId}/attachment`, formData);
  },

  // Get attachments for a bug report
  getBugAttachments: (bugId: number) =>
    api.get<UploadedDocument[]>(`/documents/bug/${bugId}/attachments`),

  // Delete bug attachment
  deleteBugAttachment: (attachmentId: number) =>
    api.delete<{ message: string }>(`/documents/bug/attachment/${attachmentId}`),

  // Get attachment URL for display (inline viewing)
  getAttachmentUrl: (attachmentId: number) =>
    `${api.defaults.baseURL}/documents/${attachmentId}/download`,
};
