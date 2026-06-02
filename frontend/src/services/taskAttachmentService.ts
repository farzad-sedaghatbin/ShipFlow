import api from './api';
import { TaskAttachment } from '../types';

export const taskAttachmentService = {
  /** Fetch all attachments for a task (newest first). */
  getAttachments: (taskId: number) =>
    api.get<TaskAttachment[]>(`/tasks/${taskId}/attachments`),

  /**
   * Upload a file attachment to a task.
   * Pass an `onUploadProgress` callback to track progress.
   */
  uploadAttachment: (
    taskId: number,
    file: File,
    onUploadProgress?: (percent: number) => void
  ) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post<TaskAttachment>(`/tasks/${taskId}/attachments`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (event) => {
        if (onUploadProgress && event.total) {
          onUploadProgress(Math.round((event.loaded * 100) / event.total));
        }
      },
    });
  },

  /**
   * Download an attachment via the authenticated axios client and trigger a
   * browser save. A plain anchor href cannot be used because the JWT lives in
   * localStorage and is only attached by the axios request interceptor.
   */
  downloadAttachment: async (
    taskId: number,
    attachmentId: number,
    fileName: string
  ) => {
    const response = await api.get<Blob>(
      `/tasks/${taskId}/attachments/${attachmentId}/download`,
      { responseType: 'blob' }
    );
    const url = window.URL.createObjectURL(response.data);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  /** Delete an attachment (uploader or ADMIN only). */
  deleteAttachment: (taskId: number, attachmentId: number) =>
    api.delete<void>(`/tasks/${taskId}/attachments/${attachmentId}`),
};
