import api from './api';
import { GlobalSearchResult } from '../types';

export const searchService = {
  /**
   * Global search across tasks, subtasks, bug reports, pitches, and epics.
   * Scoped to a specific project. Uses Postgres trigram similarity for fuzzy matching.
   */
  globalSearch: (query: string, projectId: number, limit: number = 10) => {
    return api.get<GlobalSearchResult[]>('/search/global', {
      params: { q: query, projectId, limit },
    });
  },
};
