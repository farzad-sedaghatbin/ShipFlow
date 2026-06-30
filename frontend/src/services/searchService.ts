import api from './api';
import { GlobalSearchResult } from '../types';

export const searchService = {
  /**
   * Global search across tasks, subtasks, bug reports, pitches, and epics (project-scoped),
   * plus org-global wiki spaces and pages (always searched). Pass `projectId` to include that
   * project's entities; omit it (e.g. "All Projects" selected) to search the org-global wiki only.
   * Uses Postgres trigram similarity for fuzzy matching.
   */
  globalSearch: (query: string, projectId?: number, limit: number = 10) => {
    return api.get<GlobalSearchResult[]>('/search/global', {
      params: { q: query, limit, ...(projectId != null ? { projectId } : {}) },
    });
  },
};
