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

  /** Returns the API endpoint path for an attachment download. Use as an anchor href or navigate to it to trigger the browser download. */
  getDownloadUrl: (taskId: number, attachmentId: number) =>
    `/api/tasks/${taskId}/attachments/${attachmentId}/download`,

  /** Delete an attachment (uploader or ADMIN only). */
  deleteAttachment: (taskId: number, attachmentId: number) =>
    api.delete<void>(`/tasks/${taskId}/attachments/${attachmentId}`),
};
