import api from './api';
import { TaskSuggestionResponse } from '../types';

export const sprintTaskSuggestionService = {
  generate: (cycleId: number) =>
    api.post<TaskSuggestionResponse>(`/ai/sprint-task-suggestions/${cycleId}/generate`).then(r => r.data),
  getStatus: () =>
    api.get<{ available: boolean }>('/ai/sprint-task-suggestions/status').then(r => r.data),
};
