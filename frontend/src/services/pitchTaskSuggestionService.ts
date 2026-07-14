import api from './api';
import { TaskSuggestionResponse } from '../types';

export const pitchTaskSuggestionService = {
  generate: (pitchId: number) =>
    api.post<TaskSuggestionResponse>(`/ai/pitch-task-suggestions/${pitchId}/generate`).then(r => r.data),
  getStatus: () =>
    api.get<{ available: boolean }>('/ai/pitch-task-suggestions/status').then(r => r.data),
};
